package com.example.snapcollector

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.util.Log

object FileUtils {

    private val tag = "snapper"

    private fun getAppDirectory(appName: String): File {
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), appName)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    fun saveSession(context: Context, bitmap: Bitmap, snapshot: List<SnapNode>, appName: String): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val sessionDirectory = File(getAppDirectory(appName), timestamp)
            if (!sessionDirectory.exists()) {
                sessionDirectory.mkdirs()
            }

            // Save screenshot
            val screenshotFile = File(sessionDirectory, "screenshot.png")
            FileOutputStream(screenshotFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Save snapshot
            val snapshotFile = File(sessionDirectory, "snapshot.json")
            val jsonString = Json { prettyPrint = true }.encodeToString(snapshot)
            snapshotFile.writeText(jsonString)
            
            sessionDirectory.absolutePath
        } catch (e: Exception) {
            Log.e(tag, "Error saving session: ${e.message}")
            null
        }
    }
}