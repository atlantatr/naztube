package com.naztube.app

import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private var startedAt = 0L
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); title = intent.getStringExtra("title") ?: "Naz Tube"; val settings = AppSettings(this); val factory = DefaultHttpDataSource.Factory().setDefaultRequestProperties(mapOf("Authorization" to "Bearer ${settings.accessToken}")); player = ExoPlayer.Builder(this).setMediaSourceFactory(DefaultMediaSourceFactory(factory)).build().also { it.setMediaItem(MediaItem.fromUri(intent.getStringExtra("url")!!)); it.prepare(); it.playWhenReady = true }; startedAt = SystemClock.elapsedRealtime(); setContentView(PlayerView(this).apply { this.player = this@PlayerActivity.player; useController = true }) }
    override fun onStop() { AppSettings(this).recordPlayback((SystemClock.elapsedRealtime() - startedAt) / 1000); player?.release(); player = null; super.onStop() }
}
