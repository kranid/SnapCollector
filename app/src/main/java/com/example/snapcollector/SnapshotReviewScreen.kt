package com.example.snapcollector

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


@Composable
fun SnapshotReviewScreen(viewModel: SnapshotReviewViewModel) {
    val isVisible by viewModel.isVisible.collectAsState()
    if (!isVisible) return

    val snapNodes by viewModel.snapNodes.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f) // Take up all available space except for the button
        ) {
            items(snapNodes) { node ->
                if (node.role == Role.EDIT_TEXT) {
                    OutlinedTextField(
                        value = node.text,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction("Редактировать") {
                                        viewModel.onEditNode(node)
                                        true
                                    },
                                    CustomAccessibilityAction("Удалить") {
                                        viewModel.removeNode(node)
                                        true
                                    }
                                ).toMutableList().apply {
                                    val currentIndex = snapNodes.indexOf(node)
                                    if (currentIndex > 0) {
                                        add(CustomAccessibilityAction("Переместить вверх") {
                                            viewModel.moveNodeUp(node)
                                            true
                                        })
                                    }
                                    if (currentIndex < snapNodes.size - 1) {
                                        add(CustomAccessibilityAction("Переместить вниз") {
                                            viewModel.moveNodeDown(node)
                                            true
                                        })
                                    }
                                    if (currentIndex > 0) {
                                        add(CustomAccessibilityAction("Объединить с предыдущим") {
                                            viewModel.mergeWithPrevious(node)
                                            true
                                        })
                                    }
                                    if (currentIndex < snapNodes.size - 1) {
                                        add(CustomAccessibilityAction("Объединить со следующим") {
                                            viewModel.mergeWithNext(node)
                                            true
                                        })
                                    }
                                }
                            }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .clearAndSetSemantics {
                                // Set text and content description from node.text
                                node.text.let {
                                    this.text = AnnotatedString(it)
                                    this.contentDescription = it
                                }
                                // Set role
                                node.role.toComposeRole()?.let { this.role = it }
                                // Set heading
                                if (node.heading) { this.heading() }
                                // Set toggleable state for checkboxes and switches
                                if (node.role == Role.CHECK_BOX || node.role == Role.SWITCH) {
                                    this.toggleableState = if (node.checked) ToggleableState.On else ToggleableState.Off
                                }
                                this.customActions = listOf(
                                    CustomAccessibilityAction("Редактировать") {
                                        viewModel.onEditNode(node)
                                        true
                                    },
                                    CustomAccessibilityAction("Удалить") {
                                        viewModel.removeNode(node)
                                        true
                                    }
                                ).toMutableList().apply {
                                    val currentIndex = snapNodes.indexOf(node)
                                    if (currentIndex > 0) {
                                        add(CustomAccessibilityAction("Переместить вверх") {
                                            viewModel.moveNodeUp(node)
                                            true
                                        })
                                    }
                                    if (currentIndex < snapNodes.size - 1) {
                                        add(CustomAccessibilityAction("Переместить вниз") {
                                            viewModel.moveNodeDown(node)
                                            true
                                        })
                                    }
                                    if (currentIndex > 0) {
                                        add(CustomAccessibilityAction("Объединить с предыдущим") {
                                            viewModel.mergeWithPrevious(node)
                                            true
                                        })
                                    }
                                    if (currentIndex < snapNodes.size - 1) {
                                        add(CustomAccessibilityAction("Объединить со следующим") {
                                            viewModel.mergeWithNext(node)
                                            true
                                        })
                                    }
                                }
                            }
                    ) {
                        Text(text = "Text: ${node.text}")
                        Text(text = "Role: ${node.role}")
                        Text(text = "Content Description: ${node.text}")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { 
            Log.d("SnapshotReviewScreen", "Send Report button clicked")
            coroutineScope.launch { viewModel.sendReport() } 
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Send Report")
        }
    }
}

fun Role.toComposeRole(): androidx.compose.ui.semantics.Role? {
    return when (this) {
        Role.BUTTON -> androidx.compose.ui.semantics.Role.Button
        Role.CHECK_BOX -> androidx.compose.ui.semantics.Role.Checkbox
        Role.DROP_DOWN_LIST -> androidx.compose.ui.semantics.Role.DropdownList
        Role.IMAGE_BUTTON -> androidx.compose.ui.semantics.Role.Button
        Role.IMAGE -> androidx.compose.ui.semantics.Role.Image
        Role.SWITCH -> androidx.compose.ui.semantics.Role.Switch
        Role.RADIO_BUTTON -> androidx.compose.ui.semantics.Role.RadioButton
        else -> null // No direct mapping, so no role is set
    }
}