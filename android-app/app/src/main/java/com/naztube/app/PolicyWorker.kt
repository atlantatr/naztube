package com.naztube.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PolicyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try { val settings = AppSettings(applicationContext); if (settings.accessToken.isBlank()) Result.success() else { Api.refreshPolicy(settings); Result.success() } } catch (_: Exception) { Result.retry() }
}
