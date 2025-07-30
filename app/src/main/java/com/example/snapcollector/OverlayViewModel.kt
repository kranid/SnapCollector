
package com.example.snapcollector

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

data class OverlayState(
    val issues: List<SnapIssue> = emptyList(),
    val currentIndex: Int = 0,
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isFabVisible: Boolean = true,
    val errorMessage: String? = null
)

class OverlayViewModel(
    private val context: Context,
    private val snapHubClient: SnapHubClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayState())
    val uiState: StateFlow<OverlayState> = _uiState.asStateFlow()

    fun startLoading() {
        _uiState.update { it.copy(isLoading = true, isFabVisible = false, errorMessage = null) }
    }

    fun stopLoading() {
        _uiState.update { it.copy(isLoading = false) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(isLoading = false, isFabVisible = true, errorMessage = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun runAccessibilityCheck(
        makeSnapshot: () -> List<SnapNode>?,
        captureScreenshot: suspend () -> Bitmap?,
        screenInfo: ScreenInfo?,
    ) {
        Log.d("OverlayViewModel", "runAccessibilityCheck called")
        viewModelScope.launch {
            startLoading()
            // Hide FAB before capturing screenshot
            _uiState.update { it.copy(isFabVisible = false) }

            val snapTree = withContext(Dispatchers.IO) { makeSnapshot() }
            Log.d("snapper", "snapTree is null: ${snapTree == null}")
            val screenshotBitmap = withContext(Dispatchers.IO) { captureScreenshot() }
            Log.d("snapper", "screenshotBitmap is null: ${screenshotBitmap == null}")

            // Show FAB after capturing screenshot
            _uiState.update { it.copy(isFabVisible = true) }

            if (snapTree == null || screenshotBitmap == null || screenInfo == null) {
                showError("Failed to get screen data.")
                return@launch
            }

            try {
                val sessionPath = FileUtils.saveSession(context, screenshotBitmap, snapTree, screenInfo.PackageName)
                Log.d("snapper", "sessionPath is null: ${sessionPath == null}")

                if (sessionPath == null) {
                    showError("Failed to save files.")
                     return@launch
    }

                val response = snapHubClient.saveSnap(snapTree, screenInfo)
                showError("Snapshot and screenshot saved and sent successfully!")

            } catch (e: Exception) {
                showError("An error occurred: ${e.message}")
            } finally {
                stopLoading()
                hideReport()
            }
        }
    }

    fun hideReport() {
        _uiState.update {
            it.copy(
                isVisible = false,
                isFabVisible = true
            )
        }
    }

    fun nextIssue() {
        _uiState.update {
            if (it.currentIndex < it.issues.size - 1) {
                it.copy(currentIndex = it.currentIndex + 1)
            } else {
                it
            }
        }
    }

    fun prevIssue() {
        _uiState.update {
            if (it.currentIndex > 0) {
                it.copy(currentIndex = it.currentIndex - 1)
            } else {
                it
            }
        }
    }
}
