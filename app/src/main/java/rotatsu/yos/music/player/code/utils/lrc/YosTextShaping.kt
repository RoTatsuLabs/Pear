package rotatsu.yos.music.player.code.utils.lrc

/**
 * Tells whether a line has to be drawn from one shaped layout. Letters of joining scripts
 * change shape with their neighbours and marks sit on the letter before them, so a
 * character measured alone is drawn wrong.
 */
internal object YosTextShaping {

    fun needsShapedDraw(text: CharSequence): Boolean {
        var i = 0
        while (i < text.length) {
            val cp = Character.codePointAt(text, i)
            when (YosScripts.of(cp)) {
                YosScript.ARABIC, YosScript.HEBREW, YosScript.THAI, YosScript.DEVANAGARI -> return true
                else -> {}
            }
            val type = Character.getType(cp)
            if (type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.COMBINING_SPACING_MARK.toInt()
            ) {
                return true
            }
            i += Character.charCount(cp)
        }
        return false
    }
}
