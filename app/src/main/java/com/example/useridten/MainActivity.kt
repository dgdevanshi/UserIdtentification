package com.example.useridten


import android.annotation.SuppressLint
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.util.ArrayMap
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var appsTime: TextView
    private var mUsageStatsManager: UsageStatsManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mUsageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager?

        appsTime = findViewById(R.id.appsTime)
        appsTime.movementMethod = ScrollingMovementMethod()
        getUsageStats()
        requestPermissions()
    }

    private fun requestPermissions() {
        val stats = mUsageStatsManager!!.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, 0, System.currentTimeMillis())
        if (stats.isEmpty()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }


    private fun getUsageStats() {
        val appsMap : MutableMap<String, MutableList<Long>> = mutableMapOf()
        val mAppLabelMap = ArrayMap<String, String>()
        val mPackageStats: ArrayList<UsageStats> = ArrayList()

        val cal: Calendar = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -5)
        val stats = mUsageStatsManager!!.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, cal.timeInMillis, System.currentTimeMillis())


        val map: ArrayMap<String, UsageStats> = ArrayMap()
        for (pkgStats in stats) {
            val appInfo: ApplicationInfo = packageManager.getApplicationInfo(pkgStats.packageName, 0)
            val label = appInfo.loadLabel(packageManager).toString()
            mAppLabelMap[pkgStats.packageName] = label
            val existingStats = map[pkgStats.packageName]
            if (existingStats == null) {
                map[pkgStats.packageName] = pkgStats
            } else {
                existingStats.add(pkgStats)
            }
        }
        mPackageStats.addAll(map.values)

        for (pkgStats in mPackageStats) {
            val name = mAppLabelMap[pkgStats.packageName].toString()
            val lastTimeUsed = pkgStats.lastTimeUsed
            val usageTime = pkgStats.totalTimeInForeground/1000
            appsMap[name] = mutableListOf(lastTimeUsed,usageTime)
        }

        val sortedMap: MutableMap<String, MutableList<Long>> = TreeMap(appsMap)
        sortedMap.forEach { entry ->
            appsTime.append(entry.key + " : " + getLongtoDate(entry.value[0]) + " , " + entry.value[1] + "\n")
        }


    }

    @SuppressLint("SimpleDateFormat")
    private fun getLongtoDate(time: Long) : String {
        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
        return simpleDateFormat.format(time).toString()
    }
}