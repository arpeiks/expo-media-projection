package expo.modules.mediaprojection

import android.net.Uri
import android.util.Log
import android.os.Build
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.provider.Settings
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.exception.toCodedException
import android.media.projection.MediaProjectionManager
import expo.modules.mediaprojection.overlay.OverlayService
import expo.modules.mediaprojection.services.MediaProjectionService

class ExpoMediaProjectionModule : Module() {
  private val context: Context
    get() = appContext.reactContext ?: throw Exceptions.ReactContextLost()

  private var pathType: String = "FOLDER"
  private var path: String = "MediaProjection"
  private var pendingOverlayPromise: Promise? = null
  private var pendingMediaProjectionPromise: Promise? = null

  override fun definition() = ModuleDefinition {
    Name(TAG)

    AsyncFunction("takeScreenshot") { promise: Promise ->
      try {
        val serviceIntent = Intent(context, MediaProjectionService::class.java)
        serviceIntent.putExtra(TRIGGER_MEDIA_PROJECTION_CAPTURE, true)
        context.startService(serviceIntent)
        promise.resolve(true)
      } catch (e: Exception) {
        Log.d("takeScreenshot", e.toString())
        promise.reject(e.toCodedException())
      }
    }

    AsyncFunction("stop") { promise: Promise ->
      try {
        context.stopService(Intent(context, MediaProjectionService::class.java))
        context.stopService(Intent(context, OverlayService::class.java))
        promise.resolve(true)
      } catch (e: Exception) {
        Log.d("stopMediaProjection", e.toString())
        promise.reject(e.toCodedException())
      }
    }

    AsyncFunction("showScreenshotButton") { promise: Promise ->
      try {
        val intent = Intent(context.applicationContext, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else { context.startService(intent) }
        promise.resolve(true)
      } catch (e: Exception) {
        promise.reject(e.toCodedException())
      }
    }

    AsyncFunction("askMediaProjectionPermission") { options: Map<String, Any>, promise: Promise ->
      try {
        path = (options["path"] as? String)!!
        pathType = (options["pathType"] as? String)!!
        val mediaProjectionManager = context.getSystemService(MediaProjectionManager::class.java)
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        appContext.throwingActivity.startActivityForResult(intent, MEDIA_PROJECTION_REQUEST_CODE)
        pendingMediaProjectionPromise = promise
      } catch (e: Throwable) {
        Log.d("askMediaProjectionPermission", e.toString())
        promise.reject(e.toCodedException())
      }
    }

    AsyncFunction("askForOverlayPermission") { promise: Promise ->
      try {
        val allowed = Settings.canDrawOverlays(context)
        if (allowed) { promise.resolve(true); return@AsyncFunction }

        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:" + context.packageName)
        appContext.currentActivity?.startActivityForResult(intent, OVERLAY_REQUEST_CODE)
        pendingOverlayPromise = promise
      } catch (e: Exception) {
        Log.d("askForOverlayPermission", e.toString())
        promise.reject(e.toCodedException())
      }
    }

    OnActivityResult { _, payload ->
      val ok = payload.resultCode == Activity.RESULT_OK

      if (payload.requestCode == OVERLAY_REQUEST_CODE) {
        pendingOverlayPromise?.resolve(ok)
        pendingOverlayPromise = null
        return@OnActivityResult
      }

      if (payload.requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
        val intent = Intent(context, MediaProjectionService::class.java)
        intent.putExtra("path", path)
        intent.putExtra("pathType", pathType)
        intent.putExtra("data", payload.data)
        intent.putExtra("code", payload.resultCode)
        intent.putExtra("width", context.resources.displayMetrics.widthPixels)
        intent.putExtra("density", context.resources.displayMetrics.densityDpi)
        intent.putExtra("height", context.resources.displayMetrics.heightPixels)
        if (ok) context.startService(intent)
        pendingMediaProjectionPromise?.resolve(ok)
        pendingMediaProjectionPromise = null
      }
    }
  }

  companion object {
    private const val OVERLAY_REQUEST_CODE = 2
    private const val TAG = "ExpoMediaProjection"
    private const val MEDIA_PROJECTION_REQUEST_CODE = 1
  }
}
