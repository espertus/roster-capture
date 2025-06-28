package com.ellenspertus.rostercapture.students

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class MediaManager {
    companion object {
        fun uriToFile(context: Context, uri: Uri?): File? =
            uri?.lastPathSegment?.let { fileName ->
                File(context.filesDir, fileName)
            }

        fun fileToUri(context: Context, file: File): Uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
    }
}