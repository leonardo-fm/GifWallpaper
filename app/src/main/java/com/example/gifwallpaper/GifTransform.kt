package com.example.gifwallpaper

import android.content.Context
import android.graphics.RectF

// Where the GIF sits on screen. Shared by the preview and the live wallpaper.
// zoom: 1 = fills the screen (cover), up to MAX_ZOOM.
// offsetX / offsetY: -1..1, the position inside the overflow (0 = centered).
// Keeping these in range means the GIF always covers the screen.
data class GifTransform(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {

    // Returns where the GIF is drawn, in screen pixels.
    fun layout(viewW: Int, viewH: Int, gifW: Int, gifH: Int): RectF {
        val base = maxOf(viewW.toFloat() / gifW, viewH.toFloat() / gifH)
        val w = gifW * base * zoom
        val h = gifH * base * zoom
        val maxX = (w - viewW) / 2f
        val maxY = (h - viewH) / 2f
        val left = (viewW - w) / 2f + offsetX * maxX
        val top = (viewH - h) / 2f + offsetY * maxY
        return RectF(left, top, left + w, top + h)
    }

    companion object {
        const val MAX_ZOOM = 4f
        const val PREFS = "gif_wallpaper"

        fun load(context: Context): GifTransform {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return GifTransform(
                zoom = p.getFloat("zoom", 1f),
                offsetX = p.getFloat("offsetX", 0f),
                offsetY = p.getFloat("offsetY", 0f),
            )
        }

        fun save(context: Context, t: GifTransform) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putFloat("zoom", t.zoom)
                .putFloat("offsetX", t.offsetX)
                .putFloat("offsetY", t.offsetY)
                .apply()
        }
    }
}
