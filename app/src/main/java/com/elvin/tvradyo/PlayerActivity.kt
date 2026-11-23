package com.elvin.tvradyo

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        val root = android.widget.FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()) }
        val view = PlayerView(this).apply { useController = true; controllerShowTimeoutMs = 4000 }
        root.addView(view, android.widget.FrameLayout.LayoutParams(-1,-1))
        val title = TextView(this).apply { text = intent.getStringExtra("name") ?: "ELVİN TV RADYO"; setTextColor(-1); textSize=16f; setPadding(20,18,20,18) }
        root.addView(title)
        setContentView(root)
        val url = intent.getStringExtra("url") ?: run { finish(); return }
        player = ExoPlayer.Builder(this).build().also { exo ->
            view.player = exo
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.playWhenReady = true
        }
    }
    override fun onStop() { super.onStop(); player?.pause() }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}
