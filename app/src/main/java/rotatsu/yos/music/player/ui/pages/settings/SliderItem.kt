package rotatsu.yos.music.player.ui.pages.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.CupertinoSlider
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import rotatsu.yos.music.player.R
import rotatsu.yos.music.player.code.utils.others.Vibrator
import kotlin.math.roundToInt

/**
 * A slider row. With [showStepButtons] a minus and a plus move the value by one [step], and
 * [label] shows the value at the end of the row.
 */
@Composable
fun SliderItem(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
    showStepButtons: Boolean = true,
    label: String? = null
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    fun snap(raw: Float): Float {
        val steps = ((raw - valueRange.start) / step).roundToInt()
        return (valueRange.start + steps * step).coerceIn(valueRange.start, valueRange.endInclusive)
    }

    CupertinoTheme {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showStepButtons) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tips_minus),
                    contentDescription = null,
                    modifier = Modifier
                        .size(12.dp)
                        .alpha(0.45f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (value <= valueRange.start) return@clickable
                                Vibrator.click(context)
                                onValueChange(snap(value - step))
                            })
                )
            }
            CupertinoSlider(
                value = value,
                onValueChange = { onValueChange(snap(it)) },
                thumb = {
                    Spacer(
                        Modifier
                            .size(23.dp)
                            .hoverable(interactionSource = interactionSource)
                            .shadow(
                                8.dp,
                                CircleShape,
                                clip = false,
                                spotColor = Color.Black.copy(alpha = 0.55f)
                            )
                            .background(Color.White, CircleShape)
                    )
                },
                interactionSource = interactionSource,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp, start = 12.dp),
                valueRange = valueRange
            )
            if (showStepButtons) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tips_plus),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(14.dp)
                        .alpha(0.45f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (value >= valueRange.endInclusive) return@clickable
                                Vibrator.click(context)
                                onValueChange(snap(value + step))
                            })
                )
            }
            if (label != null) {
                Text(text = label, fontSize = 17.sp, modifier = Modifier.alpha(0.6f))
            }
        }
    }
}
