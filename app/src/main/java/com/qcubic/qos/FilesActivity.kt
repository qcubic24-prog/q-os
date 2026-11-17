package com.qcubic.qos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.io.File

/** Q-OS Files: browse phone storage; tapping .apk/.exe/.deb opens it in the right engine. */
class FilesActivity : Activity() {

    private val home: File = Environment.getExternalStorageDirectory()
    private var dir: File = home
    private var entries: List<File> = emptyList()

    private lateinit var pathView: TextView
    private lateinit var list: ListView
    private lateinit var grantBox: LinearLayout
    private lateinit var emptyView: TextView
    private val adapter = RowAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = Ui.screen(this, "Files")

        pathView = Ui.text(this, "", 13f, Ui.SUBTEXT).apply { setPadding(dp(20), 0, dp(20), dp(8)) }
        body.addView(pathView)

        grantBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            addView(Ui.text(this@FilesActivity, "Q-OS needs access to your files to open apps and documents.", 16f))
            addView(Ui.button(this@FilesActivity, "Allow file access") { requestAccess() },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) })
        }
        body.addView(grantBox)

        emptyView = Ui.text(this, "This folder is empty", 15f, Ui.SUBTEXT).apply {
            setPadding(dp(20), dp(20), dp(20), dp(20)); visibility = View.GONE
        }
        body.addView(emptyView)

        list = ListView(this).apply {
            adapter = this@FilesActivity.adapter
            divider = null
            setOnItemClickListener { _, _, pos, _ ->
                val f = entries[pos]
                if (f.isDirectory) load(f) else FileOpener.open(this@FilesActivity, f)
            }
        }
        body.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    override fun onResume() {
        super.onResume()
        val ok = hasAccess()
        grantBox.visibility = if (ok) View.GONE else View.VISIBLE
        list.visibility = if (ok) View.VISIBLE else View.GONE
        if (ok) load(dir)
    }

    private fun hasAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onResume()
    }

    private fun load(d: File) {
        dir = d
        pathView.text = if (d == home) "Phone storage" else "Phone storage" + d.absolutePath.removePrefix(home.absolutePath)
        entries = (d.listFiles() ?: emptyArray())
            .filter { !it.name.startsWith(".") }
            .sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
        adapter.notifyDataSetChanged()
        emptyView.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        list.setSelection(0)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val parent = dir.parentFile
        if (dir != home && parent != null && hasAccess()) load(parent) else super.onBackPressed()
    }

    private fun sizeText(bytes: Long): String = when {
        bytes >= 1L shl 30 -> String.format("%.1f GB", bytes / (1L shl 30).toDouble())
        bytes >= 1L shl 20 -> String.format("%.1f MB", bytes / (1L shl 20).toDouble())
        bytes >= 1L shl 10 -> String.format("%.0f KB", bytes / (1L shl 10).toDouble())
        else -> "$bytes B"
    }

    inner class RowAdapter : BaseAdapter() {
        override fun getCount() = entries.size
        override fun getItem(position: Int) = entries[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val ctx = this@FilesActivity
            val row = (convertView as? LinearLayout) ?: LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(10), dp(16), dp(10))
                addView(ImageView(ctx), LinearLayout.LayoutParams(dp(40), dp(40)).apply { rightMargin = dp(14) })
                addView(LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(Ui.text(ctx, "", 15f).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.MIDDLE })
                    addView(Ui.text(ctx, "", 12f, Ui.SUBTEXT))
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
            val f = entries[position]
            val kind = FileOpener.kindOf(f)
            (row.getChildAt(0) as ImageView).setImageDrawable(LetterIcon(kind.letter, kind.color))
            val texts = row.getChildAt(1) as LinearLayout
            (texts.getChildAt(0) as TextView).text = f.name
            (texts.getChildAt(1) as TextView).text =
                if (f.isDirectory) "Folder" else "${kind.label} · ${sizeText(f.length())}"
            return row
        }
    }
}
