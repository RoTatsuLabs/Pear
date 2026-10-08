package rotatsu.yos.music.player.code.utils.player

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import androidx.annotation.OptIn
import androidx.compose.runtime.snapshotFlow
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import rotatsu.yos.music.player.data.libraries.SettingsLibrary

/**
 * Overlaps the end of a song with the start of the next one.
 *
 * A second player loads the tail of the playing song ahead of time. When the fade starts, the
 * main player jumps to the next song at volume zero while the second player carries on with the
 * tail, and the volumes cross over. It only runs when playback reaches the end of a song by
 * itself. Skipping, seeking and pausing end it.
 */
@OptIn(UnstableApi::class)
class Crossfade(
    private val player: ExoPlayer,
    private val createSecondary: () -> ExoPlayer
) : Player.Listener {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var secondary: ExoPlayer? = null
    private var prepareMessage: PlayerMessage? = null
    private var fireMessage: PlayerMessage? = null
    private var scheduledKey: String? = null
    private var fireAtMs = 0L
    private var fadeAnimator: ValueAnimator? = null
    private var tailAnimator: ValueAnimator? = null
    private var active = false
    private var firing = false

    fun attach() {
        player.addListener(this)
        scope.launch {
            snapshotFlow { SettingsLibrary.CrossfadeEnabled to SettingsLibrary.CrossfadeDuration }
                .collect {
                    reset()
                    schedule()
                }
        }
    }

    fun release() {
        player.removeListener(this)
        scope.cancel()
        abort(restoreVolume = false, fadeOutMs = 0L)
        reset()
    }

    /** Call before the main player pauses, so the tail of the old song fades out with it. */
    fun beforePause() {
        if (active) abort(restoreVolume = false, fadeOutMs = 200L)
        reset()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = playbackMoved()

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) = playbackMoved()

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) {
            schedule()
        } else {
            if (active) abort(restoreVolume = false, fadeOutMs = 200L)
            reset()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> schedule()
            Player.STATE_ENDED, Player.STATE_IDLE -> {
                if (active) abort(restoreVolume = true, fadeOutMs = 0L)
                reset()
            }
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) = schedule()

    override fun onRepeatModeChanged(repeatMode: Int) = schedule()

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = schedule()

    private fun playbackMoved() {
        if (firing) return
        if (active) abort(restoreVolume = true, fadeOutMs = 120L)
        reset()
        schedule()
    }

    private fun schedule() {
        if (active) return
        val fadeMs = SettingsLibrary.CrossfadeDuration * 1000L
        val index = player.currentMediaItemIndex
        val next = player.nextMediaItemIndex
        val eligible = SettingsLibrary.CrossfadeEnabled &&
                player.playWhenReady &&
                player.playbackState == Player.STATE_READY &&
                player.repeatMode != Player.REPEAT_MODE_ONE &&
                player.mediaItemCount > 1 &&
                next != C.INDEX_UNSET &&
                player.duration != C.TIME_UNSET
        if (!eligible) {
            reset()
            return
        }
        val duration = player.duration
        val key = "$index|$next|$duration|$fadeMs|${player.mediaItemCount}"
        if (key == scheduledKey) return

        reset()
        val plan = CrossfadePlan.plan(duration, player.currentPosition, fadeMs) ?: return
        scheduledKey = key
        fireAtMs = plan.fireAtMs
        prepareMessage = player.createMessage { _, _ -> prepareTail() }
            .setLooper(Looper.getMainLooper())
            .setPosition(index, plan.prepareAtMs)
            .setDeleteAfterDelivery(true)
            .send()
        fireMessage = player.createMessage { _, _ -> startFade() }
            .setLooper(Looper.getMainLooper())
            .setPosition(index, plan.fireAtMs)
            .setDeleteAfterDelivery(true)
            .send()
    }

    private fun prepareTail() {
        val item = player.currentMediaItem ?: return
        val tail = secondary ?: createSecondary().also { secondary = it }
        tail.volume = 1f
        tail.playWhenReady = false
        tail.playbackParameters = player.playbackParameters
        tail.setMediaItem(item, fireAtMs)
        tail.prepare()
    }

    private fun startFade() {
        val tail = secondary
        if (tail == null || !player.playWhenReady || player.nextMediaItemIndex == C.INDEX_UNSET) {
            reset()
            return
        }
        val fadeMs = SettingsLibrary.CrossfadeDuration * 1000L
        firing = true
        active = true
        scheduledKey = null

        tail.volume = 1f
        tail.playbackParameters = player.playbackParameters
        tail.playWhenReady = true
        player.volume = 0f
        player.seekToNextMediaItem()
        handler.post { firing = false }

        var cancelled = false
        fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = fadeMs
            interpolator = LinearInterpolator()
            addUpdateListener {
                val progress = it.animatedValue as Float
                player.volume = CrossfadePlan.gainIn(progress)
                secondary?.volume = CrossfadePlan.gainOut(progress)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (cancelled) return
                    fadeAnimator = null
                    active = false
                    player.volume = 1f
                    reset()
                    schedule()
                }
            })
            start()
        }
    }

    /** Ends a running fade. The tail of the old song fades out over [fadeOutMs] and stops. */
    private fun abort(restoreVolume: Boolean, fadeOutMs: Long) {
        fadeAnimator?.cancel()
        fadeAnimator = null
        active = false
        if (restoreVolume) player.volume = 1f

        val tail = secondary
        if (tail == null) return
        secondary = null
        if (fadeOutMs <= 0L) {
            tail.release()
            return
        }
        val from = tail.volume
        tailAnimator?.cancel()
        tailAnimator = ValueAnimator.ofFloat(from, 0f).apply {
            duration = fadeOutMs
            addUpdateListener { tail.volume = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tail.release()
                }
            })
            start()
        }
    }

    /** Drops the scheduled messages and any tail player that is not fading yet. */
    private fun reset() {
        prepareMessage?.cancel()
        fireMessage?.cancel()
        prepareMessage = null
        fireMessage = null
        scheduledKey = null
        if (!active) {
            secondary?.release()
            secondary = null
        }
    }
}
