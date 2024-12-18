package expo.modules.mediaprojection.services

import java.io.File
import java.sql.Date
import android.util.Log
import java.util.Locale
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.app.Service
import android.content.Intent
import android.os.Environment
import android.content.Context
import android.graphics.Bitmap
import android.app.Notification
import java.text.SimpleDateFormat
import android.os.VibratorManager
import android.provider.MediaStore
import android.os.VibrationEffect
import android.graphics.PixelFormat
import android.content.ContentValues
import android.content.pm.ServiceInfo
import expo.modules.mediaprojection.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.hardware.display.VirtualDisplay
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import androidx.exifinterface.media.ExifInterface
import android.media.projection.MediaProjectionManager
import expo.modules.mediaprojection.TRIGGER_MEDIA_PROJECTION_CAPTURE

fun normalizePath(path: String): String {
    return path.replace(Regex("/{2,}"), "/") 
}

class MediaProjectionService : Service() {
    private var resultCode = 0
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0
    private var pathType = "FOLDER"
    private var path = "MediaProjection"
    private var resultData: Intent? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null
    private var imageReader: android.media.ImageReader? = null
    private lateinit var mediaProjectionCallback: MediaProjection.Callback

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val triggerCapture = intent.getBooleanExtra(TRIGGER_MEDIA_PROJECTION_CAPTURE, false)

        if (triggerCapture) {
            captureScreenshot()
            return START_STICKY
        }

        val mPath = intent.getStringExtra("path")
        val mPathType = intent.getStringExtra("pathType")
        resultCode = intent.getIntExtra("code", -1)
        screenWidth = intent.getIntExtra("width", 720)
        screenDensity = intent.getIntExtra("density", 1)
        screenHeight = intent.getIntExtra("height", 1280)

        if (mPath != null) path = mPath
        if (mPathType != null) pathType = mPathType

        resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("data", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("data")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = MEDIA_PROJECTION_NOTIFICATION_CHANNEL_ID
            val channelName = MEDIA_PROJECTION_NOTIFICATION_CHANNEL_NAME
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)

            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            channel.description = MEDIA_PROJECTION_NOTIFICATION_CHANNEL_DESCRIPTION
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            val notification = Notification.Builder(applicationContext, channelId)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_screen_capture)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(MEDIA_PROJECTION_NOTIFICATION_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else { startForeground(MEDIA_PROJECTION_NOTIFICATION_SERVICE_ID, notification) }
        }
        else { startForeground(MEDIA_PROJECTION_NOTIFICATION_SERVICE_ID, Notification()) }

        imageReader = createImageReader()
        mediaProjection = createMediaProjection()
        virtualDisplay = createVirtualDisplay()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createMediaProjection(): MediaProjection? {
        resultData?.let {
            val mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
            val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, it)

            mediaProjectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
            }
            mediaProjection.registerCallback(mediaProjectionCallback, android.os.Handler(android.os.Looper.getMainLooper()))
            return mediaProjection
        }

        return null
    }

    private fun createImageReader(): android.media.ImageReader {
        val imageReader = android.media.ImageReader.newInstance(
            screenWidth,
            screenHeight,
            PixelFormat.RGBA_8888,
            2
        )

        return imageReader
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        return mediaProjection?.createVirtualDisplay(
            TAG,
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    private fun captureScreenshot() {
        Thread.sleep(500)
        val image = imageReader?.acquireLatestImage()

        if (image == null) {
            Log.w(TAG, "No image available to capture")
            return
        }

        val curDate = Date(System.currentTimeMillis())
        val curTime = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(curDate).replace(" ", "")

        val filename = getAppName() + "-" + curTime + ".jpg"
        var folderPath = if (pathType == "FOLDER") {
            (Environment.DIRECTORY_PICTURES).toString() + "/" + path + "/"
        } else { path }

        folderPath = normalizePath(folderPath)

        val folder = File(folderPath)
        if (!folder.exists()) folder.mkdirs()

        image.let {
            val planes = it.planes
            val buffer = it.planes[0].buffer
            val rowStride = planes[0].rowStride
            val pixelStride = planes[0].pixelStride
            val rowPadding = rowStride - pixelStride * it.width
            val bitmap = Bitmap.createBitmap(it.width + rowPadding / pixelStride, it.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)

            val resolver = applicationContext.contentResolver
            val imageCollection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

            val currentTime = System.currentTimeMillis()
            val formatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()) // Exif-compatible format

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, folderPath)
                put(MediaStore.Images.Media.DATE_TAKEN, currentTime)
            }

            val imageUri = resolver.insert(imageCollection, contentValues)

            if (imageUri != null) {
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                }

                resolver.openFileDescriptor(imageUri, "rw")?.use { fileDescriptor ->
                    val exif = ExifInterface(fileDescriptor.fileDescriptor)
                    exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, formatter.format(Date(currentTime)))
                    exif.saveAttributes()
                }
            } else {
                Log.e(TAG, "Failed to create MediaStore entry")
            }

            bitmap.recycle()
            image.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(SCREENSHOT_VIBRATION_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(SCREENSHOT_VIBRATION_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(SCREENSHOT_VIBRATION_DURATION_MS)
                }
            }

            val resultIntent = Intent("com.example.MyService.RESULT").apply { putExtra("result", true) }
            sendBroadcast(resultIntent)
        }
    }

    private fun getAppName(): String? {
        val packageManager = applicationContext.packageManager

        val applicationInfo: ApplicationInfo = try {
            packageManager.getApplicationInfo(applicationContext.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return null
        }

        return (packageManager.getApplicationLabel(applicationInfo)) as String
    }

    override fun onDestroy() {
        super.onDestroy()

        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        virtualDisplay?.release()
        mediaProjection?.stop()
        mediaProjection = null
        virtualDisplay = null
        imageReader?.close()
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private const val SCREENSHOT_VIBRATION_DURATION_MS = 300L
        private const val MEDIA_PROJECTION_NOTIFICATION_SERVICE_ID = 2
        private val TAG = MediaProjectionService::class.java.simpleName
        private const val MEDIA_PROJECTION_NOTIFICATION_CHANNEL_NAME = "Screenshot Notifications"
        private const val MEDIA_PROJECTION_NOTIFICATION_CHANNEL_ID = "MEDIA_PROJECTION_NOTIFICATION_CHANNEL_ID"
        private const val MEDIA_PROJECTION_NOTIFICATION_CHANNEL_DESCRIPTION = "Notifications for screenshot capture"
    }
}