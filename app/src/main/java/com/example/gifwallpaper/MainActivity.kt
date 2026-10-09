package com.example.gifwallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class MainActivity : ComponentActivity() {

    private val gifFile by lazy { File(filesDir, GifWallpaperService.GIF_NAME) }
    private val previewImage by lazy { findViewById<ImageView>(R.id.previewImage) }

    private val pickGif = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        // ponytail: copy once into app storage so the service never needs the picker URI again
        contentResolver.openInputStream(uri)?.use { input ->
            gifFile.outputStream().use { input.copyTo(it) }
        }
        showPreview()
        Toast.makeText(this, R.string.gif_saved, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        showPreview()

        // Keep the buttons above the navigation bar
        val padding = resources.getDimensionPixelSize(R.dimen.screen_padding)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.buttonBar)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(padding, padding, padding, padding + bars.bottom)
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
    }

    // ponytail: decodes on the main thread; move to a background thread if large GIFs stutter
    private fun showPreview() {
        if (!gifFile.exists()) return
        val gif = ImageDecoder.decodeDrawable(ImageDecoder.createSource(gifFile))
        previewImage.setImageDrawable(gif)
        (gif as? AnimatedImageDrawable)?.start()
    }
}
