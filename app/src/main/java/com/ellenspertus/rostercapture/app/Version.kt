package com.ellenspertus.rostercapture.app

import com.ellenspertus.rostercapture.BuildConfig
import kotlin.text.removePrefix
import org.json.JSONObject
import timber.log.Timber

/**
 * A version of the application. Fields [releaseNotes] and [downloadUrl] are set only
 * for the latest version, not for the current version or skipped version.
 */
class Version(val name: String) : Comparable<Version> {
    private val parts = name.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val isDownloadable
        get() = downloadUrl != null

    var releaseNotes: String = ""
        private set

    var downloadUrl: String? = null
        private set

    constructor(json: JSONObject) : this(json.getString("tag_name")) {
        releaseNotes = extractReleaseNotes(json)
        downloadUrl = extractDownloadUrl(json)
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

    companion object {
        val current = Version(BuildConfig.VERSION_NAME)

        private fun extractReleaseNotes(json: JSONObject) =
            try {
                json.getString("body")
                    .replace(
                        Regex("#{1,6}\\s*"),
                        ""
                    )  // Removes # through ###### with optional spaces
                    .replace(Regex("\\*{2,}"), "")     // Removes ** for bold
                    .replace(Regex("\\n{3,}"), "\n\n") // Collapse multiple newlines
                    .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "• ") // Lists
                    .trim()
            } catch (e: Exception) {
                ""
            }

        private fun extractDownloadUrl(json: JSONObject): String? =
            try {
                json.getJSONArray("assets")
                    .let { assets ->
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.getString("name").endsWith(".apk")) {
                                return@let asset.getString("browser_download_url")
                            }
                        }
                        Timber.e("Unable to extract download URL from JSON")
                        null
                    }
            } catch (e: Exception) {
                Timber.e(e, "Exception extracting download URL")
                null
            }
    }
}