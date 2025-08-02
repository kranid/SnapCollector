
package com.example.snapcollector

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SnapshotReviewViewModel(private val overlayViewModel: OverlayViewModel, private val nodeEditViewModel: NodeEditViewModel, private val snapHubClient: SnapHubClient) : ViewModel() {
    private val _snapNodes = MutableStateFlow<List<SnapNode>>(emptyList())
    val snapNodes = _snapNodes.asStateFlow()

    private val _originalSnapNodes = MutableStateFlow<List<SnapNode>>(emptyList())
    private val _screenshot = MutableStateFlow<ByteArray?>(null)

    private val _changes = MutableStateFlow<List<SnapChange>>(emptyList())
    val changes = _changes.asStateFlow()

    private val _isVisible = MutableStateFlow(false)
    val isVisible = _isVisible.asStateFlow()

    fun setSnapNodes(nodes: List<SnapNode>, screenshot: ByteArray) {
        _originalSnapNodes.value = nodes
        _snapNodes.value = nodes
        _screenshot.value = screenshot
        _isVisible.value = true
    }

    fun hideSnapshotReview() {
        _isVisible.value = false
    }

    fun onEditNode(node: SnapNode) {
        nodeEditViewModel.loadNode(node)
        overlayViewModel.showNodeEditScreen()
    }

    fun updateNode(originalNode: SnapNode, updatedNode: SnapNode) {
        val currentNodes = _snapNodes.value.toMutableList()
        val index = currentNodes.indexOf(originalNode)
        if (index != -1) {
            currentNodes[index] = updatedNode
            _snapNodes.value = currentNodes
        }
    }

    fun removeNode(node: SnapNode) {
        val currentNodes = _snapNodes.value.toMutableList()
        val index = currentNodes.indexOf(node)
        if (index != -1) {
            currentNodes.removeAt(index)
            _snapNodes.value = currentNodes
            _changes.value = _changes.value + SnapChange(ChangeType.REMOVE, "node", null, null, node, index, null)
        }
    }

    fun moveNodeUp(node: SnapNode) {
        val currentNodes = _snapNodes.value.toMutableList()
        val index = currentNodes.indexOf(node)
        if (index > 0) {
            val oldIndex = index
            val newIndex = index - 1
            currentNodes.removeAt(index)
            currentNodes.add(newIndex, node)
            _snapNodes.value = currentNodes
            _changes.value = _changes.value + SnapChange(ChangeType.REORDER, "node", null, null, node, oldIndex, newIndex)
        }
    }

    fun moveNodeDown(node: SnapNode) {
        val currentNodes = _snapNodes.value.toMutableList()
        val index = currentNodes.indexOf(node)
        if (index < currentNodes.size - 1) {
            val oldIndex = index
            val newIndex = index + 1
            currentNodes.removeAt(index)
            currentNodes.add(newIndex, node)
            _snapNodes.value = currentNodes
            _changes.value = _changes.value + SnapChange(ChangeType.REORDER, "node", null, null, node, oldIndex, newIndex)
        }
    }

    fun addChanges(newChanges: List<SnapChange>) {
        _changes.value = _changes.value + newChanges
    }

    fun generateHumanReadableIssues(): List<SnapIssue> {
        val issues = mutableListOf<SnapIssue>()
        _changes.value.forEach { change ->
            val message: String
            val rect = change.nodeRepresentation?.rect ?: SnapRect()
            when (change.type) {
                ChangeType.PROPERTY_CHANGE -> {
                    message = "Property '${change.path}' changed from '${change.oldValue}' to '${change.newValue}'"
                }
                ChangeType.ADD -> {
                    message = "Element added at path '${change.path}'"
                }
                ChangeType.REMOVE -> {
                    message = "Element removed from path '${change.path}'"
                }
                ChangeType.REORDER -> {
                    message = "Element reordered at path '${change.path}' from index ${change.oldIndex} to ${change.newIndex}"
                }
            }
            issues.add(SnapIssue(message, rect))
        }
        return issues
    }

    suspend fun sendReport() {
        val screenshot = _screenshot.value
        val originalNodes = _originalSnapNodes.value
        val editedNodes = _snapNodes.value
        val technicalChanges = _changes.value
        val humanReadableIssues = generateHumanReadableIssues()

        if (screenshot != null) {
            snapHubClient.saveData(
                screenshot,
                originalNodes,
                editedNodes,
                technicalChanges,
                humanReadableIssues
            )
        }
    }
}
