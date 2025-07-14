package com.ellenspertus.rostercapture.updates

import kotlin.text.removePrefix
import org.json.JSONObject

class Version(val name: String) : Comparable<Version> {

    private val parts = name.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

    var releaseNotes: String = ""
        private set

    constructor(json: JSONObject) : this(json.getString("tag_name")) {
        releaseNotes = getReleaseNotes(json)
    }

    private fun getReleaseNotes(json: JSONObject) =
        try {
            json.getString("body")
                .replace(Regex("#{1,6}\\s*"), "")  // Removes # through ###### with optional spaces
                .replace(Regex("\\*{2,}"), "")     // Removes ** for bold
                .replace(Regex("\\n{3,}"), "\n\n") // Collapse multiple newlines
                .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "• ") // Lists
                .trim()
        } catch (e: Exception) {
            ""
        }

    override fun toString() = parts.joinToString(".")

    override fun compareTo(other: Version): Int {
        for (i in 0 until maxOf(parts.size, other.parts.size)) {
            val part = parts.getOrNull(i) ?: 0
            val otherPart = other.parts.getOrNull(i) ?: 0

            if (part != otherPart) {
                return part.compareTo(otherPart)
            }
        }
        return 0
    }

    override fun equals(other: Any?) =
        other is Version && toString() == other.toString()

    override fun hashCode() = toString().hashCode()
}