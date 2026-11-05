package com.github.ilife798

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

// 设备运行期间维持前台服务并持有 Partial WakeLock，
// 避免锁屏或切到后台后进程被厂商省电策略冻结、网络请求被中断。
// 前台服务通知直接复用运行通知（含实时动态），由 [RunNotifications] 通过 Intent 传入。
class RunForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    // WakeLock 带超时获取并由该任务定期续期，服务异常退出时系统也能自动清理。
    private val renewWakeLock =
        object : Runnable {
            override fun run() {
                acquireWakeLock()
                handler.postDelayed(this, WAKE_LOCK_RENEW_INTERVAL_MS)
            }
        }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        ensureRunChannels(this)
        val notification = intent?.runNotification()
        if (intent == null || notification == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        handler.removeCallbacks(renewWakeLock)
        renewWakeLock.run()
        ServiceCompat.startForeground(
            this,
            intent.getIntExtra(EXTRA_NOTIFICATION_ID, RUN_NOTIFICATION_ID),
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun Intent.runNotification(): Notification? = getParcelableExtra(EXTRA_NOTIFICATION)

    private fun acquireWakeLock() {
        val lock =
            wakeLock
                ?: getSystemService(PowerManager::class.java)
                    ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ilife798:run")
                    ?.apply { setReferenceCounted(false) }
                ?: return
        wakeLock = lock
        runCatching { lock.acquire(WAKE_LOCK_TIMEOUT_MS) }
    }

    override fun onDestroy() {
        handler.removeCallbacks(renewWakeLock)
        wakeLock?.let { runCatching { if (it.isHeld) it.release() } }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_NOTIFICATION_ID = "com.github.ilife798.extra.NOTIFICATION_ID"
        private const val EXTRA_NOTIFICATION = "com.github.ilife798.extra.NOTIFICATION"
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L
        private const val WAKE_LOCK_RENEW_INTERVAL_MS = 9 * 60 * 1000L

        // 运行中启动/更新前台服务；无运行任务时停止服务并释放 WakeLock。
        fun refresh(
            context: Context,
            active: Boolean,
            notificationId: Int,
            notification: Notification?,
        ) {
            val intent = Intent(context, RunForegroundService::class.java)
            if (!active || notification == null) {
                runCatching { context.stopService(intent) }
                return
            }
            intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            intent.putExtra(EXTRA_NOTIFICATION, notification)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.onFailure { logDebug("ILife798", "RunForegroundService.refresh: ${it.message}") }
        }
    }
}
