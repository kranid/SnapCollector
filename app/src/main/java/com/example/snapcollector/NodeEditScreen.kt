package com.example.snapcollector

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.ExposedDropdownMenuDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeEditScreen(
    viewModel: NodeEditViewModel,
    snapshotReviewViewModel: SnapshotReviewViewModel, // Add this
    onClose: () -> Unit
) {
    val nodeState by viewModel.editableNode.collectAsState()
    val roles = Role.entries.toTypedArray()
    var isRoleMenuExpanded by remember { mutableStateOf(false) }

    val node = nodeState

    if (node != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Node") },
                    actions = {
                        Button(onClick = {
                            viewModel.saveChanges(snapshotReviewViewModel)
                            onClose()
                        }) {
                            Text("Save")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onClose) {
                            Text("Close")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Role Dropdown
                // Role Dropdown
            ExposedDropdownMenuBox(
                expanded = isRoleMenuExpanded,
                onExpandedChange = { isRoleMenuExpanded = !isRoleMenuExpanded },
                modifier = Modifier.semantics { contentDescription = "Role dropdown menu" }
            ) {
                OutlinedTextField(
                    value = node.role.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Role") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .semantics { role = SemanticsRole.DropdownList }
                )
                ExposedDropdownMenu(
                    expanded = isRoleMenuExpanded,
                    onDismissRequest = { isRoleMenuExpanded = false }
                ) {
                    roles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.name) },
                            onClick = {
                                viewModel.updateRole(role)
                                isRoleMenuExpanded = false
                            }
                        )
                    }
                }
            }
                Spacer(modifier = Modifier.height(16.dp))

                // String fields
                OutlinedTextField(
                    value = node.text,
                    onValueChange = { viewModel.updateText(it) },
                    label = { Text("Text") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = node.hint,
                    onValueChange = { viewModel.updateHint(it) },
                    label = { Text("Hint") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = node.roleDescription,
                    onValueChange = { viewModel.updateRoleDescription(it) },
                    label = { Text("Role Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = node.stateDescription,
                    onValueChange = { viewModel.updateStateDescription(it) },
                    label = { Text("State Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Boolean fields
                CheckboxWithLabel(
                    label = "Actionable",
                    checked = node.actionable,
                    onCheckedChange = { newValue -> viewModel.updateActionable(newValue) }
                )
                CheckboxWithLabel(
                    label = "Heading",
                    checked = node.heading,
                    onCheckedChange = { newValue -> viewModel.updateHeading(newValue) }
                )
                CheckboxWithLabel(
                    label = "Checked",
                    checked = node.checked,
                    onCheckedChange = { newValue -> viewModel.updateChecked(newValue) }
                )
                CheckboxWithLabel(
                    label = "Selected",
                    checked = node.selected,
                    onCheckedChange = { newValue -> viewModel.updateSelected(newValue) }
                )

                // SnapRange fields
                Spacer(modifier = Modifier.height(16.dp))
                Text("Range Properties", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = node.range?.min?.toString() ?: "",
                    onValueChange = { value ->
                        viewModel.updateMin(value.toFloatOrNull() ?: 0f)
                    },
                    label = { Text("Min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = node.range?.max?.toString() ?: "",
                    onValueChange = { value ->
                        viewModel.updateMax(value.toFloatOrNull() ?: 0f)
                    },
                    label = { Text("Max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = node.range?.current?.toString() ?: "",
                    onValueChange = { value ->
                        viewModel.updateCurrent(value.toFloatOrNull() ?: 0f)
                    },
                    label = { Text("Current") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
    @Composable
    fun CheckboxWithLabel(
        label: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    role = SemanticsRole.Checkbox
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Text(label, modifier = Modifier.padding(start = 8.dp))
        }
    }
