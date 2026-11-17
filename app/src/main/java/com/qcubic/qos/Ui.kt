package com.qcubic.qos

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

object Ui {
    const val TEXT = 0xFFFFFFFF.toInt()
    const val SUBTEXT = 0xFFA9B4C8.toInt()
    const val ACCENT = 0xFF35C9E0.toInt()
    const val SCREEN_BG = 0xFF0E1525.toInt()
    const val CARD = 0x33FFFFFF

    /** "Liquid glass" style rounded panel. */
    fun glass(ctx: Context, color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = ctx.dp(radiusDp).toFloat()
            setStroke(ctx.dp(1), 0x33FFFFFF)
        }

    fun wallpaper(lite: Boolean): Drawable =
        if (lite) ColorDrawable(0xFF0B1A3A.toInt())
        else GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF0B1A3A.toInt(), 0xFF0F4C5C.toInt(), 0xFF2A1B5E.toInt(), 0xFF0B1A3A.toInt())
        )

    fun text(ctx: Context, s: String, sizeSp: Float = 15f, color: Int = TEXT, bold: Boolean = false): TextView =
        TextView(ctx).apply {
            text = s
            textSize = sizeSp
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    fun button(ctx: Context, s: String, onClick: () -> Unit): TextView =
        text(ctx, s, 15f, TEXT, true).apply {
            gravity = Gravity.CENTER
            background = glass(ctx, 0x5535C9E0, 12)
            setPadding(ctx.dp(16), ctx.dp(10), ctx.dp(16), ctx.dp(10))
            setOnClickListener { onClick() }
        }

    /** Standard screen: dark background, top bar with back arrow and title. Returns the body container. */
    fun screen(act: Activity, title: String): LinearLayout {
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(SCREEN_BG)
        }
        val bar = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(act.dp(8), act.dp(32), act.dp(16), act.dp(8))
        }
        bar.addView(text(act, "‹", 30f, TEXT, true).apply {
            setPadding(act.dp(12), 0, act.dp(16), act.dp(4))
            setOnClickListener { act.onBackPressed() }
        })
        bar.addView(text(act, title, 20f, TEXT, true))
        root.addView(bar)
        act.setContentView(root)
        return root
    }
}

/** Simple rounded-square icon with a letter, used for Q-OS built-in apps and file types. */
class LetterIcon(private val letter: String, color: Int) : Drawable() {
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    private val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    override fun draw(canvas: Canvas) {
        val b = RectF(bounds)
        val r = b.width() * 0.24f
        canvas.drawRoundRect(b, r, r, bg)
        fg.textSize = b.height() * 0.46f
        val y = b.centerY() - (fg.descent() + fg.ascent()) / 2f
        canvas.drawText(letter, b.centerX(), y, fg)
    }

    override fun setAlpha(alpha: Int) { bg.alpha = alpha; fg.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { bg.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth(): Int = 144
    override fun getIntrinsicHeight(): Int = 144
}

data class Tile(
    val label: String,
    val icon: Drawable,
    val action: () -> Unit,
    val longAction: (() -> Unit)? = null
)

/** Grid adapter that shows an icon with a label underneath. */
class TileAdapter(private val ctx: Context, private val iconDp: Int = 52) : BaseAdapter() {
    var items: List<Tile> = emptyList()
        set(value) { field = value; notifyDataSetChanged() }

    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val box = (convertView as? LinearLayout) ?: LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(ctx.dp(4), ctx.dp(6), ctx.dp(4), ctx.dp(6))
            addView(ImageView(ctx), LinearLayout.LayoutParams(ctx.dp(iconDp), ctx.dp(iconDp)))
            addView(TextView(ctx).apply {
                textSize = 12f
                setTextColor(Ui.TEXT)
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setShadowLayer(4f, 0f, 1f, 0x99000000.toInt())
                setPadding(0, ctx.dp(4), 0, 0)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val t = items[position]
        (box.getChildAt(0) as ImageView).setImageDrawable(t.icon)
        (box.getChildAt(1) as TextView).text = t.label
        return box
    }
}
