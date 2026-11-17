package com.example.bravetabtimer

import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

data class TrackedTab(
    val id: Long,
    val title: String,
    val url: String,
    val openedAt: Long,
    var activeStartedAt: Long?,
    var activeMillis: Long = 0L
)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("tracker", Context.MODE_PRIVATE) }
    private val tabs = mutableListOf<TrackedTab>()
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadTabs()
        buildUi()
        renderTabs()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        val title = TextView(this).apply {
            text = "Brave Tab Timer"
            textSize = 25f
            setPadding(0, 0, 0, 12)
        }
        status = TextView(this).apply {
            textSize = 14f
            text = "Manual tab tracker • Android"
            setPadding(0, 0, 0, 12)
        }
        val name = EditText(this).apply { hint = "Tab / website name (e.g. YouTube)" }
        val url = EditText(this).apply {
            hint = "Website URL (e.g. https://youtube.com)"
            inputType = 17
        }
        val add = Button(this).apply {
            text = "Add tab and start timer"
            setOnClickListener {
                val n = name.text.toString().trim()
                val u = url.text.toString().trim()
                if (n.isEmpty() || u.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Enter both name and URL", Toast.LENGTH_SHORT).show()
                } else {
                    tabs.add(TrackedTab(System.currentTimeMillis(), n, u, System.currentTimeMillis(), System.currentTimeMillis()))
                    saveTabs(); name.text.clear(); url.text.clear(); renderTabs()
                }
            }
        }
        val usage = Button(this).apply {
            text = "Enable total Brave usage measurement"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }
        val usageNow = Button(this).apply {
            text = "Check today's total Brave usage"
            setOnClickListener { showBraveUsage() }
        }
        val note = TextView(this).apply {
            text = "Note: Android does not expose Brave's private tab list to companion apps. Add tabs here manually. Active time is measured while you tap Start/Stop in this app; it is not automatic detection of the browser's foreground tab."
            textSize = 13f
            setPadding(0, 12, 0, 12)
        }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(title); root.addView(status); root.addView(name); root.addView(url)
        root.addView(add); root.addView(usage); root.addView(usageNow); root.addView(note)
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun renderTabs() {
        list.removeAllViews()
        if (tabs.isEmpty()) {
            list.addView(TextView(this).apply { text = "No tabs added yet."; textSize = 16f })
            return
        }
        tabs.forEach { tab ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 14, 12, 14)
                setBackgroundColor(0xFFEFEFEF.toInt())
            }
            val active = tab.activeStartedAt != null
            val age = formatDuration(System.currentTimeMillis() - tab.openedAt)
            val activeTotal = tab.activeMillis + (tab.activeStartedAt?.let { System.currentTimeMillis() - it } ?: 0L)
            card.addView(TextView(this).apply {
                text = tab.title; textSize = 18f
            })
            card.addView(TextView(this).apply {
                text = "${tab.url}\nOpened: ${dateFormat.format(Date(tab.openedAt))}\nTab age: $age\nTracked active time: ${formatDuration(activeTotal)}\nTimer: ${if (active) "RUNNING" else "PAUSED"}"
                textSize = 13f
            })
            val row = LinearLayout(this).apply { gravity = Gravity.END }
            row.addView(Button(this@MainActivity).apply {
                text = if (active) "Stop active timer" else "Start active timer"
                setOnClickListener {
                    if (tab.activeStartedAt != null) {
                        tab.activeMillis += System.currentTimeMillis() - tab.activeStartedAt!!
                        tab.activeStartedAt = null
                    } else tab.activeStartedAt = System.currentTimeMillis()
                    saveTabs(); renderTabs()
                }
            })
            row.addView(Button(this@MainActivity).apply {
                text = "Delete"
                setOnClickListener { tabs.remove(tab); saveTabs(); renderTabs() }
            })
            card.addView(row)
            val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = 12
            list.addView(card, lp)
        }
    }

    private fun showBraveUsage() {
        if (!hasUsagePermission()) {
            status.text = "Usage access not enabled. Enable it in Android Settings, then check again."
            return
        }
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val start = now - 24L * 60L * 60L * 1000L
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)
        val brave = stats.filter { it.packageName == "com.brave.browser" }.sumOf { it.totalTimeInForeground }
        status.text = "Brave foreground usage (last 24 hours): ${formatDuration(brave)}. This is total app usage, not per-tab time."
    }

    private fun hasUsagePermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun formatDuration(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private fun saveTabs() {
        val arr = JSONArray()
        tabs.forEach {
            arr.put(JSONObject().put("id", it.id).put("title", it.title).put("url", it.url)
                .put("openedAt", it.openedAt).put("activeStartedAt", it.activeStartedAt ?: JSONObject.NULL)
                .put("activeMillis", it.activeMillis))
        }
        prefs.edit().putString("tabs", arr.toString()).apply()
    }

    private fun loadTabs() {
        val raw = prefs.getString("tabs", "[]") ?: "[]"
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val active = if (o.isNull("activeStartedAt")) null else o.getLong("activeStartedAt")
                tabs.add(TrackedTab(o.getLong("id"), o.getString("title"), o.getString("url"),
                    o.getLong("openedAt"), active, o.optLong("activeMillis", 0L)))
            }
        } catch (_: Exception) { tabs.clear() }
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized) renderTabs()
    }
}
