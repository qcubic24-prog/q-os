package com.qcubic.qos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = Ui.screen(this, "Settings")
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(24))
        }
        body.addView(ScrollView(this).apply { addView(col) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        col.addView(toggle("Lite mode", "Plain wallpaper and no animations. Best for 3–4 GB RAM phones.",
            Prefs.lite(this)) { Prefs.setLite(this, it) })
        col.addView(toggle("24-hour clock", "Show taskbar time as 14:30 instead of 2:30 PM.",
            Prefs.use24h(this)) { Prefs.set24h(this, it) })

        col.addView(Ui.button(this, "Make Q-OS my home screen") {
            try { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            catch (e: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }, lp(top = 8))
        col.addView(Ui.button(this, "Phone settings") { startActivity(Intent(Settings.ACTION_SETTINGS)) }, lp(top = 12))

        col.addView(Ui.text(this, "About", 17f, Ui.TEXT, true).apply { setPadding(0, dp(28), 0, dp(6)) })
        col.addView(Ui.text(this,
            "Q-OS 0.1.0 by Q-CUBIC\n\n" +
            "A desktop for Android that opens Android, Windows and Linux apps from one place.\n\n" +
            "Engines and credits:\n" +
            "• Winlator (Wine + Box64) for Windows apps\n" +
            "• Termux and proot-distro for Linux\n" +
            "These are separate open-source projects under their own licences.",
            14f, Ui.SUBTEXT))
    }

    private fun lp(top: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { topMargin = dp(top) }

    private fun toggle(title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.glass(this@SettingsActivity, Ui.CARD, 16)
            setPadding(dp(16), dp(14), dp(12), dp(14))
            layoutParams = lp(0).apply { bottomMargin = dp(12) }
        }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(Ui.text(this, title, 16f, Ui.TEXT, true))
        texts.addView(Ui.text(this, sub, 13f, Ui.SUBTEXT).apply { setPadding(0, dp(4), 0, 0) })
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        @Suppress("DEPRECATION")
        row.addView(Switch(this).apply {
            isChecked = value
            setOnCheckedChangeListener { _, v -> onChange(v) }
        })
        return row
    }
}
