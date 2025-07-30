package com.example.snapcollector

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OverlayViewModelFactory(private val context: Context, private val snapHubClient: SnapHubClient) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OverlayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OverlayViewModel(context, snapHubClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class OverlayManager(private val context: Context, snapHubClient: SnapHubClient) : ViewModelStoreOwner, SavedStateRegistryOwner, LifecycleOwner {
    private val windowManager: WindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
    
    private var mainOverlayView: ComposeView? = null
    private var fabOverlayView: ComposeView? = null

    val viewModel: OverlayViewModel by lazy {
        ViewModelProvider(this, OverlayViewModelFactory(context, snapHubClient))[OverlayViewModel::class.java]
    }

    // ViewModelStoreOwner implementation
    override val viewModelStore: ViewModelStore = ViewModelStore()

    // SavedStateRegistryOwner implementation
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // LifecycleOwner implementation
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        fabOverlayView?.let { windowManager.removeView(it) }
        mainOverlayView?.let { windowManager.removeView(it) }
        fabOverlayView = null
        mainOverlayView = null
    }

    fun showOverlay(makeSnapshot: () -> Unit) {
        // Initialize FAB overlay
        fabOverlayView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@OverlayManager)
            setViewTreeViewModelStoreOwner(this@OverlayManager)
            setViewTreeSavedStateRegistryOwner(this@OverlayManager)
            setContent {
                val state by viewModel.uiState.collectAsState()
                if (state.isFabVisible) {
                    FloatingActionButton(onClick = makeSnapshot) {
                        Text("Make Snapshot")
                    }
                }
            }
        }

        val fabParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100 // Offset from bottom
        }
        windowManager.addView(fabOverlayView, fabParams)

        // Initialize main overlay (for loading/report)
        mainOverlayView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@OverlayManager)
            setViewTreeViewModelStoreOwner(this@OverlayManager)
            setViewTreeSavedStateRegistryOwner(this@OverlayManager)
            setContent {
                val state by viewModel.uiState.collectAsState()
                // Only show loading or report content here
                if (state.isLoading || state.isVisible) {
                    OverlayContent(
                        state = state,
                        onMakeSnapshot = makeSnapshot,
                        onPrev = { viewModel.prevIssue() },
                        onNext = { viewModel.nextIssue() },
                        onClose = { viewModel.hideReport() },
                        onClearError = { viewModel.clearError() }
                    )
                } else {
                    mainOverlayView?.visibility = View.GONE
                }
            }
        }

        val mainParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            0, // Flags: 0 means it will receive touches and focus
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        // Add main overlay initially hidden
        windowManager.addView(mainOverlayView, mainParams)
        mainOverlayView?.visibility = View.GONE

        // Observe ViewModel state to control visibility
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.uiState.collect { state ->
                if (state.isLoading || state.isVisible) {
                    mainOverlayView?.visibility = View.VISIBLE
                } else {
                    mainOverlayView?.visibility = View.GONE
                }
            }
        }
    }
}