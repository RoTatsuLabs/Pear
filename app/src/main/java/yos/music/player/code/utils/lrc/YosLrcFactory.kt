package yos.music.player.code.utils.lrc

/**
 * Lrc 歌词文本处理
 *
 * Reads LRC text, including the word timed variant and duets written as "Singer:" lines.
 * [parse] has no side effects. [formatLrcEntries] keeps the old entry point for callers
 * that only want the line list and publishes the duet sides the way it always did.
 */
class YosLrcFactory(private val formatText: Boolean = true) {

    /** Parses [lrcText] without touching any shared state. */
    fun parse(lrcText: String): YosLyrics {
        val entries = readEntries(lrcText)
        if (entries.isEmpty()) return YosLyrics.EMPTY
        val (lines, otherSide) = processOtherSide(entries)
        val kept = lines.indices.filter { lines[it].isNotEmpty() }
        return YosLyrics(
            format = YosLyricFormat.LRC,
            entries = kept.map { lines[it] },
            otherSide = kept.map { otherSide[it] },
            transliterations = kept.map { null },
            credits = emptyList()
        )
    }

    /**
     * Lrc 歌词文本处理方法
     * @param lrcText Lrc 格式的文本
     */
    fun formatLrcEntries(lrcText: String): List<List<Pair<Float, String>>> {
        val lyrics = parse(lrcText)
        YosLyricsFactory.publishOtherSide(lyrics.otherSide)
        return lyrics.entries
    }

    private fun readEntries(lrcText: String): List<List<Pair<Float, String>>> {
        val lrcLines = lrcText.lines()
        val timeLyricPairs = mutableListOf<MutableList<Pair<Float, String>>>()
        lrcLines.forEachIndexed { index, line ->
            //将文本中完全相同而且重复的两个时间轴修改为一个
            //比如[12:34.56][12:34.56]改为[12:34.56]
            var remainingLine =
                line.replace(Regex("([\\[\\]]){2,}"), "$1").replace(Regex("<([^>]+)>"), "[$1]")
                    .replace(Regex("(\\[\\d{2}:\\d{2}\\.\\d{2,3}]){2,}"), "$1")
            val currentLinePairs = mutableListOf<Pair<Float, String>>()
            while (remainingLine.isNotEmpty()) {
                val timeIndex = remainingLine.indexOf("[")
                if (timeIndex == -1) break
                val timeAfter = remainingLine.indexOf("]")
                if (timeAfter == -1) break
                val timeText = remainingLine.substring(timeIndex + 1, timeAfter)
                val timeParts = timeText.split(":")
                if (timeParts.size != 2) break
                val minutes = timeParts[0].toIntOrNull() ?: break
                val seconds = timeParts[1].toFloatOrNull() ?: break
                val time = (minutes * 60 + seconds) * 1000

                if (remainingLine.substring(timeAfter + 1, remainingLine.length)
                        .isBlank() && remainingLine.substring(0, timeIndex).isBlank()
                ) {
                    // 检查下一行的时间差
                    if (index + 1 < lrcLines.size) {
                        val nextLine = lrcLines[index + 1]
                        val nextTimeIndex = nextLine.indexOf("[")
                        val nextTimeAfter = nextLine.indexOf("]")
                        if (nextTimeIndex != -1 && nextTimeAfter != -1) {
                            val nextTimeText = nextLine.substring(nextTimeIndex + 1, nextTimeAfter)
                            val nextTimeParts = nextTimeText.split(":")
                            if (nextTimeParts.size == 2) {
                                val nextMinutes = nextTimeParts[0].toIntOrNull()
                                val nextSeconds = nextTimeParts[1].toFloatOrNull()
                                if (nextMinutes != null && nextSeconds != null) {
                                    val nextTime = (nextMinutes * 60 + nextSeconds) * 1000
                                    if (nextTime - time <= BLANK_LINE_MAX_GAP_MS) {
                                        // 忽略当前行的处理，进行下一行的处理
                                        break
                                    }
                                }
                            }
                        }
                    } else {
                        // 这是最后一行，且为空行
                        break
                    }
                }

                val nextTimeIndex = remainingLine.substring(timeAfter + 1).indexOf("[")

                // 逐行起始或逐字末尾
                var lyric = remainingLine.substring(0, timeIndex)

                if (lyric.isEmpty()) {
                    // 句子起始
                    lyric = ""
                    currentLinePairs.add(time to lyric.replace(Regex("(?!\\n)\\s+"), " "))
                } else {
                    // 正常句子成分
                    if (lyric.trim() != "//") {
                        currentLinePairs.add(
                            time to lyric.replace(Regex("(?!\\n)\\s+"), " ")
                        )
                    }
                }

                remainingLine = remainingLine.substring(timeAfter + 1)
                if (nextTimeIndex == -1) {
                    if (lyric == "") {
                        currentLinePairs.add(
                            time to remainingLine.replace("//", "").replace(
                                Regex("(?!\\n)\\s+"),
                                " "
                            )
                        )
                    }
                    remainingLine = ""
                }
            }
            if (currentLinePairs.isNotEmpty()) {
                val existingList =
                    timeLyricPairs.find { it.first().first == currentLinePairs.first().first }
                if (existingList != null) {
                    existingList.addAll(currentLinePairs)
                } else {
                    currentLinePairs.add(currentLinePairs[0].first to "")
                    timeLyricPairs.add(currentLinePairs)
                }
            }
        }
        return timeLyricPairs
    }

    /** Returns the lines without their "Singer:" marker pairs, and the side of each line. */
    private fun processOtherSide(
        lrcEntries: List<List<Pair<Float, String>>>
    ): Pair<List<List<Pair<Float, String>>>, List<Boolean>> {
        // 对唱处理
        val otherSideResult = mutableListOf<Boolean>()
        var otherSide = false
        var lastSinger: String? = null
        var otherSideFirstTime = false

        val filteredLrcEntries = lrcEntries.map { lines ->
            val lyric = lines.joinToString(separator = "") { it.second }

            var deleteType = -1

            if (lyric.endsWith(":") || lyric.endsWith("：")) {
                otherSide = !otherSide
            } else if (lines.size > 1) {
                val currentSinger = lines[1].second
                if (currentSinger.matches(Regex(".+\\s*:\\s*"))) {
                    deleteType = 0
                    if (lastSinger != null && lastSinger == currentSinger) {
                        // 保持 otherSide 不变
                    } else {
                        if (otherSideFirstTime) {
                            otherSide = !otherSide
                        } else {
                            otherSideFirstTime = true
                        }
                    }
                    lastSinger = currentSinger
                }
            }

            otherSideResult.add(otherSide)

            lines.filterIndexed { index, char ->
                !((index == 1 && char.second.matches(Regex(".+\\s*:\\s*"))) && deleteType == 0)
            }
        }

        return filteredLrcEntries to otherSideResult
    }

    companion object {
        /**
         * A blank line only survives when the next line starts more than this long after it.
         * Shorter gaps are not worth a countdown.
         */
        const val BLANK_LINE_MAX_GAP_MS = 4200f
    }
}
