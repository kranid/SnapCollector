
package com.example.snapcollector

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.full.memberProperties

class NodeEditViewModel : ViewModel() {

    private var originalNode: SnapNode? = null
    private var originalIndex: Int? = null
    private val _editableNode = MutableStateFlow<SnapNode?>(null)
    val editableNode = _editableNode.asStateFlow()

    private val _changes = MutableStateFlow<MutableMap<String, SnapChange>>(mutableMapOf())
    val changes: List<SnapChange>
        get() = _changes.value.values.toList()

    fun loadNode(nodeToEdit: SnapNode, originalNodeForChanges: SnapNode, index: Int) {
        this.originalNode = originalNodeForChanges
        originalIndex = index
        _editableNode.value = nodeToEdit.copy()
        clearChanges()
    }

    fun saveChanges(snapshotReviewViewModel: SnapshotReviewViewModel) {
        val edited = _editableNode.value

        if (edited != null && originalIndex != null) {
            snapshotReviewViewModel.addChanges(changes)
            snapshotReviewViewModel.updateNode(originalIndex!!, edited)
        }
        clearChanges()
    }

    fun clearChanges() {
        _changes.value = mutableMapOf()
    }

    private fun updateProperty(propertyName: String, newValue: Any?) {
        val node = originalNode ?: return
        val property = node::class.memberProperties.find { it.name == propertyName }
        val oldValue = property?.call(node)

        val currentChanges = _changes.value.toMutableMap()

        if (oldValue?.toString() == newValue?.toString()) {
            currentChanges.remove(propertyName)
            _changes.value = currentChanges
            return
        }

        val existingChange = currentChanges[propertyName]

        if (existingChange == null) {
            currentChanges[propertyName] = SnapChange(
                type = ChangeType.PROPERTY_CHANGE,
                propertyName = propertyName,
                oldValue = oldValue?.toString(),
                newValue = newValue?.toString(),
                nodeRepresentation = node,
                oldIndex = originalIndex
            )
        } else {
            if (existingChange.oldValue == newValue?.toString()) {
                currentChanges.remove(propertyName)
            } else {
                currentChanges[propertyName] = existingChange.copy(newValue = newValue?.toString())
            }
        }
        _changes.value = currentChanges
    }

    fun updateText(text: String) {
        updateProperty("text", text)
        _editableNode.value = _editableNode.value?.copy(text = text)
    }

    fun updateHint(hint: String) {
        updateProperty("hint", hint)
        _editableNode.value = _editableNode.value?.copy(hint = hint)
    }

    fun updateRole(role: Role) {
        updateProperty("role", role)
        _editableNode.value = _editableNode.value?.copy(role = role)
    }

    fun updateRoleDescription(description: String) {
        updateProperty("roleDescription", description)
        _editableNode.value = _editableNode.value?.copy(roleDescription = description)
    }

    fun updateStateDescription(description: String) {
        updateProperty("stateDescription", description)
        _editableNode.value = _editableNode.value?.copy(stateDescription = description)
    }

    fun updateActionable(isActionable: Boolean) {
        updateProperty("actionable", isActionable)
        _editableNode.value = _editableNode.value?.copy(actionable = isActionable)
    }

    fun updateHeading(isHeading: Boolean) {
        updateProperty("heading", isHeading)
        _editableNode.value = _editableNode.value?.copy(heading = isHeading)
    }

    fun updateChecked(isChecked: Boolean) {
        updateProperty("checked", isChecked)
        _editableNode.value = _editableNode.value?.copy(checked = isChecked)
    }

    fun updateSelected(isSelected: Boolean) {
        updateProperty("selected", isSelected)
        _editableNode.value = _editableNode.value?.copy(selected = isSelected)
    }

    fun updateEnabled(isEnabled: Boolean) {
        updateProperty("enabled", isEnabled)
        _editableNode.value = _editableNode.value?.copy(enabled = isEnabled)
    }

    fun updateMin(min: Float) {
        updateProperty("range.min", min)
        _editableNode.value = _editableNode.value?.copy(
            range = _editableNode.value?.range?.copy(min = min) ?: SnapRange(min = min, max = 0f, current = 0f)
        )
    }

    fun updateMax(max: Float) {
        updateProperty("range.max", max)
        _editableNode.value = _editableNode.value?.copy(
            range = _editableNode.value?.range?.copy(max = max) ?: SnapRange(min = 0f, max = max, current = 0f)
        )
    }

    fun updateCurrent(current: Float) {
        updateProperty("range.current", current)
        _editableNode.value = _editableNode.value?.copy(
            range = _editableNode.value?.range?.copy(current = current) ?: SnapRange(min = 0f, max = 0f, current = current)
        )
    }

    fun clear() {
        _editableNode.value = null
        originalNode = null
        originalIndex = null
        clearChanges()
    }
}
