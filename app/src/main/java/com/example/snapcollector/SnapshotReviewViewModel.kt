package com.example.snapcollector

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.snapcollector.RoleUtils.widgetRoles

data class MergeState(
    val nodeToRemove: SnapNode,
    val nodeToUpdate: SnapNode, // The original node that was merged into
    val originalIndexOfUpdate: Int
)

class SnapshotReviewViewModel(
    private val overlayViewModel: OverlayViewModel,
    private val nodeEditViewModel: NodeEditViewModel,
    private val snapHubClient: SnapHubClient,
    private val getScreenInfo: () -> ScreenInfo
) : ViewModel() {
    private val _mergeState = MutableStateFlow<MergeState?>(null)
    private val _snapNodes = MutableStateFlow<List<SnapNode>>(emptyList())
    val snapNodes = _snapNodes.asStateFlow()

    private val _originalSnapNodes = MutableStateFlow<List<SnapNode>>(emptyList())
    private val _screenshot = MutableStateFlow<ByteArray?>(null)
    private val _screenInfo = MutableStateFlow<ScreenInfo?>(null)

    private val _changes = MutableStateFlow<MutableList<SnapChange>>(mutableListOf())
    private val changes: List<SnapChange>
        get() = _changes.value.toList()

    private val _isVisible = MutableStateFlow(false)
    val isVisible = _isVisible.asStateFlow()

    fun setSnapNodes(nodes: List<SnapNode>, screenshot: ByteArray, screenInfo: ScreenInfo) {
        _originalSnapNodes.value = nodes.map { it.copy() }
        _snapNodes.value = nodes.map { it.copy() }
        _screenshot.value = screenshot
        _screenInfo.value = screenInfo
        _isVisible.value = true
        _changes.value = mutableListOf()
    }

    fun hideSnapshotReview() {
        _isVisible.value = false
    }

    fun onEditNode(node: SnapNode) {
        val currentIndex = _snapNodes.value.indexOf(node)
        if (currentIndex == -1) {
            Log.e("SnapshotReviewViewModel", "Node not found in current list")
            return
        }

        val originalNode = _originalSnapNodes.value[currentIndex]
        val nodeToEdit = _snapNodes.value[currentIndex]

        nodeEditViewModel.loadNode(nodeToEdit, originalNode, currentIndex)
        overlayViewModel.showNodeEditScreen()
    }

    fun updateNode(index: Int, updatedNode: SnapNode) {
        val mergeState = _mergeState.value
        if (mergeState != null && mergeState.originalIndexOfUpdate == index) {
            finalizeMerge(updatedNode)
        } else {
            val currentNodes = _snapNodes.value.toMutableList()
            if (index >= 0 && index < currentNodes.size) {
                currentNodes[index] = updatedNode
                _snapNodes.value = currentNodes
            }
        }
    }

    fun finalizeMerge(updatedNode: SnapNode) {
        val mergeState = _mergeState.value ?: return

        // Update the node that was merged into
        val currentNodes = _snapNodes.value.toMutableList()
        val indexToUpdate = currentNodes.indexOf(mergeState.nodeToUpdate)
        if (indexToUpdate != -1) {
            currentNodes[indexToUpdate] = updatedNode
            _snapNodes.value = currentNodes
        }

        // Remove the other node
        removeNode(mergeState.nodeToRemove)

        // Clear the merge state
        _mergeState.value = null
    }

    fun cancelMerge() {
        _mergeState.value = null
    }

    fun removeNode(node: SnapNode) {
        val currentNodes = _snapNodes.value.toMutableList()
        val index = currentNodes.indexOf(node)
        if (index != -1) {
            currentNodes.removeAt(index)
            _snapNodes.value = currentNodes

            val originalIndex = _originalSnapNodes.value.indexOf(node)

            val currentChanges = _changes.value.toMutableList()
            // Remove any existing changes for this node, as REMOVE takes precedence
            currentChanges.removeAll { it.oldIndex == originalIndex }

            currentChanges.add(
                SnapChange(
                    type = ChangeType.REMOVE,
                    propertyName = null,
                    oldValue = null,
                    newValue = null,
                    nodeRepresentation = node,
                    oldIndex = originalIndex,
                    newIndex = null
                )
            )
            _changes.value = currentChanges
        }
    }

    fun moveNodeUp(node: SnapNode) {
        val currentNodes = _snapNodes.value.toMutableList()
        val index = currentNodes.indexOf(node)
        if (index > 0) {
            val newIndex = index - 1
            currentNodes.removeAt(index)
            currentNodes.add(newIndex, node)
            _snapNodes.value = currentNodes
            updateNodeOrder(node, newIndex)
        }
    }

    fun moveNodeDown(node: SnapNode) {
        val currentNodes = _snapNodes.value.toMutableList()
        val index = currentNodes.indexOf(node)
        if (index < currentNodes.size - 1) {
            val newIndex = index + 1
            currentNodes.removeAt(index)
            currentNodes.add(newIndex, node)
            _snapNodes.value = currentNodes
            updateNodeOrder(node, newIndex)
        }
    }

    fun mergeWithPrevious(node: SnapNode) {
        val index = _snapNodes.value.indexOf(node)
        if (index > 0) {
            val previousNode = _snapNodes.value[index - 1]
            val mergedNode = mergeNodes(previousNode, node)

            val originalIndexOfPrevious = _originalSnapNodes.value.indexOf(previousNode)
            val originalPreviousNode =
                if (originalIndexOfPrevious != -1) _originalSnapNodes.value[originalIndexOfPrevious] else previousNode

            // Set the merge state
            _mergeState.value = MergeState(
                nodeToRemove = node,
                nodeToUpdate = previousNode,
                originalIndexOfUpdate = originalIndexOfPrevious
            )

            // Load the editor with the merged node
            nodeEditViewModel.loadMergedNode(
                mergedNode,
                originalPreviousNode,
                originalIndexOfPrevious
            )
            overlayViewModel.showNodeEditScreen()
        }
    }

    fun mergeWithNext(node: SnapNode) {
        val index = _snapNodes.value.indexOf(node)
        if (index != -1 && index < _snapNodes.value.size - 1) {
            val nextNode = _snapNodes.value[index + 1]
            val mergedNode = mergeNodes(node, nextNode)

            val originalIndexOfNode = _originalSnapNodes.value.indexOf(node)
            val originalNode =
                if (originalIndexOfNode != -1) _originalSnapNodes.value[originalIndexOfNode] else node

            // Set the merge state
            _mergeState.value = MergeState(
                nodeToRemove = nextNode,
                nodeToUpdate = node,
                originalIndexOfUpdate = originalIndexOfNode
            )

            // Load the editor with the merged node
            nodeEditViewModel.loadMergedNode(mergedNode, originalNode, originalIndexOfNode)
            overlayViewModel.showNodeEditScreen()
        }
    }

    private fun mergeNodes(node1: SnapNode, node2: SnapNode): SnapNode {
        val newRect = if (node1.rect != null && node2.rect != null) {
            SnapRect(
                left = minOf(node1.rect.left, node2.rect.left),
                top = minOf(node1.rect.top, node2.rect.top),
                right = maxOf(node1.rect.right, node2.rect.right),
                bottom = maxOf(node1.rect.bottom, node2.rect.bottom)
            )
        } else {
            node1.rect ?: node2.rect
        }

        val newText = if (node1.text == node2.text) node1.text else "${node1.text}${node2.text}"
        val newHint = "${node1.hint}${node2.hint}"
        val newRole =
            if (widgetRoles.contains(node2.role) && !widgetRoles.contains(node1.role)) node2.role else node1.role
        val newRoleDescription =
            node1.roleDescription.ifEmpty { node2.roleDescription }
        val newStateDescription =
            node1.stateDescription.ifEmpty { node2.stateDescription }
        val newActionable = node1.actionable || node2.actionable
        val newHeading = node1.heading || node2.heading
        val newChecked = node1.checked || node2.checked
        val newSelected = node1.selected || node2.selected
        val newRange = node1.range ?: node2.range
        val newActions = node1.actions.ifEmpty { node2.actions }

        return SnapNode(
            text = newText,
            hint = newHint,
            role = newRole,
            roleDescription = newRoleDescription,
            stateDescription = newStateDescription,
            actionable = newActionable,
            heading = newHeading,
            checked = newChecked,
            selected = newSelected,
            rect = newRect,
            range = newRange,
            actions = newActions,
            children = (node1.children + node2.children).toMutableList()
        )
    }

    private fun updateNodeOrder(node: SnapNode, newIndex: Int) {
        val originalIndex = _originalSnapNodes.value.indexOf(node)

        val currentChanges = _changes.value.toMutableList()
        val existingReorderChange =
            currentChanges.find { it.oldIndex == originalIndex && it.type == ChangeType.REORDER }

        if (newIndex == originalIndex) {
            // If the node is moved back to its original position, remove the reorder change
            existingReorderChange?.let {
                currentChanges.remove(it)
            }
        } else {
            if (existingReorderChange != null) {
                // If a reorder change already exists, update its newIndex
                val updatedChange = existingReorderChange.copy(newIndex = newIndex)
                val indexInList = currentChanges.indexOf(existingReorderChange)
                if (indexInList != -1) {
                    currentChanges[indexInList] = updatedChange
                }
            } else {
                // If no reorder change exists, add a new one
                currentChanges.add(
                    SnapChange(
                        type = ChangeType.REORDER,
                        propertyName = null,
                        oldValue = null,
                        newValue = null,
                        nodeRepresentation = node,
                        oldIndex = originalIndex,
                        newIndex = newIndex
                    )
                )
            }
        }
        _changes.value = currentChanges
    }

    fun addChanges(newChanges: List<SnapChange>) {
        val currentChanges = _changes.value.toMutableList()

        newChanges.forEach { newPropChange ->
            val originalIndex = newPropChange.oldIndex!!
            val propertyName = newPropChange.propertyName

            val existingPropChange = currentChanges.find {
                it.type == ChangeType.PROPERTY_CHANGE &&
                        it.oldIndex == originalIndex &&
                        it.propertyName == propertyName
            }

            if (existingPropChange != null) {
                val updatedChange = existingPropChange.copy(newValue = newPropChange.newValue)
                val indexInList = currentChanges.indexOf(existingPropChange)
                if (indexInList != -1) {
                    currentChanges[indexInList] = updatedChange
                }
            } else {
                currentChanges.add(newPropChange)
            }
        }
        _changes.value = currentChanges
    }

    private fun generateHumanReadableIssues(): List<SnapIssue> {
        val issues = mutableListOf<SnapIssue>()
        _changes.value.forEach { change ->
            val message: String
            val rect = change.nodeRepresentation?.rect ?: SnapRect()
            val node = change.nodeRepresentation
            val nodeDescription = node?.let { "with text '${it.text}' and role '${it.role}'" } ?: ""

            when (change.type) {
                ChangeType.PROPERTY_CHANGE -> {
                    val propertyName = change.propertyName
                    message =
                        "Element with index ${change.oldIndex} $nodeDescription property '$propertyName' must be changed to '${change.newValue}'"

                }

                ChangeType.ADD -> {
                    message="Element with index ${change.newIndex} $nodeDescription must be added"

                }

                ChangeType.REMOVE -> {
                    message =
                    "Element with index ${change.oldIndex} $nodeDescription must be removed"
                                    }

                ChangeType.REORDER -> {
                    message =
                    "Element with index ${change.oldIndex} $nodeDescription must be reordered to ${change.newIndex}"
                                    }
            }
            issues.add(SnapIssue(message, rect))
        }
        return issues
    }

    suspend fun sendReport() {
        Log.d("SnapshotReviewViewModel", "sendReport() called")
        val screenshot = _screenshot.value
        val screenInfo = _screenInfo.value
        Log.d("SnapshotReviewViewModel", "Screenshot value: ${screenshot?.size ?: "null"}")

        if (screenshot == null || screenInfo == null) {
            Log.e(
                "SnapshotReviewViewModel",
                "Screenshot or ScreenInfo is null, cannot send report."
            )
            return
        }

        val originalNodes = _originalSnapNodes.value
        val editedNodes = _snapNodes.value
        val technicalChanges = changes
        val humanReadableIssues = generateHumanReadableIssues()
        val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        try {
            snapHubClient.saveData(
                screenshot,
                originalNodes,
                editedNodes,
                technicalChanges,
                humanReadableIssues,
                screenInfo.PackageName,
                screenInfo.Name,
                deviceModel
            )
            Log.d("SnapshotReviewViewModel", "saveData successful.")
            overlayViewModel.showSuccess("Отчет успешно отправлен!")
        } catch (e: Exception) {
            Log.e("SnapshotReviewViewModel", "Error in saveData: ${e.message}", e)
            overlayViewModel.showError("Ошибка при отправке отчета: ${e.message}")
        }
    }
}
