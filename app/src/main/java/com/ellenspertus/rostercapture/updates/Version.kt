package com.ellenspertus.rostercapture.updates

import kotlin.text.removePrefix

class Version(val name: String) : Comparable<Version> {
    private val parts = name.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

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