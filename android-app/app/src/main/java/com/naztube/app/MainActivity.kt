package com.naztube.app

import android.view.ViewGroup
import android.widget.Toast
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var settings: AppSettings
    private lateinit var adapter: VideoAdapter
    private lateinit var message: TextView
    private lateinit var loading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings(this)
        createScreen()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { confirmExit() } })
        if (settings.accessToken.isBlank()) showConnectionDialog() else refresh()
        schedulePolicySync()
    }
    private fun createScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(16, 21, 22)); setPadding(24, 24, 24, 20) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply { text = "Naz Tube"; textSize = 30f; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
        header.addView(Button(this).apply { text = "Ayarlar"; setOnClickListener { showConnectionDialog() } })
        message = TextView(this).apply { setTextColor(Color.rgb(180, 205, 202)); setPadding(0, 12, 0, 12) }
        loading = ProgressBar(this).apply { visibility = View.GONE }
        val recycler = RecyclerView(this).apply { layoutManager = GridLayoutManager(this@MainActivity, resources.configuration.screenWidthDp.let { if (it >= 700) 3 else 2 }); adapter = VideoAdapter(::openVideo).also { this@MainActivity.adapter = it } }
        root.addView(header); root.addView(message); root.addView(loading); root.addView(recycler, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)); setContentView(root)
    }
    private fun showConnectionDialog() {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 18, 48, 0) }
        val server = EditText(this).apply { hint = "https://sunucunuz"; setText(settings.baseUrl); inputType = InputType.TYPE_TEXT_VARIATION_URI }
        val code = EditText(this).apply { hint = "Cihaz kayıt kodu"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val pin = EditText(this).apply { hint = "Çıkış PIN'i (en az 4 rakam)"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        form.addView(server); form.addView(code); form.addView(pin)
        AlertDialog.Builder(this).setTitle("Naz Tube bağlantısı").setView(form).setNegativeButton("Vazgeç", null).setPositiveButton("Bağlan", null).create().also { dialog -> dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { if (pin.text.isNotBlank() && (pin.text.length < 4 || !pin.text.all(Char::isDigit))) { pin.error = "En az 4 rakam girin"; return@setOnClickListener }; if (pin.text.isNotBlank()) settings.setExitPin(pin.text.toString()); settings.baseUrl = server.text.toString(); executor.execute { try { Api.register(settings, code.text.toString()); runOnUiThread { dialog.dismiss(); refresh() } } catch (_: Exception) { runOnUiThread { Toast.makeText(this, "Bağlantı veya kayıt kodu geçersiz.", Toast.LENGTH_LONG).show() } } } } }.show() }
    }
    private fun refresh() {
        loading.visibility = View.VISIBLE
        executor.execute { try { val policy = Api.refreshPolicy(settings); val videos = Api.catalog(settings); runOnUiThread { loading.visibility = View.GONE; applyPolicy(policy); adapter.submit(videos) } } catch (_: Exception) { runOnUiThread { loading.visibility = View.GONE; message.text = "Sunucuya ulaşılamadı. Bağlantıyı Ayarlar’dan kontrol edin." } } }
    }
    private fun applyPolicy(policy: Policy) { message.text = policy.message; if (!policy.enabled || policy.forceLock) AlertDialog.Builder(this).setTitle("Naz Tube kilitli").setMessage(policy.message.ifBlank { "Bu cihaz için erişim uzaktan durduruldu." }).setCancelable(false).setPositiveButton("Tamam", null).show() }
    private fun openVideo(video: Video) { val policy = settings.policy(); if (!policy.enabled || !policy.playbackEnabled || policy.forceLock) { Toast.makeText(this, "Bu cihazda oynatma kapalı.", Toast.LENGTH_SHORT).show(); return }; if (!settings.canStartPlayback()) { Toast.makeText(this, "Günlük izleme sınırına ulaşıldı.", Toast.LENGTH_LONG).show(); return }; startActivity(Intent(this, PlayerActivity::class.java).putExtra("title", video.title).putExtra("url", video.streamUrl)) }
    private fun confirmExit() { if (!settings.hasExitPin()) { finish(); return }; val pin = EditText(this).apply { hint = "Çıkış PIN'i"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD }; AlertDialog.Builder(this).setTitle("Naz Tube çıkışı").setMessage("Çıkmak için PIN girin.").setView(pin).setNegativeButton("Vazgeç", null).setPositiveButton("Çık", null).create().also { dialog -> dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { if (settings.matchesExitPin(pin.text.toString())) { dialog.dismiss(); finish() } else pin.error = "PIN doğru değil" } } }.show() }
    private fun schedulePolicySync() { val request = PeriodicWorkRequestBuilder<PolicyWorker>(15, TimeUnit.MINUTES).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build(); WorkManager.getInstance(this).enqueueUniquePeriodicWork("naz_tube_policy", ExistingPeriodicWorkPolicy.KEEP, request) }
}
