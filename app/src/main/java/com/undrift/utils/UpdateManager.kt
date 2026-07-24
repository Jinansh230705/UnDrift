package com.undrift.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val GITHUB_API_URL = "https://api.github.com/repos/Jinansh230705/UnDrift/releases/latest"

    /**
     * Checks for updates and returns the download URL if a new version is available.
     * @return Download URL string if update available, null otherwise.
     */
    suspend fun checkForUpdates(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonStr = URL(GITHUB_API_URL).readText()
                val json = JSONObject(jsonStr)
                
                val tagName = json.getString("tag_name").replace("v", "")
                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""

                if (isNewerVersion(currentVersion, tagName)) {
                    val assets = json.getJSONArray("assets")
                    if (assets.length() > 0) {
                        val asset = assets.getJSONObject(0)
                        return@withContext asset.getString("browser_download_url")
                    }
                }
                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check for updates", e)
                return@withContext null
            }
        }
    }

    fun downloadUpdate(context: Context, url: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("UnDrift Update")
                setDescription("Downloading the latest version...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "UnDrift-update.apk")
            }
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue download", e)
        }
    }

    private fun isNewerVersion(current: String, fetched: String): Boolean {
        try {
            val parseVersion = { ver: String ->
                ver.replace("^v".toRegex(), "")
                    .split("-")[0]
                    .split(".")
                    .map { part -> part.filter { it.isDigit() }.toIntOrNull() ?: 0 }
            }
            
            val currParts = parseVersion(current)
            val fetchParts = parseVersion(fetched)
            
            val maxLength = maxOf(currParts.size, fetchParts.size)
            for (i in 0 until maxLength) {
                val c = currParts.getOrElse(i) { 0 }
                val f = fetchParts.getOrElse(i) { 0 }
                if (f > c) return true
                if (f < c) return false
            }
            return false
        } catch (e: Exception) {
            return fetched > current
        }
    }
}
