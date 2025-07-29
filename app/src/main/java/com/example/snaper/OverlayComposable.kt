package com.example.snaper

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.size

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun OverlayContent(
    state: OverlayState,
    onMakeSnapshot: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onClearError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) {
        Box(modifier = Modifier.padding(it)) {
            when {
                state.isLoading -> LoadingIndicator()
                state.isVisible -> ReportOverlay(state, onPrev, onNext, onClose, it)
            }
        }
    }
}

@Composable
fun FloatingActionButton(onClick: () -> Unit) {
    Box(modifier = Modifier) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .alpha(0.5f)
                
        ) {
            Text("Make Snapshot")
        }
    }
}

@Composable
fun ReportOverlay(
    state: OverlayState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    paddingValues: PaddingValues
) {
    if (!state.isVisible) return

    val issue = state.issues.getOrNull(state.currentIndex)

    var buttonsColumnCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val view = LocalView.current
    val viewLocation = IntArray(2)
    view.getLocationOnScreen(viewLocation)
    val viewOffset = Offset(viewLocation[0].toFloat(), viewLocation[1].toFloat())

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val systemBarsInsets = WindowInsets.systemBars.asPaddingValues(density)
    val systemTopPaddingPx = with(density) { systemBarsInsets.calculateTopPadding().toPx() }
    val systemLeftPaddingPx = with(density) { systemBarsInsets.calculateLeftPadding(layoutDirection).toPx() }

    Log.d("OverlayDebug", "issue.rect: ${issue?.rect}")
    Log.d("OverlayDebug", "paddingValues: $paddingValues")
    Log.d("OverlayDebug", "systemBarsInsets: $systemBarsInsets")
    Log.d("OverlayDebug", "systemTopPaddingPx: $systemTopPaddingPx, systemLeftPaddingPx: $systemLeftPaddingPx")
    Log.d("OverlayDebug", "viewOffset: $viewOffset")

    Box(modifier = Modifier.fillMaxSize()) {
        var buttonsOffset by remember { mutableStateOf(Offset.Zero) }

        if (issue != null) {
            val issueRect = Rect(
                left = issue.rect.left.toFloat() - viewOffset.x,
                top = issue.rect.top.toFloat() - viewOffset.y,
                right = issue.rect.right.toFloat() - viewOffset.x,
                bottom = issue.rect.bottom.toFloat() - viewOffset.y
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas {
                    val paint = Paint().apply {
                        color = Color.Red.copy(alpha = 0.5f)
                    }

                    it.drawRect(
                        issueRect.left,
                        issueRect.top,
                        issueRect.right,
                        issueRect.bottom,
                        paint
                    )
                }
            }

            buttonsColumnCoordinates?.let { coords ->
                val buttonsRect = Rect(
                    left = coords.boundsInWindow().left - viewOffset.x,
                    top = coords.boundsInWindow().top - viewOffset.y,
                    right = coords.boundsInWindow().right - viewOffset.x,
                    bottom = coords.boundsInWindow().bottom - viewOffset.y
                )

                Log.d("OverlayDebug", "issueRect: $issueRect")
                Log.d("OverlayDebug", "buttonsRect: $buttonsRect")

                if (issueRect.overlaps(buttonsRect)) {
                    // If they intersect, move buttons to the top
                    buttonsOffset = Offset(0f, -(buttonsRect.height + with(density) { 16.dp.toPx() })) // Move above the screen
                    Log.d("OverlayDebug", "Buttons intersect, new buttonsOffset: $buttonsOffset")
                } else {
                    buttonsOffset = Offset.Zero
                    Log.d("OverlayDebug", "Buttons do not intersect, buttonsOffset: $buttonsOffset")
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.White)
                .padding(16.dp)
                .offset(x = buttonsOffset.x.dp, y = buttonsOffset.y.dp)
                .onGloballyPositioned {
                    buttonsColumnCoordinates = it
                }
        ) { 
            if (issue != null) {
                Text(text = issue.message)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = onPrev) {
                    Text("Prev")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onNext) {
                    Text("Next")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onClose) {
                    Text("Close")
                }
            }
        }
    }
}