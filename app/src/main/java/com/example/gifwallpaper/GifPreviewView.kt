package com.example.gifwallpaper

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.roundToInt

// Shows the GIF full screen. In edit mode the user can drag it and pinch to zoom,
// always staying inside the screen.
class GifPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var gif: Drawable? = null
    var editable = false
    var transform: GifTransform = GifTransform()
        set(value) {
            field = value
            invalidate()
        }

    private var lastX = 0f
    private var lastY = 0f

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val zoom = (transform.zoom * detector.scaleFactor).coerceIn(1f, GifTransform.MAX_ZOOM)
                transform = transform.copy(zoom = zoom)
                return true
            }
        }
    )

    fun setGif(drawable: Drawable?) {
        gif?.callback = null
        gif = drawable
        drawable?.callback = this
        (drawable as? AnimatedImageDrawable)?.start()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val d = gif ?: return
        val r = transform.layout(width, height, d.intrinsicWidth, d.intrinsicHeight)
        d.setBounds(r.left.roundToInt(), r.top.roundToInt(), r.right.roundToInt(), r.bottom.roundToInt())
        d.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!editable || gif == null) return super.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) pan(event.x - lastX, event.y - lastY)
                lastX = event.x
                lastY = event.y
            }
        }
        return true
    }

    // Moves the GIF by dx/dy pixels, clamped so it never leaves the screen.
    private fun pan(dx: Float, dy: Float) {
        val d = gif ?: return
        val r = transform.layout(width, height, d.intrinsicWidth, d.intrinsicHeight)
        val maxX = (r.width() - width) / 2f
        val maxY = (r.height() - height) / 2f
        val ox = if (maxX > 0) ((transform.offsetX * maxX + dx) / maxX).coerceIn(-1f, 1f) else 0f
        val oy = if (maxY > 0) ((transform.offsetY * maxY + dy) / maxY).coerceIn(-1f, 1f) else 0f
        transform = transform.copy(offsetX = ox, offsetY = oy)
    }
}
