package com.qcubic.qos

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView

/** The Q-OS desktop: wallpaper, desktop icons, taskbar and Start menu. Works as the phone's home screen. */
class MainActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var desktopAdapter: TileAdapter
    private lateinit var startAdapter: TileAdapter
    private lateinit var startMenu: LinearLayout
    private lateinit var search: EditText
    private lateinit var taskbarApps: LinearLayout
    private lateinit var battery: TextView
    private lateinit var clock: TextClock
    private var apps: List<AppEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        buildUi()
    }

    private fun buildUi() {
        root = FrameLayout(this)
        root.background = Ui.wallpaper(Prefs.lite(this))

        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // Desktop icons
        desktopAdapter = TileAdapter(this, 52)
        val desktop = GridView(this).apply {
            numColumns = GridView.AUTO_FIT
            columnWidth = dp(84)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            verticalSpacing = dp(10)
            setPadding(dp(8), dp(40), dp(8), dp(8))
            clipToPadding = false
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            adapter = desktopAdapter
            setOnItemClickListener { _, _, pos, _ -> hideStart(); desktopAdapter.items[pos].action() }
            setOnItemLongClickListener { _, _, pos, _ -> desktopAdapter.items[pos].longAction?.invoke(); true }
        }
        column.addView(desktop, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        column.addView(buildTaskbar(), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)))
        root.addView(column)

        startMenu = buildStartMenu()
        val dm = resources.displayMetrics
        val w = minOf(dm.widthPixels - dp(20), dp(440))
        val h = (dm.heightPixels * 0.68f).toInt()
        root.addView(startMenu, FrameLayout.LayoutParams(w, h, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(68)
        })
        startMenu.visibility = View.GONE

        setContentView(root)
    }

    private fun buildTaskbar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.glass(this@MainActivity, 0xD9101826.toInt(), 0)
            setPadding(dp(10), 0, dp(14), 0)
        }
        // Start button
        bar.addView(Ui.text(this, "Q", 20f, Ui.TEXT, true).apply {
            gravity = Gravity.CENTER
            background = Ui.glass(this@MainActivity, 0x8835C9E0.toInt(), 20)
            setOnClickListener { toggleStart() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(10) })

        bar.addView(taskIcon(LetterIcon("F", 0xFFF2B233.toInt())) { open(FilesActivity::class.java) })
        bar.addView(taskIcon(LetterIcon("E", 0xFF7B5CFA.toInt())) { open(EnginesActivity::class.java) })

        taskbarApps = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bar.addView(taskbarApps)

        bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))

        battery = Ui.text(this, "", 13f, Ui.SUBTEXT).apply { setPadding(0, 0, dp(12), 0) }
        bar.addView(battery)
        clock = TextClock(this).apply {
            textSize = 15f
            setTextColor(Ui.TEXT)
        }
        bar.addView(clock)
        return bar
    }

    private fun taskIcon(icon: android.graphics.drawable.Drawable, onClick: () -> Unit): View =
        ImageView(this).apply {
            setImageDrawable(icon)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setOnClickListener { hideStart(); onClick() }
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(4) }
        }

    private fun buildStartMenu(): LinearLayout {
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.glass(this@MainActivity, 0xF0141C2E.toInt(), 20)
            setPadding(dp(14), dp(14), dp(14), dp(10))
            isClickable = true
        }
        menu.addView(Ui.text(this, "Start", 18f, Ui.TEXT, true).apply { setPadding(dp(4), 0, 0, dp(10)) })

        search = EditText(this).apply {
            hint = "Search apps"
            setHintTextColor(Ui.SUBTEXT)
            setTextColor(Ui.TEXT)
            textSize = 15f
            setSingleLine(true)
            background = Ui.glass(this@MainActivity, 0x22FFFFFF, 12)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) = fillStart()
            })
        }
        menu.addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        startAdapter = TileAdapter(this, 44)
        menu.addView(GridView(this).apply {
            numColumns = GridView.AUTO_FIT
            columnWidth = dp(76)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            verticalSpacing = dp(6)
            setPadding(0, dp(10), 0, dp(6))
            clipToPadding = false
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            adapter = startAdapter
            setOnItemClickListener { _, _, pos, _ -> hideStart(); startAdapter.items[pos].action() }
            setOnItemLongClickListener { _, _, pos, _ -> startAdapter.items[pos].longAction?.invoke(); true }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(8), 0, 0)
        }
        bottom.addView(Ui.text(this, "Q-OS 0.1 · Q-CUBIC", 12f, Ui.SUBTEXT), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bottom.addView(Ui.text(this, "Settings", 14f, Ui.ACCENT, true).apply {
            setPadding(dp(12), dp(6), dp(4), dp(6))
            setOnClickListener { hideStart(); open(SettingsActivity::class.java) }
        })
        menu.addView(bottom)
        return menu
    }

    private fun builtInTiles(): List<Tile> = listOf(
        Tile("Files", LetterIcon("F", 0xFFF2B233.toInt()), { open(FilesActivity::class.java) }),
        Tile("Engines", LetterIcon("E", 0xFF7B5CFA.toInt()), { open(EnginesActivity::class.java) }),
        Tile("Windows", LetterIcon("W", Engines.WINDOWS.color), { openEngine(Engines.WINDOWS) }),
        Tile("Linux", LetterIcon("L", Engines.LINUX.color), { openEngine(Engines.LINUX) }),
        Tile("Settings", LetterIcon("S", 0xFF4A5568.toInt()), { open(SettingsActivity::class.java) })
    )

    private fun appTile(app: AppEntry) = Tile(app.label, app.icon, { AppRepo.launch(this, app) }, { appOptions(app) })

    private fun refresh() {
        apps = AppRepo.load(this)
        val pinned = Prefs.pinned(this)
        val pinnedApps = apps.filter { it.key in pinned }
        desktopAdapter.items = builtInTiles() + pinnedApps.map { appTile(it) }

        taskbarApps.removeAllViews()
        val maxTaskbar = if (resources.displayMetrics.widthPixels > dp(600)) 8 else 3
        pinnedApps.take(maxTaskbar).forEach { app ->
            taskbarApps.addView(taskIcon(app.icon) { AppRepo.launch(this, app) })
        }
        fillStart()
        updateStatus()
    }

    private fun fillStart() {
        val q = search.text.toString().trim().lowercase()
        startAdapter.items = apps.filter { q.isEmpty() || it.label.lowercase().contains(q) }.map { appTile(it) }
    }

    private fun appOptions(app: AppEntry) {
        val pinned = app.key in Prefs.pinned(this)
        val options = arrayOf(if (pinned) "Unpin from desktop" else "Pin to desktop", "App info", "Uninstall")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { Prefs.togglePin(this, app.key); refresh() }
                    1 -> startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}")))
                    2 -> startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}")))
                }
            }
            .show()
    }

    private fun openEngine(e: Engine) {
        if (!Engines.open(this, e)) {
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("${e.name} not installed")
                .setMessage(e.description + "\n\nOpen the Engines screen to set it up.")
                .setPositiveButton("Set up") { _, _ -> open(EnginesActivity::class.java) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateStatus() {
        val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (b != null) {
            val level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level >= 0) battery.text = "${level * 100 / scale}%"
        }
        if (Prefs.use24h(this)) {
            clock.format12Hour = "HH:mm"; clock.format24Hour = "HH:mm"
        } else {
            clock.format12Hour = "h:mm a"; clock.format24Hour = "h:mm a"
        }
    }

    private fun toggleStart() = if (startMenu.visibility == View.VISIBLE) hideStart() else showStart()

    private fun showStart() {
        search.setText("")
        startMenu.visibility = View.VISIBLE
        if (!Prefs.lite(this)) {
            startMenu.alpha = 0f
            startMenu.translationY = dp(24).toFloat()
            startMenu.animate().alpha(1f).translationY(0f).setDuration(160).start()
        }
    }

    private fun hideStart() {
        startMenu.visibility = View.GONE
    }

    private fun open(cls: Class<*>) = startActivity(Intent(this, cls))

    override fun onResume() {
        super.onResume()
        root.background = Ui.wallpaper(Prefs.lite(this))
        refresh()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        hideStart() // Home button pressed while already on the desktop
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // A home screen never closes on Back; it only closes the Start menu.
        if (startMenu.visibility == View.VISIBLE) hideStart()
    }
}
