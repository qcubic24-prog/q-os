package com.qcubic.qos

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

/** Read-only provider that lets the package installer and other apps open a file the user tapped. */
class FileShareProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.qcubic.qos.files"

        fun uriFor(f: File): Uri {
            val b = Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("f")
            f.absolutePath.split('/').filter { it.isNotEmpty() }.forEach { b.appendPath(it) }
            return b.build()
        }

        private fun fileFor(uri: Uri): File {
            val segs = uri.pathSegments
            if (segs.isEmpty() || segs[0] != "f") throw FileNotFoundException(uri.toString())
            val f = File("/" + segs.drop(1).joinToString("/")).canonicalFile
            if (!f.isFile) throw FileNotFoundException(uri.toString())
            return f
        }
    }

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor =
        ParcelFileDescriptor.open(fileFor(uri), ParcelFileDescriptor.MODE_READ_ONLY)

    override fun getType(uri: Uri): String {
        val ext = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase() ?: ""
        if (ext == "apk") return "application/vnd.android.package-archive"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val f = fileFor(uri)
        val c = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
        c.addRow(arrayOf<Any>(f.name, f.length()))
        return c
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
