package com.qcubic.qos

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import java.io.File

enum class FileKind(val label: String, val letter: String, val color: Int) {
    FOLDER("Folder", "▸", 0xFFF2B233.toInt()),
    ANDROID("Android app", "A", 0xFF3DDC84.toInt()),
    WINDOWS("Windows app", "W", 0xFF2D7FF9.toInt()),
    LINUX("Linux file", "L", 0xFFE8772E.toInt()),
    OTHER("File", "•", 0xFF4A5568.toInt())
}

/** Decides which engine opens a file: the core "smart file manager" idea of Q-OS. */
object FileOpener {
    private const val APK_MIME = "application/vnd.android.package-archive"

    fun kindOf(f: File): FileKind {
        if (f.isDirectory) return FileKind.FOLDER
        return when (f.extension.lowercase()) {
            "apk" -> FileKind.ANDROID
            "exe", "msi", "bat" -> FileKind.WINDOWS
            "deb", "rpm", "sh", "py", "appimage" -> FileKind.LINUX
            else -> FileKind.OTHER
        }
    }

    fun open(act: Activity, f: File) {
        when (kindOf(f)) {
            FileKind.ANDROID -> installApk(act, f)
            FileKind.WINDOWS -> openWindows(act, f)
            FileKind.LINUX -> openLinux(act, f)
            FileKind.OTHER -> openOther(act, f)
            FileKind.FOLDER -> {}
        }
    }

    private fun installApk(act: Activity, f: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !act.packageManager.canRequestPackageInstalls()) {
            AlertDialog.Builder(act, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Allow app installs")
                .setMessage("To install Android apps from Files, allow Q-OS to install apps, then tap the file again.")
                .setPositiveButton("Allow") { _, _ ->
                    act.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${act.packageName}")))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(FileShareProvider.uriFor(f), APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        tryStart(act, i)
    }

    private fun openWindows(act: Activity, f: File) {
        if (Engines.installedPackage(act, Engines.WINDOWS) == null) {
            missingEngine(act, Engines.WINDOWS); return
        }
        copy(act, f.absolutePath)
        Engines.open(act, Engines.WINDOWS)
        Toast.makeText(
            act,
            "Windows engine opened. File path copied:\n${f.absolutePath}\nIn Winlator your phone storage is usually drive D:.",
            Toast.LENGTH_LONG
        ).show()
    }

    fun linuxCommand(f: File): String {
        val p = "'" + f.absolutePath.replace("'", "'\\''") + "'"
        return when (f.extension.lowercase()) {
            "sh" -> "bash $p"
            "py" -> "python3 $p"
            "deb" -> "proot-distro login ubuntu -- apt install -y $p"
            "rpm" -> "proot-distro login ubuntu -- bash -c \"apt install -y alien && alien -i $p\""
            "appimage" -> "chmod +x $p && $p"
            else -> "ls -l $p"
        }
    }

    private fun openLinux(act: Activity, f: File) {
        if (Engines.installedPackage(act, Engines.LINUX) == null) {
            missingEngine(act, Engines.LINUX); return
        }
        copy(act, linuxCommand(f))
        Engines.open(act, Engines.LINUX)
        Toast.makeText(act, "Linux engine opened. Command copied, long-press in the terminal and paste.", Toast.LENGTH_LONG).show()
    }

    private fun openOther(act: Activity, f: File) {
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(FileShareProvider.uriFor(f), mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        tryStart(act, Intent.createChooser(i, "Open with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun missingEngine(act: Activity, e: Engine) {
        AlertDialog.Builder(act, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("${e.name} needed")
            .setMessage(e.description + "\n\nInstall it once from the Engines screen, then tap this file again.")
            .setPositiveButton("Open Engines") { _, _ -> act.startActivity(Intent(act, EnginesActivity::class.java)) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun copy(ctx: Context, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Q-OS", text))
    }

    private fun tryStart(act: Activity, i: Intent) {
        try {
            act.startActivity(i)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(act, "No app on this phone can open this file", Toast.LENGTH_SHORT).show()
        }
    }
}
