// Copyright (c) 2016 Radek Micek

package cz.radekm.memo

import java.io.BufferedReader
import java.util.*

private class TopicBuilder(val title : String) {
    private val questions = mutableListOf<String>()
    private val subtopics = mutableListOf<Topic>()

    fun addQuestion(s: String) = questions.add(s)
    fun addSubtopic(t: Topic) = subtopics.add(t)

    fun build() = Topic(
            title = title,
            questions = questions.toList(),
            subtopics = subtopics.toList())
}

private class TopicStack {
    private val stack = Stack<TopicBuilder>()

    val size: Int
        get() = stack.size

    fun push(title: String) { stack.push(TopicBuilder(title)) }
    fun pop() = stack.pop().build()

    fun addQuestion(s: String) = stack.peek().addQuestion(s)
    fun addSubtopic(t: Topic) = stack.peek().addSubtopic(t)
}

private enum class Environment {
    NoEnvironment, Document, Questions
}

private fun StringBuilder.pop(): String {
    val str = toString()
    setLength(0)
    return str
}

/**
 * Parsing of a Latex document failed.
 */
class ParseException(val msg: String, val lineNo: Int? = null) : Exception(
        if (lineNo == null)
            msg
        else
            "Line $lineNo: $msg")

private fun error(msg: String): Nothing = throw ParseException(msg)

private class LatexHandler() {
    private var environment = Environment.NoEnvironment

    private val openTopics = TopicStack()
    private val headerBuilder = StringBuilder()
    private val questionBuilder = StringBuilder()

    private fun closeTopicsExcept(remain: Int) {
        if (remain < 1)
            throw IllegalArgumentException("remain must be positive.")
        while (openTopics.size > remain)
            openTopics.addSubtopic(openTopics.pop())
    }

    private val isQuestionOpen: Boolean
        get() = questionBuilder.isNotEmpty()

    private fun closeQuestion () {
        if (isQuestionOpen)
            openTopics.addQuestion(questionBuilder.pop())
    }

    fun beginDocument() {
        when (environment) {
            Environment.NoEnvironment -> Unit
            Environment.Document,
            Environment.Questions ->
                error("Cannot open document because it is already open.")
        }

        openTopics.push("") // Root topic.
        environment = Environment.Document
    }

    fun endDocument(): Exam {
        when (environment) {
            Environment.Document -> Unit
            Environment.NoEnvironment -> error("Cannot close document because it isn't open.")
            Environment.Questions ->
                error("Cannot close document because environment questions is still open.")
        }

        closeTopicsExcept(1)
        environment = Environment.NoEnvironment
        return Exam(
                header = headerBuilder.pop(),
                topic = openTopics.pop()
        )
    }

    fun section(title: String) {
        when (environment) {
            Environment.Document -> Unit
            Environment.NoEnvironment -> error("Section must be inside document.")
            Environment.Questions -> error("Section must not be inside environment questions.")
        }

        closeTopicsExcept(1)
        openTopics.push(title)
    }

    fun subsection(title: String) {
        when (environment) {
            Environment.Document -> Unit
            Environment.NoEnvironment -> error("Subsection must be inside document.")
            Environment.Questions -> error("Subsection must not be inside environment questions.")
        }

        closeTopicsExcept(2)
        if (openTopics.size != 2)
            error("Subsection must be under section.")
        openTopics.push(title)
    }

    fun beginQuestions() {
        when (environment) {
            Environment.Document -> Unit
            Environment.NoEnvironment -> error("Environment questions must be inside document.")
            Environment.Questions ->
                error("Cannot open environment questions because it is already open.")
        }

        environment = Environment.Questions
    }

    fun endQuestions() {
        when (environment) {
            Environment.Questions -> Unit
            Environment.NoEnvironment,
            Environment.Document ->
                error("Cannot close environment questions because it isn't open.")
        }

        closeQuestion()
        environment = Environment.Document
    }

    fun question(line: String) {
        when (environment) {
            Environment.Questions -> Unit
            Environment.NoEnvironment,
            Environment.Document ->
                error("Question must be inside environment questions.")
        }

        closeQuestion()
        questionBuilder.appendln(line)
    }

    fun other(line: String) {
        when (environment) {
            Environment.NoEnvironment -> headerBuilder.appendln(line)
            Environment.Document -> Unit
            Environment.Questions ->
                if (isQuestionOpen)
                    questionBuilder.appendln(line)
        }
    }
}

/**
 * Parses a command with the given name and returns its parameter.
 *
 * Returns `null` iff the line doesn't start with the string
 * `"\\" + name + "{"`.
 *
 * @throws [ParseException] if the command isn't alone on the line
 * (i.e. the line contains a non-whitespace character after the command)
 * or if the command doesn't have exactly one parameter on the line
 * (i.e. it has more parameters or the parameter continues to the next line).
 */
internal fun parseCommand(line: String, name: String): String? {
    val ln = line.trimEnd()

    if (!ln.startsWith("\\" + name + "{"))
        return null

    tailrec fun findParamEnd(i: Int, openBraces: Int): Int = when {
        // Reached EOL and all braces are closed.
        i == ln.length && openBraces == 0 -> i - 1 // Index of the last closing brace.
        // Reached EOL.
        i == ln.length -> error("Cannot parse parameter, closing brace is missing.")
        // Command is not alone on the line.
        openBraces == 0 -> error("Rest of line after command must be blank.")
        ln.startsWith("{", i) -> findParamEnd(i + 1, openBraces + 1)
        ln.startsWith("}", i) -> findParamEnd(i + 1, openBraces - 1)
        // Escaped character.
        ln.startsWith("\\{", i) ||
                ln.startsWith("\\}", i) ||
                ln.startsWith("\\\\", i) -> findParamEnd(i + 2, openBraces)
        else -> findParamEnd(i + 1, openBraces)
    }

    val startIndex = 2 + name.length
    val endIndex = findParamEnd(startIndex, 1)

    return ln.substring(startIndex, endIndex)
}

private val questionRegex = """^\\question(?:\s.*)?$""".toRegex()

/**
 * Determines whether a question starts at the given line.
 *
 * A question starts at the given line iff the line starts
 * with the command `\question` without any parameters.
 */
internal fun startsQuestion(line: String): Boolean = questionRegex.matches(line)

private class Parser {
    private var lineNo = 0
    private var result: Exam? = null
    private val handler = LatexHandler()

    private fun String.cmd(name: String): String? = parseCommand(this, name)

    private fun String.startsQuestion() = startsQuestion(this)

    fun feed(line: String?): Exam? {
        lineNo += 1

        try {
            when {
                // Exam has been already parsed - do nothing.
                result != null -> Unit
                // Exam hasn't been parsed but EOF has been reached.
                line == null -> error("Unexpected end of input.")
                line.cmd("begin") == "document" -> handler.beginDocument()
                line.cmd("end") == "document" -> result = handler.endDocument()
                line.cmd("begin") == "questions" -> handler.beginQuestions()
                line.cmd("end") == "questions" -> handler.endQuestions()
                line.cmd("section") != null -> handler.section(line.cmd("section").toString())
                line.cmd("subsection") != null ->
                    handler.subsection(line.cmd("subsection").toString())
                line.startsQuestion() -> handler.question(line)
                else -> handler.other(line)
            }
        } catch (e: ParseException) {
            // Rethrow with `lineNo`.
            throw ParseException(e.msg, lineNo)
        }

        return result
    }
}

/**
 * Parses a Latex document with the document class `exam`.
 *
 * @throws ParseException
 */
fun parse(r: BufferedReader): Exam {
    val p = Parser()
    var result: Exam? = null
    while (result == null)
        result = p.feed(r.readLine())
    return result
}
