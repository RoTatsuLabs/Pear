package yos.music.player.code.utils.lrc

import yos.music.player.data.objects.MediaViewModelObject

/**
 * Entry point for lyric text. It tells TTML from LRC and publishes the result for the UI.
 */
object YosLyricsFactory {
    private val TTML_ROOT = Regex("<(?:[A-Za-z_][\\w.-]*:)?tt[\\s>]")

    /**
     * Reads lyric text in either format. Returns [YosLyrics.EMPTY] for blank text and for a
     * TTML file that cannot be read.
     *
     * Automatic transliteration is not part of this call, [YosTransliterator.fill] adds it.
     * It can take a moment, so the caller decides when and on which thread it runs.
     *
     * @param preferredLanguage language code used to pick a TTML translation, e.g. "en"
     */
    fun parse(content: String?, preferredLanguage: String? = null): YosLyrics {
        if (content.isNullOrBlank()) return YosLyrics.EMPTY
        return if (looksLikeTtml(content)) {
            YosTtmlFactory(preferredLanguage).parse(content)
        } else {
            YosLrcFactory().parse(content)
        }
    }

    fun looksLikeTtml(content: String): Boolean {
        val start = content.trimStart('\uFEFF', ' ', '\n', '\r', '\t')
        return start.startsWith("<") && TTML_ROOT.containsMatchIn(start.take(2048))
    }

    /**
     * Hands the lyrics of the playing song to the lyric view and the status bar lyric.
     * Publishing the same lines again with a transliteration added only changes
     * [MediaViewModelObject.lyrics], so the list does not redraw.
     */
    fun publish(lyrics: YosLyrics) {
        MediaViewModelObject.lyrics.value = lyrics
        if (MediaViewModelObject.otherSideForLines.toList() != lyrics.otherSide) {
            publishOtherSide(lyrics.otherSide)
        }
        MediaViewModelObject.lrcEntries.value = lyrics.entries
    }

    fun publishOtherSide(sides: List<Boolean>) {
        MediaViewModelObject.otherSideForLines.clear()
        MediaViewModelObject.otherSideForLines.addAll(sides)
    }
}
