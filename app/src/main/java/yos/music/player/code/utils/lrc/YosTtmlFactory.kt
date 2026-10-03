package yos.music.player.code.utils.lrc

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

/**
 * TTML 歌词处理
 *
 * Reads TTML lyrics, including the Apple Music flavour: word spans, duet agents, inline or
 * head level translations and transliterations, and the songwriter credits in the head.
 * The result has the same shape as the LRC one, so the lyric view treats both alike.
 *
 * Timing is only taken from the file. A line whose spans carry no times stays a plain line,
 * and nothing is spread over it.
 *
 * @param preferredLanguage language code such as "en" used to pick one of several
 * translations. When it matches none, the first translation is used.
 */
class YosTtmlFactory(private val preferredLanguage: String? = null) {

    /** A piece of text with the times the file gave it, if any. */
    private class Segment(var text: String, val begin: Float?, val end: Float?)

    private class Node(val name: String, val attrs: Map<String, String>) {
        val children = mutableListOf<Any>() // Node or String
        fun elements(): List<Node> = children.filterIsInstance<Node>()
        fun text(): String = buildString {
            children.forEach { if (it is Node) append(it.text()) else append(it as String) }
        }
    }

    private class Builder : DefaultHandler() {
        var root: Node? = null
        private val stack = ArrayDeque<Node>()

        override fun startElement(uri: String?, localName: String?, qName: String?, atts: Attributes) {
            val name = (localName?.takeIf { it.isNotEmpty() } ?: qName.orEmpty().substringAfter(':'))
            val attrs = HashMap<String, String>()
            for (i in 0 until atts.length) {
                val key = atts.getLocalName(i).takeIf { it.isNotEmpty() }
                    ?: atts.getQName(i).substringAfter(':')
                attrs[key] = atts.getValue(i)
            }
            val node = Node(name, attrs)
            stack.lastOrNull()?.children?.add(node)
            if (root == null) root = node
            stack.addLast(node)
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            stack.removeLastOrNull()
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            stack.lastOrNull()?.children?.add(String(ch, start, length))
        }
    }

    fun parse(ttmlText: String): YosLyrics {
        val root = readTree(ttmlText) ?: return YosLyrics.EMPTY
        val head = root.elements().firstOrNull { it.name == "head" }
        val body = root.elements().firstOrNull { it.name == "body" } ?: return YosLyrics.EMPTY

        val agents = LinkedHashMap<String, Boolean>() // agent id -> is a sung voice
        val credits = mutableListOf<YosLyricCredit>()
        val translations = HashMap<String, String>()
        val transliterations = HashMap<String, List<Segment>>()
        if (head != null) readHead(head, agents, credits, translations, transliterations)

        // Voices alternate between the two sides in the order they first sing.
        val sideOfAgent = HashMap<String, Boolean>()
        var voices = 0
        agents.forEach { (id, isVoice) ->
            if (isVoice) sideOfAgent[id] = voices++ % 2 == 1 else sideOfAgent[id] = false
        }

        class Line(
            val begin: Float,
            val end: Float?,
            val segments: List<Segment>,
            val translation: String?,
            val romanSegments: List<Segment>?,
            val otherSide: Boolean
        )

        val lines = mutableListOf<Line>()

        fun readParagraph(p: Node) {
            val begin0 = parseTime(p.attrs["begin"])
            val end0 = parseTime(p.attrs["end"])
            val collected = Collected()
            collect(p, begin0, collected)

            val segments = collected.segments
            val firstTimed = segments.firstOrNull { it.begin != null }
            val begin = begin0 ?: firstTimed?.begin ?: return // no time, no place in the song
            val lastTimed = segments.lastOrNull { it.end != null }
            val end = end0 ?: lastTimed?.end

            if (segments.none { it.text.isNotEmpty() }) return
            val key = p.attrs["key"]
            val agent = p.attrs["agent"]

            val translation = collected.translation?.ifBlank { null }
                ?: key?.let { translations[it] }
            val roman = collected.roman ?: key?.let { transliterations[it] }

            lines += Line(begin, end, segments, translation, roman, agent?.let { sideOfAgent[it] } ?: false)
        }

        fun walk(node: Node) {
            node.elements().forEach {
                if (it.name == "p") readParagraph(it) else walk(it)
            }
        }
        walk(body)
        if (lines.isEmpty()) return YosLyrics.EMPTY

        val sorted = lines.sortedBy { it.begin }

        val entries = mutableListOf<List<Pair<Float, String>>>()
        val sides = mutableListOf<Boolean>()
        val roman = mutableListOf<List<String>?>()

        fun blank(time: Float, side: Boolean) {
            entries += listOf(time to "", time to "", time to "")
            sides += side
            roman += null
        }

        if (sorted.first().begin > YosLrcFactory.BLANK_LINE_MAX_GAP_MS) blank(0f, false)

        sorted.forEachIndexed { index, line ->
            val (entry, slots) = toEntry(line.begin, line.segments, line.translation)
            entries += entry
            sides += line.otherSide
            roman += line.romanSegments?.let { align(slots, it) }

            val next = sorted.getOrNull(index + 1)
            val lineEnd = line.end
            if (next != null && lineEnd != null && next.begin - lineEnd > YosLrcFactory.BLANK_LINE_MAX_GAP_MS) {
                blank(lineEnd, line.otherSide)
            }
        }

        return YosLyrics(
            format = YosLyricFormat.TTML,
            entries = entries,
            otherSide = sides,
            transliterations = roman,
            credits = credits,
            language = root.attrs["lang"]?.ifBlank { null }
        )
    }

    // ---- tree -------------------------------------------------------------------------

    private fun readTree(text: String): Node? {
        return try {
            val factory = SAXParserFactory.newInstance()
            factory.isNamespaceAware = true
            runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            val builder = Builder()
            val clean = text.trimStart('\uFEFF', ' ', '\n', '\r', '\t')
            factory.newSAXParser().parse(InputSource(StringReader(clean)), builder)
            builder.root?.takeIf { it.name == "tt" }
        } catch (_: Exception) {
            null
        }
    }

    // ---- head -------------------------------------------------------------------------

    private fun readHead(
        head: Node,
        agents: MutableMap<String, Boolean>,
        credits: MutableList<YosLyricCredit>,
        translations: MutableMap<String, String>,
        transliterations: MutableMap<String, List<Segment>>
    ) {
        val metadata = head.elements().firstOrNull { it.name == "metadata" } ?: return

        // Several translation languages can exist, so keep the best match only.
        var pickedTranslation: Node? = null

        metadata.elements().forEach { node ->
            when (node.name) {
                "agent" -> {
                    val id = node.attrs["id"] ?: return@forEach
                    val type = node.attrs["type"]
                    agents[id] = type == null || type == "person"
                }

                "title", "desc", "copyright" -> {
                    val value = node.text().trim()
                    if (value.isNotEmpty()) credits += YosLyricCredit(node.name, listOf(value))
                }

                "iTunesMetadata" -> node.elements().forEach { child ->
                    when (child.name) {
                        "translations" -> pickedTranslation = pickTranslation(child)
                        "transliterations" -> child.elements()
                            .firstOrNull { it.name == "transliteration" }
                            ?.elements()?.filter { it.name == "text" }?.forEach { text ->
                                val key = text.attrs["for"] ?: return@forEach
                                val collected = Collected()
                                collect(text, null, collected)
                                transliterations[key] = collected.segments
                            }

                        else -> {
                            val values = (if (child.elements().isEmpty()) listOf(child) else child.elements())
                                .map { it.text().trim() }
                                .filter { it.isNotEmpty() }
                            if (values.isNotEmpty()) credits += YosLyricCredit(child.name, values)
                        }
                    }
                }
            }
        }

        pickedTranslation?.elements()?.filter { it.name == "text" }?.forEach { text ->
            val key = text.attrs["for"] ?: return@forEach
            val value = text.text().trim().replace(WHITESPACE, " ")
            if (value.isNotEmpty()) translations[key] = value
        }
    }

    private fun pickTranslation(translations: Node): Node? {
        val all = translations.elements().filter { it.name == "translation" }
        val language = preferredLanguage?.lowercase()?.substringBefore('-')?.substringBefore('_')
        return all.firstOrNull {
            language != null &&
                    it.attrs["lang"]?.lowercase()?.substringBefore('-')?.substringBefore('_') == language
        } ?: all.firstOrNull()
    }

    // ---- paragraph --------------------------------------------------------------------

    private class Collected {
        val segments = mutableListOf<Segment>()
        var translation: String? = null
        var roman: List<Segment>? = null
    }

    /** Walks one paragraph (or one transliteration line) and gathers its segments. */
    private fun collect(node: Node, paragraphBegin: Float?, out: Collected) {
        node.children.forEach { child ->
            if (child is String) {
                appendText(out, child)
                return@forEach
            }
            child as Node
            when {
                child.name == "br" -> appendText(out, " ")
                child.name != "span" -> collect(child, paragraphBegin, out)
                child.attrs["role"] == "x-translation" ->
                    out.translation = (out.translation.orEmpty() + child.text()).replace(WHITESPACE, " ").trim()

                child.attrs["role"] == "x-roman" -> {
                    val inner = Collected()
                    collect(child, paragraphBegin, inner)
                    out.roman = inner.segments
                }

                child.elements().any { it.name == "span" } -> collect(child, paragraphBegin, out)
                else -> {
                    val text = child.text().replace(WHITESPACE, " ")
                    var begin = parseTime(child.attrs["begin"])
                    var end = parseTime(child.attrs["end"])
                    // Times before the paragraph start are offsets inside the paragraph.
                    if (paragraphBegin != null && begin != null && begin < paragraphBegin) {
                        begin += paragraphBegin
                        end = end?.plus(paragraphBegin)
                    }
                    if (begin != null && end != null) {
                        if (text.isNotEmpty()) out.segments += Segment(text, begin, end)
                    } else {
                        appendText(out, text)
                    }
                }
            }
        }
    }

    /** Text without times belongs to the segment before it, so it never gets a made up time. */
    private fun appendText(out: Collected, raw: String) {
        val text = raw.replace(WHITESPACE, " ")
        if (text.isEmpty()) return
        val last = out.segments.lastOrNull()
        if (last != null) {
            last.text += text
        } else if (text.isNotBlank()) {
            out.segments += Segment(text.trimStart(), null, null)
        }
    }

    // ---- entries ----------------------------------------------------------------------

    /**
     * Builds the line entry. Also returns one slot per lyric segment pair of the entry, in
     * the same order, so a transliteration can be matched to the pair it belongs to. A pause
     * pair gets an empty slot.
     */
    private fun toEntry(
        begin: Float,
        rawSegments: List<Segment>,
        translation: String?
    ): Pair<List<Pair<Float, String>>, List<Segment>> {
        val segments = mutableListOf<Segment>()
        var pending = ""
        rawSegments.forEach { raw ->
            if (raw.begin == null || raw.end == null) {
                // Text without a time of its own joins the next timed segment.
                pending += raw.text
            } else {
                segments += Segment(pending + raw.text, raw.begin, raw.end)
                pending = ""
            }
        }
        if (segments.isEmpty()) segments += Segment(pending, null, null)
        segments.firstOrNull()?.let { it.text = it.text.trimStart() }
        segments.lastOrNull()?.let { it.text = it.text.trimEnd() }

        val entry = mutableListOf<Pair<Float, String>>()
        val slots = mutableListOf<Segment>()
        entry += begin to ""

        val timed = segments.any { it.begin != null && it.end != null }
        if (!timed) {
            val text = segments.joinToString("") { it.text }
            entry += begin to text
            slots += Segment(text, null, null)
        } else {
            var clock = begin
            segments.forEach { segment ->
                val start = segment.begin ?: return@forEach
                val end = segment.end ?: return@forEach
                if (start > clock + 1f) {
                    entry += start to "" // a pause inside the line
                    slots += Segment("", clock, start)
                }
                entry += end to segment.text
                slots += segment
                clock = end
            }
        }
        entry += begin to ""
        if (translation != null) {
            entry += begin to ""
            entry += begin to translation
        }
        return entry to slots
    }

    /**
     * Gives every lyric segment slot the transliteration that belongs to it. The result has
     * one item per slot, empty for a pause. Equal counts pair up one to one, otherwise the
     * times decide, and without usable times nothing is shown rather than a guess.
     */
    private fun align(slots: List<Segment>, roman: List<Segment>): List<String>? {
        val sung = slots.indices.filter { slots[it].text.isNotEmpty() }
        if (sung.isEmpty() || roman.none { it.text.isNotBlank() }) return null

        val result = Array(slots.size) { StringBuilder() }
        fun clean(text: String) = text.replace(WHITESPACE, " ").trim()

        if (sung.size == 1) {
            result[sung.first()].append(clean(roman.joinToString("") { it.text }))
        } else if (sung.size == roman.size) {
            sung.forEachIndexed { i, slot -> result[slot].append(clean(roman[i].text)) }
        } else {
            var placed = false
            roman.forEach { r ->
                val rb = r.begin
                val re = r.end
                if (rb == null || re == null) return@forEach
                var best = -1
                var bestOverlap = 0f
                sung.forEach { i ->
                    val wb = slots[i].begin ?: return@forEach
                    val we = slots[i].end ?: return@forEach
                    val overlap = minOf(re, we) - maxOf(rb, wb)
                    if (overlap > bestOverlap) {
                        bestOverlap = overlap
                        best = i
                    }
                }
                if (best >= 0) {
                    if (result[best].isNotEmpty()) result[best].append(' ')
                    result[best].append(clean(r.text))
                    placed = true
                }
            }
            if (!placed) return null
        }
        return result.map { it.toString() }
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")
        private val OFFSET_TIME = Regex("^(\\d+(?:\\.\\d+)?)(h|m|s|ms)$")

        /** Reads a TTML time in milliseconds. Clock times and offset times are supported. */
        fun parseTime(raw: String?): Float? {
            val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null

            OFFSET_TIME.matchEntire(text)?.let { match ->
                val value = match.groupValues[1].toDoubleOrNull() ?: return null
                return when (match.groupValues[2]) {
                    "h" -> value * 3_600_000.0
                    "m" -> value * 60_000.0
                    "s" -> value * 1000.0
                    else -> value
                }.toFloat()
            }

            val parts = text.split(":")
            val numbers = parts.map { it.toDoubleOrNull() ?: return null }
            return when (numbers.size) {
                1 -> (numbers[0] * 1000.0).toFloat()
                2 -> ((numbers[0] * 60.0 + numbers[1]) * 1000.0).toFloat()
                3 -> ((numbers[0] * 3600.0 + numbers[1] * 60.0 + numbers[2]) * 1000.0).toFloat()
                else -> null // frame based clocks need a frame rate, so they are not read
            }
        }
    }
}
