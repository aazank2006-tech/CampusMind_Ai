package com.example.data.model

import java.util.Locale
import java.util.regex.Pattern

data class StudentMemory(
    val name: String? = null,
    val major: String? = null,
    val year: String? = null,
    val university: String? = null,
    val customFacts: Map<String, String> = emptyMap()
) {
    val isEmpty: Boolean
        get() = name.isNullOrBlank() && major.isNullOrBlank() && year.isNullOrBlank() && university.isNullOrBlank() && customFacts.isEmpty()

    fun toPromptBlock(): String {
        if (isEmpty) return ""
        val lines = mutableListOf("Known facts about this user:")
        name?.takeIf { it.isNotBlank() }?.let { lines.add("  - Name: $it") }
        major?.takeIf { it.isNotBlank() }?.let { lines.add("  - Major: $it") }
        year?.takeIf { it.isNotBlank() }?.let { lines.add("  - Year: ${it.replaceFirstChar { c -> c.uppercase() }}") }
        university?.takeIf { it.isNotBlank() }?.let { lines.add("  - University: $it") }
        for ((k, v) in customFacts) {
            lines.add("  - ${k.replaceFirstChar { it.uppercase() }}: $v")
        }
        return lines.joinToString("\n")
    }

    companion object {
        private val NAME_PATTERN = Pattern.compile(
            """(?:my name is|i'm|i am|call me)\s+([A-Za-z]+(?: [A-Za-z]+)?)""",
            Pattern.CASE_INSENSITIVE
        )

        private val MAJOR_PATTERN = Pattern.compile(
            """(?:studying|majoring in|i study|my major is|my field is)\s+([a-zA-Z &]+?)(?:\.|,|\n|$)""",
            Pattern.CASE_INSENSITIVE
        )

        private val YEAR_PATTERN = Pattern.compile(
            """\b(freshman|sophomore|junior|senior|1st year|2nd year|3rd year|4th year|first year|second year|third year|fourth year)\b""",
            Pattern.CASE_INSENSITIVE
        )

        private val UNI_PATTERN = Pattern.compile(
            """(?:at|attend|go to|from|study at)\s+([A-Za-z][a-zA-Z ]+?(?:University|College|Institute|School|Academy))""",
            Pattern.CASE_INSENSITIVE
        )

        fun extractFromMessage(current: StudentMemory, text: String): StudentMemory {
            var newName = current.name
            var newMajor = current.major
            var newYear = current.year
            var newUni = current.university

            // Name
            val nameMatcher = NAME_PATTERN.matcher(text)
            if (nameMatcher.find()) {
                val candidate = nameMatcher.group(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    newName = candidate.split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    }
                }
            }

            // Major
            val majorMatcher = MAJOR_PATTERN.matcher(text)
            if (majorMatcher.find()) {
                val candidate = majorMatcher.group(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    newMajor = candidate.split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    }
                }
            }

            // Year
            val yearMatcher = YEAR_PATTERN.matcher(text)
            if (yearMatcher.find()) {
                val candidate = yearMatcher.group(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    newYear = candidate.lowercase(Locale.ROOT)
                }
            }

            // University
            val uniMatcher = UNI_PATTERN.matcher(text)
            if (uniMatcher.find()) {
                val candidate = uniMatcher.group(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    newUni = candidate
                }
            }

            return current.copy(
                name = newName,
                major = newMajor,
                year = newYear,
                university = newUni
            )
        }
    }
}
