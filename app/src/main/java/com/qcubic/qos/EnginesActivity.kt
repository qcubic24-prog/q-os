package com.qcubic.qos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

/** Shows the Windows and Linux engines, whether they are installed, and how to set them up. */
class EnginesActivity : Activity() {

    private lateinit var cards: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = Ui.screen(this, "Engines")
        cards = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(24))
        }
        body.addView(ScrollView(this).apply { addView(cards) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = Ui.glass(this@EnginesActivity, Ui.CARD, 16)
        setPadding(dp(16), dp(16), dp(16), dp(16))
    }

    private fun addCard(c: LinearLayout) = cards.addView(c,
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14) })

    private fun render() {
        cards.removeAllViews()

        val androidCard = card()
        androidCard.addView(Ui.text(this, "Android engine", 17f, Ui.TEXT, true))
        androidCard.addView(Ui.text(this, "Built in. Tap any .apk in Files to install it.  ✓ Ready", 14f, Ui.SUBTEXT).apply { setPadding(0, dp(6), 0, 0) })
        addCard(androidCard)

        for (e in Engines.ALL) {
            val installed = Engines.installedPackage(this, e) != null
            val c = card()
            val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            head.addView(ImageView(this).apply { setImageDrawable(LetterIcon(e.letter, e.color)) },
                LinearLayout.LayoutParams(dp(40), dp(40)).apply { rightMargin = dp(12) })
            head.addView(Ui.text(this, e.name, 17f, Ui.TEXT, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            head.addView(Ui.text(this, if (installed) "✓ Installed" else "Not installed", 13f,
                if (installed) 0xFF3DDC84.toInt() else 0xFFFF8A80.toInt(), true))
            c.addView(head)
            c.addView(Ui.text(this, e.description, 14f, Ui.SUBTEXT).apply { setPadding(0, dp(10), 0, dp(12)) })
            c.addView(Ui.button(this, if (installed) "Open" else "Download engine") {
                if (installed) Engines.open(this, e)
                else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(e.downloadUrl)))
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            if (!installed) {
                c.addView(Ui.text(this, "Download the latest .apk from the page, then install it from Q-OS Files (Download folder).",
                    12f, Ui.SUBTEXT).apply { setPadding(0, dp(10), 0, 0) })
            }
            addCard(c)
        }

        val setup = card()
        setup.addView(Ui.text(this, "Linux one-time setup", 17f, Ui.TEXT, true))
        setup.addView(Ui.text(this, "After installing the Linux engine, open it once and paste this command. It installs Ubuntu (about 10 minutes on Wi-Fi).",
            14f, Ui.SUBTEXT).apply { setPadding(0, dp(8), 0, dp(10)) })
        setup.addView(Ui.text(this, Engines.LINUX_SETUP, 13f, Ui.ACCENT).apply {
            typeface = android.graphics.Typeface.MONOSPACE
            background = Ui.glass(this@EnginesActivity, 0x44000000, 10)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        })
        setup.addView(Ui.button(this, "Copy command") {
            FileOpener.copy(this, Engines.LINUX_SETUP)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        addCard(setup)

        val apple = card()
        apple.addView(Ui.text(this, "Apple apps", 17f, Ui.TEXT, true))
        apple.addView(Ui.text(this, "Not supported. iOS and macOS apps cannot run reliably outside Apple devices.",
            14f, Ui.SUBTEXT).apply { setPadding(0, dp(8), 0, 0) })
        addCard(apple)
    }
}
