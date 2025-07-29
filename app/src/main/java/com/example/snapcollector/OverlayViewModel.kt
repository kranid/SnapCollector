
package com.example.snapcollector

import android.util.Log
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val geminiClient: GeminiClient,
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
        promt: String
    ) {
        Log.d("OverlayViewModel", "runAccessibilityCheck called")
        viewModelScope.launch {
            val snapTree = withContext(Dispatchers.IO) { makeSnapshot() }
            val screenshotBitmap = withContext(Dispatchers.IO) { captureScreenshot() }

            if (snapTree == null || screenshotBitmap == null || screenInfo == null) {
                showError("Failed to get screen data.")
                return@launch
            }

            startLoading() // Move startLoading here

            try {
                snapHubClient.saveSnap(snapTree, screenInfo)
                val json = Json.encodeToJsonElement(snapTree).toString()

                val result = geminiClient.checkSnapshot(screenshotBitmap, "$promt $json")
                result.onSuccess { report ->
                    if (report.isEmpty()) {
                        showError("No accessibility issues found!")
                        hideReport() // Go back to the FAB
                    } else {
                        snapHubClient.saveReport(report, screenInfo)
                        _uiState.update {
                            it.copy(
                                issues = report,
                                currentIndex = 0,
                                isVisible = true,
                                isLoading = false,
                                isFabVisible = false,
                                errorMessage = null
                            )
                        }
                    }
                }.onFailure { e ->
                    showError("An error occurred: ${e.message}")
                }
            } catch (e: Exception) {
                showError("A processing error occurred: ${e.message}")
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
