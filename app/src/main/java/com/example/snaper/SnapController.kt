package com.example.snaper

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

class SnapController {

    private val widgetRoles = setOf(
        Role.BUTTON,
        Role.SWITCH,
        Role.CHECKED_TEXT_VIEW,
        Role.CHECK_BOX,
        Role.NUMBER_PICKER,
        Role.RADIO_BUTTON,
        Role.SEEK_CONTROL,
        Role.TOGGLE_BUTTON,
        Role.IMAGE_BUTTON,
        Role.EDIT_TEXT,
        Role.TALKBACK_EDIT_TEXT_OVERLAY,
        Role.TEXT_ENTRY_KEY
    )

    private val containerRoles = setOf(
        Role.LIST,
        Role.DROP_DOWN_LIST,
        Role.GRID,
        Role.HORIZONTAL_SCROLL_VIEW,
        Role.SCROLL_VIEW,
        Role.TAB_BAR,
        Role.ALERT_DIALOG,
        Role.ACTION_BAR_TAB,
        Role.DATE_PICKER_DIALOG,
        Role.DATE_PICKER,
        Role.TIME_PICKER_DIALOG,
        Role.TIME_PICKER,
        Role.PAGER,
        Role.TOAST,
        Role.SLIDING_DRAWER,
        Role.DRAWER_LAYOUT,
    )
    private lateinit var rootWindow: AccessibilityNodeInfoCompat
    fun createSnapTree(root: AccessibilityNodeInfoCompat): List<SnapNode>? {
        rootWindow = root
        val orderController = OrderedTraversalController()
        orderController.setSpeakingNodesCache(mutableMapOf())
        val workingTree = orderController.initOrder(root, false) ?: return null
        return createSnapTree(workingTree)
    }

    private fun createSnapTree(root: WorkingTree): List<SnapNode> {
        fun traverse(tree: WorkingTree): MutableList<SnapNode> {
            val snapNodes = mutableListOf<SnapNode>()
            val node = tree.getNode()
            val snapNode = createSnap(node)
            // val isParent = containerRoles.contains(RoleUtils.getRole(node))
            val isParent = false
            if (AccessibilityNodeInfoUtils.shouldFocusNode(node) || isParent) {
                snapNodes.add(snapNode)
            }
            for (child in tree.children) {
                val childSnapNodes = traverse(child)
                if (isParent) {
                    snapNode.children.addAll(childSnapNodes)
                } else {
                    snapNodes.addAll(childSnapNodes)
                }
            }
            return snapNodes
        }

        return traverse(root)
    }


    private fun createSnap(node: AccessibilityNodeInfoCompat): SnapNode {

        var snap = SnapNode(
            role = RoleUtils.getRole(node),
            text = AccessibilityNodeInfoUtils.getNodeText(node)?.toString() ?: "",
            hint = node.hintText?.toString() ?: "",
            actionable = AccessibilityNodeInfoUtils.isActionableForAccessibility(node),
            roleDescription = node.roleDescription?.toString() ?: "",
            checked = node.isChecked,
            selected = node.isSelected,
            stateDescription = node.stateDescription?.toString() ?: "",
            heading = node.isHeading,
            rect = getRectOfNode(node),
            range = getRange(node),
            actions = AccessibilityNodeInfoUtils.getCustomActions(node)
        )


        val contentDescriptionIsNotEmpty = node.contentDescription?.isNotEmpty() ?: false
        if (contentDescriptionIsNotEmpty) {
            return snap
        }
        for (childIndex in 0 until node.childCount) {
            val childNode = node.getChild(childIndex) ?: continue
            if (AccessibilityNodeInfoUtils.isVisible(childNode)
                    && !AccessibilityNodeInfoUtils.isAccessibilityFocusable(childNode)
                ) {
                    snap = joinSnap(snap, createSnap(childNode))
                }
        }
        return snap
    }


    private fun joinSnap(snap1: SnapNode, snap2: SnapNode): SnapNode {
        val resultSnap = SnapNode(
            text = snap1.text + snap2.text,
            role = if (widgetRoles.contains(snap1.role)) snap1.role else snap2.role,
            actionable = snap1.actionable || snap2.actionable,
            heading = snap1.heading || snap2.heading,
            checked = snap1.checked || snap2.checked,
            selected = snap1.selected || snap2.selected,
            roleDescription = snap1.roleDescription + snap2.roleDescription,
            stateDescription = snap1.stateDescription + snap2.stateDescription,
            rect = snap1.rect,
            range = snap1.range ?: snap2.range,
            actions = snap1.actions.ifEmpty { snap2.actions }
                )
        return resultSnap
    }

    private fun getRectOfNode(node: AccessibilityNodeInfoCompat): SnapRect {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)

        return SnapRect(rect.left, rect.top, rect.right, rect.bottom)
    }


    

    private fun getRange(node: AccessibilityNodeInfoCompat): SnapRange? {
        val rangeInfo = node.rangeInfo ?: return null
        return SnapRange(
            min = rangeInfo.min,
            max = rangeInfo.max,
            current = rangeInfo.current
        )
    }
}
