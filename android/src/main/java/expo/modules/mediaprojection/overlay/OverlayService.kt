package expo.modules.mediaprojection.overlay

import android.util.Log
import android.os.Build
import android.os.IBinder
import android.app.Service
import android.content.Intent
import android.app.Notification
import android.view.WindowManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.content.pm.ServiceInfo
import expo.modules.mediaprojection.R
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.core.app.NotificationCompat
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import expo.modules.mediaprojection.TRIGGER_MEDIA_PROJECTION_CAPTURE
import expo.modules.mediaprojection.services.MediaProjectionService

class OverlayService : AbstractOverlayService() {
    private val trashSizeDp = 70
    private val overlayButtonDefaultSizeDp = 75
    private lateinit var overlayDraggableViewController: OverlayDraggableViewController

    override fun onDestroy() {
        super.onDestroy()
        stopScreenCapture()
        overlayDraggableViewController.disableView()
        this.stopSelf()
    }

    override fun onCreate() {
        super.onCreate()

        overlayDraggableViewController =
            OverlayDraggableViewController(
                windowManager = windowManager,
                service = this,
                onDestroyed = {
                    Log.d("Overlay", "Button Destroyed")
                    stopScreenCapture()
                    this.stopSelf()
                },

                buttonRadiusDp = overlayButtonDefaultSizeDp,
                trashSizeDp = trashSizeDp,
                viewAlpha = 0.8f,
            ) {
                Image(
                    painter = painterResource(R.drawable.icon),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            startScreenCapture()
                        },
                )
            }
    }

    override fun onOverlayServiceStarted() {
        overlayDraggableViewController.enableView()
    }

    private fun stopScreenCapture() {
        val serviceIntent = Intent(this, MediaProjectionService::class.java)
        stopService(serviceIntent)
        this.stopSelf()
    }

    private fun startScreenCapture() {
        val serviceIntent = Intent(this, MediaProjectionService::class.java)
        serviceIntent.putExtra(TRIGGER_MEDIA_PROJECTION_CAPTURE, true)
        startService(serviceIntent)
    }
}

abstract class AbstractOverlayService : Service() {
    val state = ServiceState()
    open val windowManager get() = getSystemService(WINDOW_SERVICE) as WindowManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    abstract fun onOverlayServiceStarted()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else { startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, buildNotification()) }

        onOverlayServiceStarted()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        var channelId = FOREGROUND_SERVICE_NOTIFICATION_CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) channelId = createNotificationChannel(channelId)
        val notification: Notification = NotificationCompat.Builder(this, channelId).build()
        return notification
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String): String {
        val service = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
        service.createNotificationChannel(channel)
        return channelId
    }

    companion object {
        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 3
        const val CHANNEL_NAME: String = "OverlayButtonService"
        const val FOREGROUND_SERVICE_NOTIFICATION_CHANNEL_ID = "FOREGROUND_SERVICE_NOTIFICATION_CHANNEL_ID"
    }
}
