package com.example.gifwallpaper

import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File
import kotlin.math.roundToInt

class GifWallpaperService : WallpaperService() {

    companion object {
        const val GIF_NAME = "wallpaper.gif"
    }

    override fun onCreateEngine(): Engine = GifEngine()

    inner class GifEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var gif: AnimatedImageDrawable? = null
        private var transform = GifTransform()

        // The drawable calls back to schedule frames; we redraw on each one.
        private val callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) = draw()
            override fun scheduleDrawable(who: Drawable, what: Runnable, whenMs: Long) {
                handler.postAtTime(what, whenMs)
            }
            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                handler.removeCallbacks(what)
            }
        }

        // When the user saves a new position in the app, apply it right away.
        private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            transform = GifTransform.load(this@GifWallpaperService)
            draw()
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            getSharedPreferences(GifTransform.PREFS, MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(prefsListener)
        }

        override fun onDestroy() {
            getSharedPreferences(GifTransform.PREFS, MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(prefsListener)
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                // ponytail: reloads the GIF every time it becomes visible; cache it if this gets slow
                loadGif()
                transform = GifTransform.load(this@GifWallpaperService)
                gif?.start()
                draw()
            } else {
                gif?.stop()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            gif?.stop()
            handler.removeCallbacksAndMessages(null)
            super.onSurfaceDestroyed(holder)
        }

        private fun loadGif() {
            val file = File(filesDir, GIF_NAME)
            if (!file.exists()) return
            val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(file))
            gif = (drawable as? AnimatedImageDrawable)?.also { it.callback = callback }
        }

        private fun draw() {
            val holder = surfaceHolder
            val canvas: Canvas = holder.lockCanvas() ?: return
            try {
                canvas.drawColor(Color.BLACK)
                gif?.let { g ->
                    val r = transform.layout(canvas.width, canvas.height, g.intrinsicWidth, g.intrinsicHeight)
                    g.setBounds(r.left.roundToInt(), r.top.roundToInt(), r.right.roundToInt(), r.bottom.roundToInt())
                    g.draw(canvas)
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
