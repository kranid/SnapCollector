package com.example.snapcollector

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

data class ScreenInfo(var Name: String, val PackageName: String)

class MyAccessibilityService : AccessibilityService() {
    

    private val tag = "snapper"
    private val snapHubClient: SnapHubClient = SnapHubClient()
    private var screenInfo: ScreenInfo? = null
    private val excludedActivityNames: HashSet<String> = hashSetOf(
        "",
        "android.widget.RelativeLayout",
        "android.widget.LinearLayout",
        "android.view.View",
        "android.inputmethodservice.SoftInputWindow"
    )
    private lateinit var overlayManager: OverlayManager
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val info = ScreenInfo(
                PackageName = event.packageName?.toString() ?: "",
                Name = event.className?.toString() ?: ""
            )
            if (screenInfo == null) {
                screenInfo = info
            } else if (!excludedActivityNames.contains(info.Name)) {
                screenInfo = info
            }
            Log.i(tag, "screen info: ${screenInfo?.Name}, ${screenInfo?.PackageName}")
        }
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(tag, "The snapcollector has successfully started")
        overlayManager = OverlayManager(this, snapHubClient)
        overlayManager.showOverlay { makeAndSaveSnapshot() }

        this.serviceInfo = serviceInfo
    }

    override fun onInterrupt() {
        overlayManager.destroy()
    }

    private fun makeAndSaveSnapshot() {
        Log.d(tag, "makeAndSaveSnapshot called")
        overlayManager.viewModel.runAccessibilityCheck(
            makeSnapshot = ::makeSnapshot,
            captureScreenshot = ::takeScreenshotAsBitmap,
            screenInfo = screenInfo
        )
    }

    private fun makeSnapshot(): List<SnapNode>? {
        val root = this.rootInActiveWindow
        if (root == null) {
            Log.e(tag, "rootInActiveWindow is null, cannot create snapshot.")
            return null
        }
        val controller = SnapController()
        return try {
            controller.createSnapTree(AccessibilityNodeInfoCompat.wrap(root))
        } catch (e: Exception) {
            Log.e(tag, "Error creating snapshot tree: ${e.message}")
            null
        }
    }

    

    private fun getScreenInfo(): ScreenInfo =
        ScreenInfo(
            PackageName = rootInActiveWindow.packageName.toString(),
            Name = "${rootInActiveWindow.packageName}/${rootInActiveWindow.className}"
        )

}