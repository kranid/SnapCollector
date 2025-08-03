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
import java.io.ByteArrayOutputStream
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
    val isSnapshotReviewVisible: Boolean = false,
    val isNodeEditScreenVisible: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isUniversalMessageVisible: Boolean = false,
    val universalMessageTitle: String? = null,
    val universalMessageText: String? = null
)

class OverlayViewModel(
    private val context: Context,
    private val snapHubClient: SnapHubClient,
    private val getScreenInfo: () -> ScreenInfo
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayState())
    val uiState: StateFlow<OverlayState> = _uiState.asStateFlow()

    fun startLoading() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null, isUniversalMessageVisible = false, universalMessageTitle = null, universalMessageText = null) }
    }

    fun stopLoading() {
        _uiState.update { it.copy(isLoading = false) }
    }

    fun showError(message: String) {
        Log.e("OverlayViewModel", "Showing error: $message")
        _uiState.update { it.copy(errorMessage = message, isUniversalMessageVisible = true, universalMessageTitle = "Ошибка", universalMessageText = message) }
    }

    fun showSuccess(message: String) {
        Log.i("OverlayViewModel", "Showing success: $message")
        _uiState.update { it.copy(successMessage = message, isUniversalMessageVisible = true, universalMessageTitle = "Успех", universalMessageText = message) }
    }

    fun hideSuccessOrError() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null, isUniversalMessageVisible = false, universalMessageTitle = null, universalMessageText = null) }
    }

    fun runAccessibilityCheck(
        makeSnapshot: () -> List<SnapNode>?,
        snapshotReviewViewModel: SnapshotReviewViewModel,
        takeScreenshot: suspend () -> Bitmap? // Add this parameter
    ) {
        Log.d("OverlayViewModel", "runAccessibilityCheck called")
        viewModelScope.launch {
            _uiState.update { it.copy(isFabVisible = false) } // Скрыть FAB сразу
            try {
                val snapTree = withContext(Dispatchers.IO) { makeSnapshot() }
                val screenshotBitmap = withContext(Dispatchers.IO) { takeScreenshot() }

                if (snapTree == null) {
                    showError("Failed to get screen data.")
                    _uiState.update { it.copy(isFabVisible = true) } // Показать FAB при ошибке
                    return@launch
                }

                val screenshotByteArray = screenshotBitmap?.let {
                    val outputStream = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.toByteArray()
                }

                if (screenshotByteArray == null) {
                    showError("Failed to capture screenshot.")
                    _uiState.update { it.copy(isFabVisible = true) } // Показать FAB при ошибке
                    return@launch
                }

                startLoading() // Показать индикатор загрузки после захвата
                snapshotReviewViewModel.setSnapNodes(snapTree, screenshotByteArray)
                _uiState.update { it.copy(isSnapshotReviewVisible = true) }
                stopLoading()
            } catch (e: Exception) {
                showError("Ошибка при создании снимка: ${e.message}")
                _uiState.update { it.copy(isFabVisible = true) } // Показать FAB при ошибке
            }
        }
    }

    fun showNodeEditScreen() {
        _uiState.update { it.copy(isNodeEditScreenVisible = true, isSnapshotReviewVisible = false) }
    }

    fun hideNodeEditScreen() {
        _uiState.update { it.copy(isNodeEditScreenVisible = false, isSnapshotReviewVisible = true) }
    }

    fun hideSnapshotReview() {
        _uiState.update {
            it.copy(
                isSnapshotReviewVisible = false,
                isFabVisible = true // Показать FAB при скрытии SnapshotReviewScreen
            )
        }
    }

    fun hideReport() {
        _uiState.update {
            it.copy(
                isVisible = false,
                isFabVisible = true // Показать FAB при скрытии ReportOverlay
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