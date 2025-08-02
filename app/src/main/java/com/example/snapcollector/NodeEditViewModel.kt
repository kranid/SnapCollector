
package com.example.snapcollector

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NodeEditViewModel : ViewModel() {

    private var originalNode: SnapNode? = null
    private val _editableNode = MutableStateFlow<SnapNode?>(null)
    val editableNode = _editableNode.asStateFlow()

    private val _changes = MutableStateFlow<MutableList<SnapChange>>(mutableListOf())
    val changes: List<SnapChange> = _changes.asStateFlow().value

    fun loadNode(node: SnapNode) {
        originalNode = node
        _editableNode.value = node.copy()
    }

    fun saveChanges(snapshotReviewViewModel: SnapshotReviewViewModel) {
        val original = originalNode
        val edited = _editableNode.value

        if (original != null && edited != null) {
            snapshotReviewViewModel.addChanges(_changes.value)
            snapshotReviewViewModel.updateNode(original, edited)
        }
        clearChanges()
    }

    fun clearChanges() {
        _changes.value.clear()
    }

    fun updateText(text: String) {
        val oldText = _editableNode.value?.text
        if (oldText != text) {
            _changes.value.add(SnapChange(ChangeType.PROPERTY_CHANGE, "text", oldText, text))
        }
        _editableNode.value = _editableNode.value?.copy(text = text)
    }

    fun updateHint(hint: String) {
        val oldHint = _editableNode.value?.hint
        if (oldHint != hint) {
            _changes.value.add(SnapChange(ChangeType.PROPERTY_CHANGE, "hint", oldHint, hint))
        }
        _editableNode.value = _editableNode.value?.copy(hint = hint)
    }

    fun updateRole(role: Role) {
        val oldRole = _editableNode.value?.role
        if (oldRole != role) {
            _changes.value.add(SnapChange(ChangeType.PROPERTY_CHANGE, "role", oldRole?.name, role.name))
        }
        _editableNode.value = _editableNode.value?.copy(role = role)
    }

    fun updateRoleDescription(description: String) {
        val oldDescription = _editableNode.value?.roleDescription
        if (oldDescription != description) {
            _changes.value.add(SnapChange(ChangeType.PROPERTY_CHANGE, "roleDescription", oldDescription, description))
        }
        _editableNode.value = _editableNode.value?.copy(roleDescription = description)
    }

    fun updateStateDescription(description: String) {
        val oldDescription = _editableNode.value?.stateDescription
        if (oldDescription != description) {
            _changes.value.add(SnapChange(ChangeType.PROPERTY_CHANGE, "stateDescription", oldDescription, description))
        }
        _editableNode.value = _editableNode.value?.copy(stateDescription = description)
    }

    fun updateActionable(isActionable: Boolean) {
        val oldActionable = _editableNode.value?.actionable
        if (oldActionable != isActionable) {
            _changes.value.add(SnapChange(ChangeType.PROPERTY_CHANGE, "actionable", oldActionable.toString(), isActionable.toString()))
        }
        _editableNode.value = _editableNode.value?.copy(actionable = isActionable)
    }

    fun updateHeading(isHeading: Boolean) {
        val oldHeading = _editableNode.value?.heading
        if (oldHeading != isHeading) {
            _changes.value.add(SnapChange(ChangeType.PROPERTY_CHANGE, "heading", oldHeading.toString(), isHeading.toString()))
        }
        _editableNode.value = _editableNode.value?.copy(heading = isHeading)
    }

    fun updateChecked(isChecked: Boolean) {
        val oldChecked = _editableNode.value?.checked
        if (oldChecked != isChecked) {
            _changes.value.add(SnapChange(ChangeType.PROPERTY_CHANGE, "checked", oldChecked.toString(), isChecked.toString()))
        }
        _editableNode.value = _editableNode.value?.copy(checked = isChecked)
    }

    fun updateSelected(isSelected: Boolean) {
        val oldSelected = _editableNode.value?.selected
        if (oldSelected != isSelected) {
            _changes.value.add(SnapChange(ChangeType.PROPERTY_CHANGE, "selected", oldSelected.toString(), isSelected.toString()))
        }
        _editableNode.value = _editableNode.value?.copy(selected = isSelected)
    }

    fun updateMin(min: Float) {
        val oldMin = _editableNode.value?.range?.min
        if (oldMin != min) {
            _changes.value.add(SnapChange(ChangeType.PROPERTY_CHANGE, "range.min", oldMin?.toString(), min.toString()))
        }
        _editableNode.value = _editableNode.value?.copy(
            range = _editableNode.value?.range?.copy(min = min) ?: SnapRange(min = min, max = 0f, current = 0f)
        )
    }

    fun updateMax(max: Float) {
        val oldMax = _editableNode.value?.range?.max
        if (oldMax != max) {
            _changes.value.add(SnapChange(ChangeType.PROPERTY_CHANGE, "range.max", oldMax?.toString(), max.toString()))
        }
        _editableNode.value = _editableNode.value?.copy(
            range = _editableNode.value?.range?.copy(max = max) ?: SnapRange(min = 0f, max = max, current = 0f)
        )
    }

    fun updateCurrent(current: Float) {
        val oldCurrent = _editableNode.value?.range?.current
        if (oldCurrent != current) {
            _changes.value.add(SnapChange(ChangeType.PROPERTY_CHANGE, "range.current", oldCurrent?.toString(), current.toString()))
        }
        _editableNode.value = _editableNode.value?.copy(
            range = _editableNode.value?.range?.copy(current = current) ?: SnapRange(min = 0f, max = 0f, current = current)
        )
    }

    fun clear() {
        _editableNode.value = null
    }
}
