package com.example.unlocklogger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupRecyclerView()
        setupButtons()
        startUnlockLoggerService()
        refreshUI()
        
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
    
    companion object {
        private const val TAG = "MainActivity"
    }
}