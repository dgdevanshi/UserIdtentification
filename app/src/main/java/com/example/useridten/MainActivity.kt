package com.example.useridten

import android.annotation.SuppressLint
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.util.ArrayMap
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var appsTime: TextView
    private var mUsageStatsManager: UsageStatsManager? = null

    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    private val ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"

    private lateinit var interceptedNotificationImageView: ImageView
    private var imageChangeBroadcastReceiver: ImageChangeBroadcastReceiver? = null
    private var enableNotificationListenerAlertDialog: AlertDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mUsageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager?

        appsTime = findViewById(R.id.appsTime)
        appsTime.movementMethod = ScrollingMovementMethod()
        getUsageStats()
        requestPermissions()

        interceptedNotificationImageView = findViewById(R.id.intercepted_notification_logo)
        if(!isNotificationServiceEnabled()){
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog()
            enableNotificationListenerAlertDialog!!.show()
        }

        imageChangeBroadcastReceiver = ImageChangeBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction("com.example.useridten")
        registerReceiver(imageChangeBroadcastReceiver, intentFilter)
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
            val timesUsed = pkgStats.firstTimeStamp - pkgStats.lastTimeUsed
            appsMap[name] = mutableListOf(lastTimeUsed,usageTime, timesUsed)
        }

        val sortedMap: MutableMap<String, MutableList<Long>> = TreeMap(appsMap)
        sortedMap.forEach { entry ->
            appsTime.append(entry.key + " : " + getLongtoDate(entry.value[0]) + " , " + entry.value[1] + " , " + milliToMins(entry.value[2]) + "\n")
        }
    }

    private fun milliToMins(time: Long) : String {
        val mins = TimeUnit.MILLISECONDS.toMinutes(time)
        val secs = (TimeUnit.MILLISECONDS.toSeconds(time) % 60)
        return "$mins:$secs"
    }


    @SuppressLint("SimpleDateFormat")
    private fun getLongtoDate(time: Long) : String {
        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
        return simpleDateFormat.format(time).toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(imageChangeBroadcastReceiver)
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS)
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":".toRegex()).toTypedArray()
            for (element in names) {
                val cn = ComponentName.unflattenFromString(element)
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun changeInterceptedNotificationImage(notificationCode: Int) {
        when (notificationCode) {
            NotificationService.InterceptedNotificationCode.FACEBOOK_CODE -> interceptedNotificationImageView.setImageResource(
                R.drawable.facebook_logo
            )
            NotificationService.InterceptedNotificationCode.INSTAGRAM_CODE -> interceptedNotificationImageView.setImageResource(
                R.drawable.instagram_logo
            )
            NotificationService.InterceptedNotificationCode.WHATSAPP_CODE -> interceptedNotificationImageView.setImageResource(
                R.drawable.whatsapp_logo
            )
            NotificationService.InterceptedNotificationCode.OTHER_NOTIFICATION_CODE -> interceptedNotificationImageView.setImageResource(
                R.drawable.notification_logo
            )
        }
    }


    inner class ImageChangeBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val receivedNotificationCode = intent.getIntExtra("Notification Code", -1)
            this@MainActivity.changeInterceptedNotificationImage(receivedNotificationCode)
        }
    }

    private fun buildNotificationServiceAlertDialog() : AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(R.string.notification_listener_service)
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation)
        alertDialogBuilder.setPositiveButton(R.string.yes) {
                _, _ -> startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        alertDialogBuilder.setNegativeButton(R.string.no) {
                _, _ -> startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        return alertDialogBuilder.create()
    }
}