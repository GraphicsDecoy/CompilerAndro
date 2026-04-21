package com.emergent.codeide.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.emergent.codeide.ui.theme.*

/** Light-weight regex-based syntax highlighter for C/C++/JS/Python/Kotlin. */
object SyntaxHighlighter {

    private val cppKeywords = setOf(
        "alignas","alignof","and","asm","auto","bool","break","case","catch","char",
        "class","const","constexpr","continue","decltype","default","delete","do",
        "double","else","enum","explicit","export","extern","false","float","for",
        "friend","goto","if","inline","int","long","mutable","namespace","new",
        "noexcept","not","nullptr","operator","or","private","protected","public",
        "register","return","short","signed","sizeof","static","static_cast","struct",
        "switch","template","this","throw","true","try","typedef","typeid","typename",
        "union","unsigned","using","virtual","void","volatile","while","std","string",
        "vector","cout","cin","endl","printf","scanf"
    )
    private val pyKeywords = setOf(
        "and","as","assert","async","await","break","class","continue","def","del",
        "elif","else","except","finally","for","from","global","if","import","in",
        "is","lambda","None","nonlocal","not","or","pass","raise","return","True",
        "False","try","while","with","yield","self","print"
    )
    private val jsKeywords = setOf(
        "var","let","const","function","return","if","else","for","while","do",
        "switch","case","break","continue","new","this","class","extends","super",
        "import","export","from","default","async","await","try","catch","finally",
        "throw","typeof","instanceof","null","undefined","true","false"
    )
    private val ktKeywords = setOf(
        "fun","val","var","class","object","interface","if","else","when","for",
        "while","do","return","break","continue","package","import","public","private",
        "protected","internal","override","open","abstract","sealed","data","inline",
        "suspend","null","true","false","this","super","is","as","in","out"
    )

    private val stringRe = Regex("\"(\\\\.|[^\"\\\\])*\"|'(\\\\.|[^'\\\\])*'")
    private val numRe = Regex("\\b\\d+(\\.\\d+)?[fFLlUu]?\\b")
    private val identRe = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
    private val lineCmtRe = Regex("//[^\\n]*|#[^\\n]*")
    private val blockCmtRe = Regex("/\\*[\\s\\S]*?\\*/")
    private val preRe = Regex("^\\s*#\\w+.*$", RegexOption.MULTILINE)
    private val fnCallRe = Regex("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")

    fun keywordsFor(lang: String): Set<String> = when (lang.lowercase()) {
        "cpp","c","h","hpp","cc","cxx" -> cppKeywords
        "py","python" -> pyKeywords
        "js","ts","jsx","tsx","javascript","typescript" -> jsKeywords
        "kt","kotlin" -> ktKeywords
        else -> cppKeywords
    }

    fun highlight(code: String, lang: String): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val keywords = keywordsFor(lang)
        val spans = mutableListOf<Triple<Int, Int, SpanStyle>>()
        val masked = BooleanArray(code.length) // chars already styled (strings/comments)

        fun mark(range: IntRange) { for (i in range) if (i in masked.indices) masked[i] = true }
        fun add(range: IntRange, style: SpanStyle) {
            spans.add(Triple(range.first, range.last + 1, style))
        }

        // Preprocessor (C/C++)
        if (lang.lowercase() in setOf("cpp","c","h","hpp","cc","cxx")) {
            preRe.findAll(code).forEach {
                add(it.range, SpanStyle(color = SynPreproc)); mark(it.range)
            }
        }
        // Block comments
        blockCmtRe.findAll(code).forEach {
            add(it.range, SpanStyle(color = SynComment)); mark(it.range)
        }
        // Line comments
        lineCmtRe.findAll(code).forEach { m ->
            if (!masked[m.range.first]) { add(m.range, SpanStyle(color = SynComment)); mark(m.range) }
        }
        // Strings
        stringRe.findAll(code).forEach { m ->
            if (!masked[m.range.first]) { add(m.range, SpanStyle(color = SynString)); mark(m.range) }
        }
        // Numbers
        numRe.findAll(code).forEach { m ->
            if (!masked[m.range.first]) add(m.range, SpanStyle(color = SynNumber))
        }
        // Identifiers: keywords + function calls
        identRe.findAll(code).forEach { m ->
            if (masked[m.range.first]) return@forEach
            val word = m.value
            if (word in keywords) {
                add(m.range, SpanStyle(color = SynKeyword))
            }
        }
        fnCallRe.findAll(code).forEach { m ->
            val nameRange = m.groups[1]!!.range
            if (!masked[nameRange.first] && code.substring(nameRange) !in keywords) {
                add(nameRange, SpanStyle(color = SynFunction))
            }
        }

        spans.sortBy { it.first }
        spans.forEach { (s, e, st) -> builder.addStyle(st, s, e) }
        return builder.toAnnotatedString()
    }

    fun detectLang(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "cpp","cc","cxx","hpp","h","hxx","c" -> "cpp"
            "py" -> "py"
            "js","jsx","mjs" -> "js"
            "ts","tsx" -> "ts"
            "kt","kts" -> "kt"
            else -> "txt"
        }
    }
}
