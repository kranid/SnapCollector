
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

    fun loadMergedNode(mergedNode: SnapNode, originalNodeForChanges: SnapNode, index: Int) {
        this.originalNode = originalNodeForChanges
        originalIndex = index
        _editableNode.value = mergedNode.copy()
        clearChanges()

        // Register all properties
        updateText(mergedNode.text)
        updateHint(mergedNode.hint)
        updateRole(mergedNode.role)
        updateActionable(mergedNode.actionable)
        updateHeading(mergedNode.heading)
        updateChecked(mergedNode.checked)
        updateSelected(mergedNode.selected)
        updateRoleDescription(mergedNode.roleDescription)
        updateStateDescription(mergedNode.stateDescription)
        mergedNode.range?.let {
            updateMin(it.min)
            updateMax(it.max)
            updateCurrent(it.current)
        }
        mergedNode.rect?.let {
            updateLeft(it.left)
            updateTop(it.top)
            updateRight(it.right)
            updateBottom(it.bottom)
        }
    }

    private fun getNestedProperty(instance: Any?, propertyName: String): Any? {
        if (instance == null) return null
        val properties = propertyName.split('.')
        var current: Any? = instance
        for (prop in properties) {
            if (current == null) return null
            val kClass = current!!::class
            val member = kClass.memberProperties.find { it.name == prop } ?: return null
            current = member.call(current)
        }
        return current
    }

    private fun updateProperty(propertyName: String, newValue: Any?) {
        val node = originalNode ?: return
        val oldValue = getNestedProperty(node, propertyName)

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

    fun updateLeft(left: Int) {
        updateProperty("rect.left", left)
        _editableNode.value = _editableNode.value?.copy(
            rect = _editableNode.value?.rect?.copy(left = left) ?: SnapRect(left = left)
        )
    }

    fun updateTop(top: Int) {
        updateProperty("rect.top", top)
        _editableNode.value = _editableNode.value?.copy(
            rect = _editableNode.value?.rect?.copy(top = top) ?: SnapRect(top = top)
        )
    }

    fun updateRight(right: Int) {
        updateProperty("rect.right", right)
        _editableNode.value = _editableNode.value?.copy(
            rect = _editableNode.value?.rect?.copy(right = right) ?: SnapRect(right = right)
        )
    }

    fun updateBottom(bottom: Int) {
        updateProperty("rect.bottom", bottom)
        _editableNode.value = _editableNode.value?.copy(
            rect = _editableNode.value?.rect?.copy(bottom = bottom) ?: SnapRect(bottom = bottom)
        )
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
