package dev.rk.systemapps.files.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.rk.systemapps.core.common.format.formatBytes
import dev.rk.systemapps.files.MainActivity
import dev.rk.systemapps.files.R
import dev.rk.systemapps.files.data.operation.FileOperationManager
import dev.rk.systemapps.files.data.operation.OperationQueueState
import dev.rk.systemapps.files.domain.model.OperationState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * Dosya işlemlerini uygulama arka plandayken de sürdüren foreground servis.
 *
 * İşi kendisi yapmaz: [FileOperationManager] kuyruğu yürütür, servis yalnızca süreci
 * ayakta tutar ve bildirimi günceller. Böylece UI çakışma diyaloğu için servise
 * Intent göndermek zorunda kalmaz.
 */
@AndroidEntryPoint
class FileOperationService : Service() {

    @Inject
    lateinit var manager: FileOperationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observer: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            manager.cancelCurrent()
            return START_NOT_STICKY
        }

        // Foreground'a geçiş 5 sn içinde yapılmak zorunda; kuyruk durumu beklenmez.
        startForegroundCompat(buildNotification(OperationQueueState()))

        if (observer == null) {
            observer = scope.launch {
                manager.state.collectLatest { state ->
                    if (state.isBusy || state.queued > 0) {
                        notificationManager().notify(NOTIFICATION_ID, buildNotification(state))
                    } else {
                        // Servisi dışarıdan stopService ile öldürmek, işlem çok kısa
                        // sürdüğünde onStartCommand hiç çalışmadan servisi sonlandırıp
                        // ForegroundServiceDidNotStartInTimeException'a yol açıyordu.
                        // Bu yüzden servis kendi kendini durduruyor: startForeground
                        // çağrıldıktan sonra.
                        stopSelf()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        observer?.cancel()
        scope.cancel()
        // Bildirim NotificationManager üzerinden de güncellendiği için servis ölünce
        // kendiliğinden kalkmıyor; açıkça kaldırılmazsa kullanıcıda silinemeyen bir
        // "işlem sürüyor" bildirimi kalıyor.
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationManager().cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    private fun buildNotification(state: OperationQueueState): Notification {
        val progress = state.current
        val title = when (progress?.state) {
            OperationState.PREPARING -> getString(R.string.operation_preparing)
            else -> getString(R.string.operation_running)
        }

        val cancelIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, FileOperationService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.action_cancel), cancelIntent)

        if (progress != null) {
            builder.setContentText(
                getString(
                    R.string.operation_counter,
                    progress.processedItems,
                    progress.totalItems,
                    formatBytes(progress.processedBytes),
                    formatBytes(progress.totalBytes),
                ),
            )
            val fraction = progress.fraction
            if (fraction == null) {
                builder.setProgress(0, 0, true)
            } else {
                builder.setProgress(PROGRESS_MAX, (fraction * PROGRESS_MAX).toInt(), false)
            }
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun startForegroundCompat(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_operations),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(NotificationManager::class.java)

    companion object {
        private const val CHANNEL_ID = "file_operations"
        private const val NOTIFICATION_ID = 1001
        private const val PROGRESS_MAX = 1000
        private const val ACTION_CANCEL = "dev.rk.systemapps.files.CANCEL_OPERATION"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FileOperationService::class.java),
            )
        }

        /**
         * Servis kuyruk boşalınca kendini durduruyor (bkz. [onStartCommand]); buradan
         * `stopService` çağrılmıyor. Kuyruk durumunu servis de gözlediği için bu
         * çağrıya iş düşmüyor, yine de arayüz sözleşmesi korunuyor.
         */
        fun stop(@Suppress("UNUSED_PARAMETER") context: Context) = Unit
    }
}
