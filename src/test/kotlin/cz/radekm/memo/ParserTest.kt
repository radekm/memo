// Copyright (c) 2016 Radek Micek

package cz.radekm.memo

import java.io.BufferedReader
import java.io.InputStreamReader

import org.junit.Assert.*
import org.junit.Test

class ParserTest {

    @Test
    fun `parseCommand - command with nonempty param`() {
        assertEquals("hello world", parseCommand("\\foo{hello world}", "foo"))
    }

    @Test
    fun `parseCommand - command with empty param`() {
        assertEquals("", parseCommand("\\foo{}", "foo"))
    }

    @Test
    fun `parseCommand - spaces after command`() {
        assertEquals("bar", parseCommand("\\foo{bar}   ", "foo"))
    }

    @Test
    fun `parseCommand - command with balanced braces in param`() {
        assertEquals("{} {}", parseCommand("\\foo{{} {}}", "foo"))
    }

    @Test
    fun `parseCommand - command with escaped characters in param`() {
        assertEquals("\\{", parseCommand("\\foo{\\{}", "foo"))
        assertEquals("\\}", parseCommand("\\foo{\\}}", "foo"))
        assertEquals("\\\\{}", parseCommand("\\foo{\\\\{}}", "foo"))
    }

    @Test
    fun `parseCommand - no command with given name`() {
        // Actual command name is `foo` but given name is `bar`.
        assertEquals(null, parseCommand("\\foo{param}", "bar"))
    }

    @Test
    fun `parseCommand - no command with param`() {
        // Command is ignored.
        assertEquals(null, parseCommand("\\foo bar", "foo"))
        assertEquals(null, parseCommand("\\foo {bar}", "foo"))
    }

    @Test
    fun `parseCommand - command not at beginning of line`() {
        // Command is ignored.
        assertEquals(null, parseCommand(" \\foo{param}", "foo"))
    }

    @Test
    fun `parseCommand - closing brace missing`() {
        try {
            parseCommand("\\foo{", "foo")
            fail("Unexpected success")
        } catch (e: ParseException) {}
    }

    @Test
    fun `parseCommand - text after command`() {
        try {
            parseCommand("\\foo{} bar", "foo")
            fail("Unexpected success")
        } catch (e: ParseException) {}
    }

    @Test
    fun `startsQuestion - command question alone on line`() {
        assertTrue(startsQuestion("\\question"))
    }

    @Test
    fun `startsQuestion - command question and text on same line`() {
        assertTrue(startsQuestion("\\question Foo"))
    }

    @Test
    fun `startsQuestion - no space between command question and text after it`() {
        assertFalse(startsQuestion("\\questionFoo"))
    }

    @Test
    fun `startsQuestion - command question not at beginning of line`() {
        assertFalse(startsQuestion(" \\question"))
    }

    @Test
    fun `parse - simple valid document`() {
        val input = "example.tex"
        val header =
                """\documentclass{exam}
                  |
                  |% Ensure unique label for each question.
                  |\makeatletter
                  |\let\orig@first@questionobject\first@questionobject
                  |\def\first@questionobject{%
                  |  \orig@first@questionobject
                  |  \edef\@queslabel{question@\arabic{section}@\arabic{subsection}@\arabic{question}}%
                  |}
                  |\makeatother
                  |
                  |% Use triangles instead of question numbers.
                  |\renewcommand{\questionlabel}{$\triangleright$}
                  |
                  |% Hide text "Solution:" before every answer.
                  |\renewcommand{\solutiontitle}{}
                  |
                  |\printanswers
                  |
                  |""".trimMargin()
        val q1 =
                """\question Q1
                  |
                  |\begin{solution}
                  |A1
                  |\end{solution}
                  |""".trimMargin()
        val q2 = "\\question Q2\n"
        val q3 = "\\question Q3\n"
        val q4 = "\\question Q4\n"
        val q5 = "\\question Q5\n"
        val q6 = "\\question Q6\n"
        val rootTopic = Topic(
                "",
                listOf(q1),
                listOf(
                        Topic("Sect 1", listOf(q2, q3),
                                listOf(
                                        Topic("Subsect 1.1", listOf(q4), listOf()),
                                        Topic("Subsect 1.2", listOf(q5), listOf()))),
                        Topic("Sect 2", listOf(q6), listOf())))
        val expOutput = Exam(header, rootTopic)

        BufferedReader(InputStreamReader(ClassLoader.getSystemResourceAsStream(input))).use {
            assertEquals(expOutput, parse(it))
        }
    }
}
