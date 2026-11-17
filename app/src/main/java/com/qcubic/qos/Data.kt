package com.qcubic.qos

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.Toast

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("qos", Context.MODE_PRIVATE)

    fun pinned(c: Context): Set<String> = sp(c).getStringSet("pinned", emptySet())?.toSet() ?: emptySet()

    fun togglePin(c: Context, key: String) {
        val s = pinned(c).toMutableSet()
        if (!s.add(key)) s.remove(key)
        sp(c).edit().putStringSet("pinned", s).apply()
    }

    fun lite(c: Context): Boolean = sp(c).getBoolean("lite", false)
    fun setLite(c: Context, v: Boolean) = sp(c).edit().putBoolean("lite", v).apply()

    fun use24h(c: Context): Boolean = sp(c).getBoolean("24h", false)
    fun set24h(c: Context, v: Boolean) = sp(c).edit().putBoolean("24h", v).apply()
}

data class AppEntry(val label: String, val pkg: String, val cls: String, val icon: Drawable) {
    val key: String get() = "$pkg/$cls"
}

object AppRepo {
    fun load(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map {
                AppEntry(
                    it.loadLabel(pm).toString(),
                    it.activityInfo.packageName,
                    it.activityInfo.name,
                    it.loadIcon(pm)
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launch(ctx: Context, app: AppEntry) {
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(ComponentName(app.pkg, app.cls))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            )
        } catch (e: Exception) {
            Toast.makeText(ctx, "Could not open ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Compatibility engines Q-OS hands files to. v0.1 uses existing engine apps; later versions embed them. */
data class Engine(
    val name: String,
    val description: String,
    val packages: List<String>,
    val downloadUrl: String,
    val letter: String,
    val color: Int
)

object Engines {
    val WINDOWS = Engine(
        "Windows engine",
        "Runs Windows .exe and .msi apps using Wine + Box64. Powered by Winlator.",
        listOf("com.winlator", "com.winlator.cmod"),
        "https://github.com/brunodev85/winlator/releases",
        "W", 0xFF2D7FF9.toInt()
    )
    val LINUX = Engine(
        "Linux engine",
        "Runs Linux tools and a full Ubuntu system. Powered by Termux + proot-distro.",
        listOf("com.termux"),
        "https://github.com/termux/termux-app/releases",
        "L", 0xFFE8772E.toInt()
    )
    val ALL = listOf(WINDOWS, LINUX)

    const val LINUX_SETUP =
        "termux-setup-storage && pkg update -y && pkg install -y proot-distro python && proot-distro install ubuntu"

    fun installedPackage(ctx: Context, e: Engine): String? = e.packages.firstOrNull {
        try {
            ctx.packageManager.getPackageInfo(it, 0); true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun open(ctx: Context, e: Engine): Boolean {
        val pkg = installedPackage(ctx, e) ?: return false
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
