package com.example.gifwallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.ImageDecoder
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class MainActivity : ComponentActivity() {

    private val gifFile by lazy { File(filesDir, GifWallpaperService.GIF_NAME) }
    private val previewView by lazy { findViewById<GifPreviewView>(R.id.previewView) }
    private val btnEdit by lazy { findViewById<ImageButton>(R.id.btnEdit) }
    private val normalBar by lazy { findViewById<LinearLayout>(R.id.normalBar) }
    private val editBar by lazy { findViewById<LinearLayout>(R.id.editBar) }

    private val pickGif = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        // ponytail: copy once into app storage so the service never needs the picker URI again
        contentResolver.openInputStream(uri)?.use { input ->
            gifFile.outputStream().use { input.copyTo(it) }
        }
        GifTransform.save(this, GifTransform()) // a new GIF starts centered and full screen
        showPreview()
        Toast.makeText(this, R.string.gif_saved, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        showPreview()

        // Keep the buttons above the navigation bar and the edit button below the status bar
        val padding = resources.getDimensionPixelSize(R.dimen.screen_padding)
        val editMargin = resources.getDimensionPixelSize(R.dimen.edit_margin)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.buttonBar)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(padding, padding, padding, padding + bars.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(btnEdit) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            (view.layoutParams as ViewGroup.MarginLayoutParams).topMargin = bars.top + editMargin
            insets
        }

        findViewById<Button>(R.id.btnPick).setOnClickListener {
            pickGif.launch(arrayOf("image/gif"))
        }

        findViewById<Button>(R.id.btnSet).setOnClickListener {
            if (!gifFile.exists()) {
                Toast.makeText(this, R.string.no_gif_yet, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, GifWallpaperService::class.java)
            )
            startActivity(intent)
        }

        btnEdit.setOnClickListener {
            if (!gifFile.exists()) {
                Toast.makeText(this, R.string.no_gif_yet, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setEditMode(true)
        }

        // Save the position the user chose, then leave edit mode
        findViewById<Button>(R.id.btnApply).setOnClickListener {
            GifTransform.save(this, previewView.transform)
            setEditMode(false)
        }

        // Throw away the changes and go back to the saved position
        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            previewView.transform = GifTransform.load(this)
            setEditMode(false)
        }
    }

    private fun setEditMode(on: Boolean) {
        previewView.editable = on
        normalBar.visibility = if (on) View.GONE else View.VISIBLE
        editBar.visibility = if (on) View.VISIBLE else View.GONE
        btnEdit.visibility = if (on) View.GONE else View.VISIBLE
    }

    // ponytail: decodes on the main thread; move to a background thread if large GIFs stutter
    private fun showPreview() {
        if (!gifFile.exists()) return
        val gif = ImageDecoder.decodeDrawable(ImageDecoder.createSource(gifFile))
        previewView.setGif(gif)
        previewView.transform = GifTransform.load(this)
    }
}
