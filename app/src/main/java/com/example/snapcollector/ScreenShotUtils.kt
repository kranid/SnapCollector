package com.example.snapcollector


import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.ScreenshotResult
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback
import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.util.Base64
import android.util.Log
import android.view.Display
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

/**
 * Делает скриншот основного экрана и возвращает его как base64 PNG строку.
 */
 // API 30
suspend fun AccessibilityService.takeScreenshotAsBitmap(): Bitmap? = suspendCancellableCoroutine { cont ->
    takeScreenshot(
        Display.DEFAULT_DISPLAY,
        mainExecutor,
        object : TakeScreenshotCallback {
            override fun onSuccess(result: ScreenshotResult) {
                val buffer: HardwareBuffer = result.hardwareBuffer
                val colorSpace = result.colorSpace
                val bitmap = buffer.let {
                    Bitmap.wrapHardwareBuffer(it, colorSpace)
                }

                if (bitmap == null) {
                    Log.e("ScreenshotUtils", "Не удалось создать Bitmap из скриншота")
                    cont.resume(null)
                    return
                }

                cont.resume(bitmap)
            }

            override fun onFailure(errorCode: Int) {
                Log.e("ScreenshotUtils", "Ошибка скриншота: $errorCode")
                cont.resume(null)
            }
        }
    )
}

/**
 * Преобразует Bitmap в base64 PNG.
 */
fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}
