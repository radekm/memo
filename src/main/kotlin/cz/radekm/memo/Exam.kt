// Copyright (c) 2016 Radek Micek

package cz.radekm.memo

/**
 * [Exam] represents a parsed Latex document with the document class `exam`.
 *
 * @param header the header of the Latex document (all lines before the line
 *               with `\begin{document}` which is not a part of the header).
 * @param topic the tree of topics. The questions from the Latex document are organized
 *              into topics. The root topic contains the questions
 *              which don't belong to any section or subsection.
 *              The other topics correspond to the sections and the subsections
 *              and contain the respective questions.
 */
data class Exam(
        val header: String,
        val topic: Topic
)

/**
 * Each topic except a root topic corresponds to a section
 * or to a subsection from a Latex document.
 * @param title the name of the corresponding (sub)section
 *              or an empty string if this topic is a root topic.
 * @param questions the questions from the corresponding (sub)section
 *                  or the questions which don't belong to any section
 *                  or subsection if this topic is a root topic.
 */
data class Topic(
        val title: String,
        val questions: List<String>,
        val subtopics: List<Topic>
)
