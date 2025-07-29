package com.example.snaper

import com.example.snaper.takeScreenshotAsBitmap
import android.graphics.Bitmap
import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.example.snaper.promt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScreenInfo(var Name: String, val PackageName: String)

class MyAccessibilityService : AccessibilityService() {
    private val ApiKey = "AIzaSyD8GLiiTXHdgdahZE4frRhQhMcLD82KWnM"
    private val gemini = GeminiClient(ApiKey)

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
        Log.i(tag, "The snapper has successfully started")
        overlayManager = OverlayManager(this, gemini, snapHubClient)
        overlayManager.showOverlay { makeAndSaveSnapshot() }
    }

    override fun onInterrupt() {
        overlayManager.destroy()
    }

    private fun makeAndSaveSnapshot() {
        Log.d(tag, "makeAndSaveSnapshot called")
        overlayManager.viewModel.runAccessibilityCheck(
            makeSnapshot = ::makeSnapshot,
            captureScreenshot = ::takeScreenshotAsBitmap,
            screenInfo = screenInfo,
            promt = promt
        )
    }

    private fun makeSnapshot(): List<SnapNode>? {
        val root = this.rootInActiveWindow ?: return null
        val controller = SnapController()
        return try {
            controller.createSnapTree(AccessibilityNodeInfoCompat.wrap(root))
        } catch (e: Exception) {
            Log.i(tag, e.stackTraceToString())
            null
        }
    }

    

    private fun getScreenInfo(): ScreenInfo =
        ScreenInfo(
            PackageName = rootInActiveWindow.packageName.toString(),
            Name = "${rootInActiveWindow.packageName}/${rootInActiveWindow.className}"
        )

}