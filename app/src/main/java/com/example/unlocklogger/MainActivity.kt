package com.example.unlocklogger

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    
    private lateinit var countTextView: TextView
    private lateinit var lastUpdatedText: TextView
    private lateinit var startButton: Button
    private lateinit var clearButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: UnlockEventsAdapter
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var updateDownloadId: Long = -1L

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE != intent.action) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != updateDownloadId) return

            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val apkUri = manager.getUriForDownloadedFile(id)
            if (apkUri == null) {
                Log.e(TAG, "Downloaded APK URI is null")
                return
            }

            promptInstall(apkUri)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupRecyclerView()
        setupButtons()
        startUnlockLoggerService()
        refreshUI()
        registerDownloadReceiver()
        checkForGithubUpdate()
        
        swipeRefresh.setOnRefreshListener {
            refreshUI()
        }
    }
    
    private fun initializeViews() {
        countTextView = findViewById(R.id.countTextView)
        lastUpdatedText = findViewById(R.id.lastUpdatedText)
        startButton = findViewById(R.id.startButton)
        clearButton = findViewById(R.id.clearButton)
        recyclerView = findViewById(R.id.recyclerView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
    }
    
    private fun setupRecyclerView() {
        adapter = UnlockEventsAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
    }
    
    private fun setupButtons() {
        startButton.setOnClickListener {
            Log.d(TAG, "Start button clicked")
            startUnlockLoggerService()
        }
        
        clearButton.setOnClickListener {
            Log.d(TAG, "Clear button clicked")
            clearDatabase()
        }
    }
    
    private fun startUnlockLoggerService() {
        val serviceIntent = Intent(this, UnlockLoggerService::class.java)
        startForegroundService(serviceIntent)
        Log.d(TAG, "Service started from MainActivity")
    }
    
    private fun refreshUI() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val allEvents = withContext(Dispatchers.IO) {
                    db.unlockEventDao().getAllEvents()
                }
                
                val count = allEvents.size
                countTextView.text = "Total Unlocks: $count"
                
                if (allEvents.isNotEmpty()) {
                    val lastEvent = allEvents.first()
                    lastUpdatedText.text = "Last updated: ${formatter.format(Date(lastEvent.timestamp))}"
                } else {
                    lastUpdatedText.text = "Last updated: --"
                }
                
                adapter.submitList(allEvents)
                Log.d(TAG, "UI refreshed with $count events")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing UI", e)
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun clearDatabase() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getInstance(applicationContext)
                    db.unlockEventDao().deleteAll()
                }
                refreshUI()
                Log.d(TAG, "Database cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing database", e)
            }
        }
    }

    private fun checkForGithubUpdate() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val releaseUrl = getString(R.string.github_release_api_url)
                val connection = URL(releaseUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github+json")

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = buildString {
                    var line: String?
                    while (true) {
                        line = reader.readLine() ?: break
                        append(line)
                    }
                }

                val json = JSONObject(response)
                val latestVersion = json.optString("tag_name").trim()
                val assets = json.optJSONArray("assets")
                val apkAssetName = getString(R.string.github_apk_asset_name)

                val apkUrl = findApkUrl(assets, apkAssetName)
                val currentVersion = getCurrentVersionName()

                if (apkUrl != null && isNewerVersion(latestVersion, currentVersion)) {
                    withContext(Dispatchers.Main) {
                        showUpdateDialog(latestVersion, apkUrl)
                    }
                } else {
                    Log.d(TAG, "No GitHub update. Current=$currentVersion, Latest=$latestVersion")
                }
            } catch (e: Exception) {
                Log.e(TAG, "GitHub update check failed", e)
            }
        }
    }

    private fun showUpdateDialog(latestVersion: String, apkUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("Update available")
            .setMessage("A new version ($latestVersion) is available. Download and install?")
            .setPositiveButton("Download") { _, _ ->
                downloadApk(apkUrl)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadApk(apkUrl: String) {
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Unlock Logger Update")
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType("application/vnd.android.package-archive")

        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        updateDownloadId = manager.enqueue(request)
        Log.d(TAG, "Update download enqueued: $updateDownloadId")
    }

    private fun promptInstall(apkUri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun registerDownloadReceiver() {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun getCurrentVersionName(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }

            packageInfo.versionName ?: "0.0.0"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read versionName", e)
            "0.0.0"
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = normalizeVersion(latest)
        val currentParts = normalizeVersion(current)
        val maxSize = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxSize) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l != c) return l > c
        }
        return false
    }

    private fun normalizeVersion(version: String): List<Int> {
        val cleaned = version.trim().lowercase(Locale.getDefault()).removePrefix("v")
        return cleaned
            .replace(Regex("[^0-9.]"), "")
            .split(".")
            .filter { it.isNotBlank() }
            .map { it.toIntOrNull() ?: 0 }
    }

    private fun findApkUrl(assets: org.json.JSONArray?, preferredName: String): String? {
        if (assets == null) return null
        var fallback: String? = null

        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url")
            if (name.equals(preferredName, ignoreCase = true)) {
                return url
            }
            if (name.endsWith(".apk", ignoreCase = true)) {
                fallback = url
            }
        }

        return fallback
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(downloadReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Download receiver not registered", e)
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}