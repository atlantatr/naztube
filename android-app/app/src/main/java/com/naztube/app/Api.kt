package com.naztube.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID

data class Video(val id: String, val title: String, val description: String, val category: String, val streamUrl: String)
data class Policy(val enabled: Boolean = true, val playbackEnabled: Boolean = true, val forceLock: Boolean = false, val maxDailyMinutes: Int = 0, val message: String = "", val blockedVideoIds: Set<String> = emptySet())

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("naz_tube", Context.MODE_PRIVATE)
    var baseUrl: String get() = prefs.getString("base_url", "")!!; set(value) = prefs.edit().putString("base_url", value.trimEnd('/')).apply()
    var accessToken: String get() = prefs.getString("access_token", "")!!; set(value) = prefs.edit().putString("access_token", value).apply()
    val installationId: String get() { val existing = prefs.getString("installation_id", null); return existing ?: UUID.randomUUID().toString().also { prefs.edit().putString("installation_id", it).apply() } }
    fun savePolicy(policy: Policy) = prefs.edit().putString("policy", JSONObject().apply { put("enabled", policy.enabled); put("playbackEnabled", policy.playbackEnabled); put("forceLock", policy.forceLock); put("maxDailyMinutes", policy.maxDailyMinutes); put("message", policy.message); put("blockedVideoIds", JSONArray(policy.blockedVideoIds.toList())) }.toString()).apply()
    fun policy(): Policy { val json = JSONObject(prefs.getString("policy", "{}")); return Policy(json.optBoolean("enabled", true), json.optBoolean("playbackEnabled", true), json.optBoolean("forceLock", false), json.optInt("maxDailyMinutes", 0), json.optString("message"), json.optJSONArray("blockedVideoIds")?.let { values -> (0 until values.length()).map { values.getString(it) }.toSet() } ?: emptySet()) }
    fun canStartPlayback(): Boolean { resetUsageIfNeeded(); val limit = policy().maxDailyMinutes; return limit == 0 || prefs.getLong("used_seconds", 0) < limit * 60L }
    fun recordPlayback(seconds: Long) { resetUsageIfNeeded(); prefs.edit().putLong("used_seconds", prefs.getLong("used_seconds", 0) + seconds.coerceAtLeast(0)).apply() }
    fun setExitPin(pin: String) { if (pin.length >= 4 && pin.all(Char::isDigit)) prefs.edit().putString("exit_pin_hash", sha256(pin)).apply() }
    fun hasExitPin() = prefs.contains("exit_pin_hash")
    fun matchesExitPin(pin: String) = sha256(pin) == prefs.getString("exit_pin_hash", "")
    private fun resetUsageIfNeeded() { val today = LocalDate.now().toString(); if (prefs.getString("usage_date", "") != today) prefs.edit().putString("usage_date", today).putLong("used_seconds", 0).apply() }
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}

object Api {
    fun register(settings: AppSettings, enrollmentCode: String) {
        val body = JSONObject().put("installationId", settings.installationId).put("name", "Android ${android.os.Build.MODEL}").put("enrollmentCode", enrollmentCode)
        val response = request(settings.baseUrl + "/api/devices/register", "POST", null, body.toString())
        settings.accessToken = response.getString("accessToken")
        settings.savePolicy(parsePolicy(response.getJSONObject("policy")))
    }
    fun refreshPolicy(settings: AppSettings): Policy { val policy = parsePolicy(request(settings.baseUrl + "/api/device/config", "GET", settings.accessToken).getJSONObject("policy")); settings.savePolicy(policy); return policy }
    fun catalog(settings: AppSettings): List<Video> { val videos = request(settings.baseUrl + "/api/catalog", "GET", settings.accessToken).getJSONArray("videos"); return (0 until videos.length()).map { index -> videos.getJSONObject(index).let { Video(it.getString("id"), it.getString("title"), it.optString("description"), it.optString("category", "Videolar"), it.getString("streamUrl")) } } }
    private fun parsePolicy(json: JSONObject) = Policy(json.optBoolean("enabled", true), json.optBoolean("playbackEnabled", true), json.optBoolean("forceLock", false), json.optInt("maxDailyMinutes", 0), json.optString("message"), json.optJSONArray("blockedVideoIds")?.let { values -> (0 until values.length()).map { values.getString(it) }.toSet() } ?: emptySet())
    private fun request(address: String, method: String, token: String? = null, body: String? = null): JSONObject { val connection = URL(address).openConnection() as HttpURLConnection; connection.requestMethod = method; connection.connectTimeout = 12_000; connection.readTimeout = 20_000; if (token != null) connection.setRequestProperty("Authorization", "Bearer $token"); if (body != null) { connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/json"); connection.outputStream.use { it.write(body.toByteArray()) } }; val status = connection.responseCode; val stream = if (status in 200..299) connection.inputStream else connection.errorStream; val text = stream.bufferedReader().use { it.readText() }; if (status !in 200..299) throw IllegalStateException("Sunucu hatası: $status"); return JSONObject(text) }
}
