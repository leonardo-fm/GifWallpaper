package com.example.gifwallpaper

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

class GifWallpaperService : WallpaperService() {

    companion object {
        const val GIF_NAME = "wallpaper.gif"
    }

    override fun onCreateEngine(): Engine = GifEngine()

    inner class GifEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var gif: AnimatedImageDrawable? = null

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

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                // ponytail: reloads the GIF every time it becomes visible; cache it if this gets slow
                loadGif()
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
                gif?.let { drawCenterCrop(canvas, it) }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        // Scales the GIF to fill the screen and crops the overflow, centered.
        private fun drawCenterCrop(canvas: Canvas, gif: AnimatedImageDrawable) {
            val scale = maxOf(
                canvas.width.toFloat() / gif.intrinsicWidth,
                canvas.height.toFloat() / gif.intrinsicHeight
            )
            val w = (gif.intrinsicWidth * scale).toInt()
            val h = (gif.intrinsicHeight * scale).toInt()
            val left = (canvas.width - w) / 2
            val top = (canvas.height - h) / 2
            gif.setBounds(left, top, left + w, top + h)
            gif.draw(canvas)
        }
    }
}
