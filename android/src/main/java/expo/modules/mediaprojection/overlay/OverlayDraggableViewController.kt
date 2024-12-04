package expo.modules.mediaprojection.overlay

import android.os.Build
import android.util.Log
import android.os.Bundle
import android.view.View
import android.app.Service
import android.view.Gravity
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import android.graphics.PixelFormat
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelStore
import androidx.compose.ui.unit.IntOffset
import android.hardware.input.InputManager
import androidx.compose.runtime.Composable
import androidx.lifecycle.LifecycleRegistry
import androidx.compose.runtime.MutableState
import android.content.Context.INPUT_SERVICE
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import android.content.res.Resources.getSystem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.compositionLocalOf
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

val Int.px: Int get() = (this * getSystem().displayMetrics.density).toInt()
val LocalServiceState = compositionLocalOf<ServiceState> { error("No ServiceState provided") }

interface OverlayInterface {
    fun enableView()
    fun disableView()
}

enum class OverlayEnableDisableMode {
    CREATE_AND_DESTROY,
    VISIBLE_AND_INVISIBLE,
}

class ServiceState {
    var screenWidthPx = Int.MAX_VALUE
    var screenHeightPx = Int.MAX_VALUE
    val overlayButtonState = OverlayState()
}

class OverlayState {
    var isTimerHoverTrash = false
    var showTrash by mutableStateOf(false)
    var timerOffset by mutableStateOf(IntOffset.Zero)
    val isVisible: MutableState<Boolean> = mutableStateOf(false)
}

class OverlayDraggableViewController(
    val windowManager: WindowManager,
    val service: AbstractOverlayService,
    val onDestroyed: () -> Unit,
    val buttonRadiusDp: Int,
    val trashSizeDp: Int,
    val viewAlpha: Float = 1.0f,
    val content: @Composable () -> Unit,
) : OverlayInterface {

    val buttonRadiusPx = buttonRadiusDp.px
    private val overlayButtonState = service.state.overlayButtonState

    private val trashScreenViewController = OverlayViewController(
        createOverlayViewHolder = this::createTrashScreenOverlay,
        windowManager = windowManager,
        name = "FullScreen"
    )

    private val overlayButtonViewController = OverlayViewController(
        createOverlayViewHolder = this::createOverlayButtonClickTarget,
        windowManager = windowManager,
        name = "Button ClickTarget"
    )

    private fun getLayoutType(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

        @Suppress("DEPRECATION")
        return WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun createTrashScreenOverlay(): OverlayViewHolder {
        var alpha = 1f
        val inputManager = service.applicationContext.getSystemService(INPUT_SERVICE) as InputManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alpha = inputManager.maximumObscuringOpacityForTouch
        }

        val fullscreenOverlay = OverlayViewHolder(
            windowManager = windowManager,
            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                getLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ),
            alpha = alpha,
            service = service,
        ) {
            CompositionLocalProvider(LocalServiceState provides service.state) {
                TrashContentScreen(
                    showOverlayButton = overlayButtonState.isVisible.value,
                    serviceState = service.state,
                    buttonRadiusDp = buttonRadiusDp,
                    trashSizeDp = trashSizeDp,
                )
            }
        }

        return fullscreenOverlay
    }

    private fun createOverlayButtonClickTarget(): OverlayViewHolder {
        val overlayButtonClickTarget = OverlayViewHolder(
            windowManager = windowManager,
            params = WindowManager.LayoutParams(
                buttonRadiusPx,
                buttonRadiusPx,
                0,
                0,
                getLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ),
            alpha = viewAlpha, service = service,
        )
        overlayButtonClickTarget.setContent {
            ClickTarget(
                serviceState = service.state,
                controller = this,
                overlayState = overlayButtonState,
                viewHolder = overlayButtonClickTarget,
                onDropOnTrash = {
                    this.disableView()
                    Log.d("Droppings here and there are what", "And are what not!")
                },
                content = content,
            )
        }
        return overlayButtonClickTarget
    }


    override fun enableView() {
        trashScreenViewController.enableView()
        overlayButtonViewController.enableView()
        overlayButtonState.isVisible.value = true
    }

    override fun disableView() {
        trashScreenViewController.disableView()
        overlayButtonViewController.disableView()
        overlayButtonState.isVisible.value = false
        onDestroyed()
    }
}

class OverlayViewHolder(
    alpha: Float,
    val service: AbstractOverlayService,
    private val windowManager: WindowManager,

    private val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        0,
        0,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        0,
        PixelFormat.TRANSLUCENT
    ),
    content: @Composable (composeView: ComposeView) -> Unit = {},
) {

    private val originalWindowFlag: Int
    private var view: ComposeView? = null
    private val originalWindowAlpha: Float

    init {
        params.alpha = alpha
        originalWindowFlag = params.flags
        originalWindowAlpha = params.alpha
        params.gravity = Gravity.TOP or Gravity.START
        this.view = overlayViewFactory(service = service, content = content)
    }

    fun getParams(): WindowManager.LayoutParams {
        return this.params
    }

    fun getView(): ComposeView? {
        return this.view
    }

    fun updateViewPos(x: Int, y: Int) {
        params.x = x
        params.y = y
        windowManager.updateViewLayout(view, params)
    }

    fun setVisible(visible: Boolean) {
        if (visible) {
            params.flags = originalWindowFlag
            params.alpha = originalWindowAlpha
            windowManager.updateViewLayout(view, params)
        } else {
            params.alpha = 0f
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            windowManager.updateViewLayout(view, params)
        }
    }

    fun setContent(content: @Composable (composeView: ComposeView) -> Unit = {}) {
        this.view = overlayViewFactory(service = service, content = content)
    }
}

class OverlayViewController(
    val name: String = "",
    val windowManager: WindowManager,
    val createOverlayViewHolder: () -> OverlayViewHolder,
    private val enableDisableMode: OverlayEnableDisableMode = OverlayEnableDisableMode.VISIBLE_AND_INVISIBLE,
) : OverlayInterface {
    private var enabledCount = 0
    private var viewHolder: OverlayViewHolder? = null

    init {
        when (enableDisableMode) {
            OverlayEnableDisableMode.VISIBLE_AND_INVISIBLE -> {
                viewHolder = createOverlayViewHolder()
                windowManager.addView(viewHolder!!.getView(), viewHolder!!.getParams())
                viewHolder!!.setVisible(false)
            }

            OverlayEnableDisableMode.CREATE_AND_DESTROY -> {}
        }
    }

    override fun enableView() {
        when (enableDisableMode) {
            OverlayEnableDisableMode.VISIBLE_AND_INVISIBLE -> {
                viewHolder!!.setVisible(true)
            }

            OverlayEnableDisableMode.CREATE_AND_DESTROY -> {
                if (enabledCount >= 1) {
                    throw IllegalStateException("Cannot reuse this view controller when [enableDisableMode] is set to [${this.enableDisableMode}] " + "because it is not thread safe when creating/destroying, a new instance is needed ")
                }

                viewHolder = createOverlayViewHolder()
                windowManager.addView(viewHolder!!.getView(), viewHolder!!.getParams())
            }
        }

        enabledCount++
    }

    override fun disableView() {
        when (enableDisableMode) {
            OverlayEnableDisableMode.VISIBLE_AND_INVISIBLE -> {
                viewHolder!!.setVisible(false)
            }

            OverlayEnableDisableMode.CREATE_AND_DESTROY -> {
                windowManager.removeView(viewHolder!!.getView())
            }
        }
    }
}

fun overlayViewFactory(
    service: Service,
    content: @Composable (composeView: ComposeView) -> Unit = {}
): ComposeView {

    val composeView = ComposeView(context = service)
    composeView.id = View.generateViewId()
    composeView.setContent(content = { content(composeView) })

    val viewModelStore = ViewModelStore()
    val lifecycleOwner = MyLifecycleOwner()
    lifecycleOwner.performRestore(null)
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

    val viewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore
            get() = viewModelStore
    }

    composeView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
    composeView.setViewTreeLifecycleOwner(lifecycleOwner)
    composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
    return composeView
}

private class MyLifecycleOwner : SavedStateRegistryOwner {
    private var mLifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private var mSavedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = mLifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = mSavedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        mLifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: Bundle?) {
        mSavedStateRegistryController.performRestore(savedState)
    }
}