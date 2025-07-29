package com.example.snapcollector

import android.graphics.Rect
import android.os.Build
import android.text.TextUtils
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityWindowInfoCompat
import androidx.core.text.trimmedLength
import android.util.Log

object AccessibilityNodeInfoUtils {

    private const val TAG = "SnapCollector.AccessibilityNodeInfoUtils"


    fun shouldFocusNode(node: AccessibilityNodeInfoCompat?): Boolean {
        return shouldFocusNode(node, null, true)
    }

    fun shouldFocusNode(
        node: AccessibilityNodeInfoCompat?,
        speakingNodesCache: MutableMap<AccessibilityNodeInfoCompat, Boolean>?
    ): Boolean {
        return shouldFocusNode(node, speakingNodesCache, true)
    }

    fun shouldFocusNode(
        node: AccessibilityNodeInfoCompat?,
        speakingNodesCache: MutableMap<AccessibilityNodeInfoCompat, Boolean>?,
        checkChildren: Boolean
    ): Boolean {
        if (node == null) {
            return false
        }
        // Inside views that support web navigation, we delegate focus to the view itself and
        // assume that it navigates to and focuses the correct elements.


        if (!isVisible(node)) {

            return false
        }

        if (isPictureInPicture(node)) {
            // For picture-in-picture, allow focusing the root node, and any app controls inside the
            // pic-in-pic window.
            return true
        } else {
            // Reject all non-leaf nodes that are neither actionable nor focusable, and have the same
            // bounds as the window.
            if (areBoundsIdenticalToWindow(node) && node.childCount > 0 && !isFocusableOrClickable(
                    node
                )
            ) {

                return false
            }
        }

        val visitedNodes = HashSet<AccessibilityNodeInfoCompat>()
        // This checks if a node is clickable, focusable, screen reader focusable, or a direct
        // spekaing child of a scrollable container.
        val accessibilityFocusable = (
                isFocusableOrClickable(node)
                        || (isTopLevelScrollItem(node) && isSpeakingNode(node, null, visitedNodes)))

        if (!checkChildren) {
            // End of the line. Don't check children and don't allow any recursion.
            // checkChildren is only false in the shouldFocusNode call below. This is to avoid
            // repetitive checks down the tree when looking up at the ancestors.
            return accessibilityFocusable
        }

        // A node that is deemed accessibility focusable shouldn't actually get focus if it has
        // nothing to speak. For example, a view may be focusable, but if it has no text and all of
        // its children are clickable, focus should go on each child individually and not on this
        // view.
        // Note: This is redundant for nodes that pass isSpeakingNode above
        // Note: A special case exists for unlabeled buttons which otherwise wouldn't get focus.
        if (accessibilityFocusable) {
            visitedNodes.clear()
            // TODO: This may still result in focusing non-speaking nodes, but it
            // won't prevent unlabeled buttons from receiving focus.
            return if (!canKeepSearchChildren(node)) {

                true
            } else if (isSpeakingNode(node, speakingNodesCache, visitedNodes)) {

                true
            } else {

                false
            }
        }

        // At this point, the node is an unfocusable target.
        // If it has no focusable ancestors, but it still has text, then it should receive focus and be
        // read aloud.
        val filter: (AccessibilityNodeInfoCompat) -> Boolean = { n ->
            shouldFocusNode(n, speakingNodesCache, false)
        }

        return !hasMatchingAncestor(
            node, filter
        ) && (hasText(node) || hasStateDescription(node))
    }


    private fun canKeepSearchChildren(node: AccessibilityNodeInfoCompat): Boolean {
        return hasVisibleChildren(node)
    }

    fun isPictureInPicture(node: AccessibilityNodeInfoCompat): Boolean {
        return isPictureInPicture(node.unwrap())
    }

    fun isPictureInPicture(node: AccessibilityNodeInfo?): Boolean {
        return node != null && AccessibilityWindowInfoUtils.isPictureInPicture(getWindow(node))
    }

    fun getWindow(
        node: AccessibilityNodeInfoCompat?
    ): AccessibilityWindowInfoCompat? {
        // This implementation is redundant with getWindow(AccessibilityNodeInfo) because there are no
        // un/wrap() functions for AccessibilityWindowInfoCompat.

        if (node == null) {
            return null
        }

        return try {
            node.window
        } catch (e: SecurityException) {
            // any log
            null
        }
    }

    private fun areBoundsIdenticalToWindow(node: AccessibilityNodeInfoCompat?): Boolean {
        if (node == null) {
            return false
        }

        val window = getWindow(node) ?: return false

        val windowBounds = Rect()
        window.getBoundsInScreen(windowBounds)

        val nodeBounds = Rect()
        node.getBoundsInScreen(nodeBounds)

        return windowBounds == nodeBounds
    }

    fun getWindow(node: AccessibilityNodeInfo?): AccessibilityWindowInfo? {
        if (node == null) {
            return null
        }

        return try {
            node.window
        } catch (e: SecurityException) {
            // any log
            null
        }
    }

    private val CLASS_TOUCHWIZ_TWADAPTERVIEW: Class<*>? =
        getClass("com.sec.android.touchwiz.widget.TwAdapterView")


    fun isAccessibilityFocusable(node: AccessibilityNodeInfoCompat): Boolean {
        return isFocusableOrClickable(node) || isTopLevelScrollItem(node) && isSpeakingNode(
            node,
            null,
            mutableSetOf()
        )
    }


    private fun isSpeakingNode(
        node: AccessibilityNodeInfoCompat,
        speakingNodeCache: MutableMap<AccessibilityNodeInfoCompat, Boolean>?,
        visitedNodes: MutableSet<AccessibilityNodeInfoCompat>
    ): Boolean {
        if (speakingNodeCache != null && speakingNodeCache.containsKey(node)) {
            return speakingNodeCache[node]!!
        }
        var result = false
        if (hasText(node)) {
            result = true
        } else if (hasStateDescription(node)) {
            result = true
        } else if (node.isCheckable) { // Special case for check boxes.
            result = true
        } else if (hasNonActionableSpeakingChildren(
                node,
                speakingNodeCache, visitedNodes
            )
        ) {
            // Special case for containers with non-focusable content. In this case, the container should
            // speak its non-focusable yet speakable content.
            result = true
        }
        if (speakingNodeCache != null) {
            speakingNodeCache[node] = result
        }
        return result
    }

    private fun isClickable(node: AccessibilityNodeInfoCompat?): Boolean {
        return (node != null
                && (node.isClickable
                || supportsAnyAction(
            node,
            AccessibilityNodeInfoCompat.ACTION_CLICK
        )))
    }

    fun isActionableForAccessibility(node: AccessibilityNodeInfoCompat?): Boolean {
        if (node == null) {
            return false
        }

        // Nodes that are clickable are always actionable.
        if (isClickable(node) || isLongClickable(
                node
            )
        ) {
            return true
        }
        return node.isFocusable
    }

    /**
     * Returns if a node is focusable or clickable.
     *
     *
     * This is used in [.shouldFocusNode] and [.isAccessibilityFocusable]
     *
     * @param node the node to check
     * @return `true` if the node is focusable or clickable
     */
    private fun isFocusableOrClickable(node: AccessibilityNodeInfoCompat?): Boolean {
        return (node != null
                && isVisible(node)
                && (node.isScreenReaderFocusable || isActionableForAccessibility(
            node
        )))
    }

    private fun hasNonActionableSpeakingChildren(
        node: AccessibilityNodeInfoCompat,
        speakingNodeCache: MutableMap<AccessibilityNodeInfoCompat, Boolean>?,
        visitedNodes: MutableSet<AccessibilityNodeInfoCompat>
    ): Boolean {
        val childCount = node.childCount
        var child: AccessibilityNodeInfoCompat?
        for (i in 0 until childCount) {
            child = node.getChild(i)
            if (child == null) {
                continue
            }
            if (!visitedNodes.add(child)) {
                return false
            }

            // Ignore invisible nodes.
            if (!isVisible(child)) {
                continue
            }

            // Ignore focusable nodes
            if (isFocusableOrClickable(child)) {
                continue
            }

            // Ignore top level scroll items that 1) are speaking and 2) have non-clickable parents. This
            // means that a scrollable container that is clickable should get focus before its children.
            if (isTopLevelScrollItem(child) && isSpeakingNode(
                    child,
                    speakingNodeCache,
                    visitedNodes
                )
                && !(isClickable(node) || isLongClickable(
                    node
                ))
            ) {
                continue
            }

            // Recursively check non-focusable child nodes.
            if (isSpeakingNode(child, speakingNodeCache, visitedNodes)) {
                return true
            }
        }

        return false
    }

    private fun hasText(node: AccessibilityNodeInfoCompat?): Boolean {
        return node != null && node.collectionInfo == null && (!TextUtils.isEmpty(
            getText(node)
        )
                || !TextUtils.isEmpty(node.contentDescription)
                || !TextUtils.isEmpty(node.hintText))
    }

    private fun hasStateDescription(node: AccessibilityNodeInfoCompat?): Boolean {
        return (node != null
                && (!TextUtils.isEmpty(node.stateDescription)
                || node.isCheckable
                || hasValidRangeInfo(node)))
    }

    fun getAccessibilityFocusableElements(rootNode: AccessibilityNodeInfoCompat): List<AccessibilityNodeInfoCompat> {
        val focusableElements = mutableListOf<AccessibilityNodeInfoCompat>()
        traverseAccessibilityTree(rootNode, focusableElements)
        return focusableElements
    }

    private fun traverseAccessibilityTree(
        node: AccessibilityNodeInfoCompat,
        focusableElements: MutableList<AccessibilityNodeInfoCompat>
    ) {
        if (isAccessibilityFocusable(node)) {
            focusableElements.add(node)
        }
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                traverseAccessibilityTree(childNode, focusableElements)
            }
        }
    }

    internal fun isVisible(node: AccessibilityNodeInfoCompat): Boolean {
        // We need to move focus to invisible node in WebView to scroll it but we don't want to
        // move focus if WebView itself is invisible.
        return node.isVisibleToUser

    }

    private fun isTopLevelScrollItem(node: AccessibilityNodeInfoCompat?): Boolean {
        if (node == null) {
            return false
        }
        if (!isVisible(node)) {
            return false
        }
        val parent = node.parent
            ?: // Not a child node of anything.
            return false

        // Drop down lists (spinners) are not included to retain the old behavior of focusing on
        // the spinner itself rather than on the single visible item.
        // A spinner being scrollable is disingenuous since the scrollable list inside isn't exposed
        // without interaction.
        // TODO: Remove this check?
        if (RoleUtils.getRole(parent) == Role.DROP_DOWN_LIST) {
            return false
        }

        // A node with a scrollable parent is a top level scroll item.
        if (isScrollable(parent)) {
            return true
        }
        val parentRole = RoleUtils.getRole(parent)
        // Note that ROLE_DROP_DOWN_LIST(Spinner) is not accepted.
        // RecyclerView is classified as a list or grid based on its CollectionInfo.
        // These parents may not be scrollable in some cases, like if the list is too short to be
        // scrolled, but their children should still be considered top level scroll items.
        return parentRole == Role.LIST || parentRole == Role.GRID || parentRole == Role.SCROLL_VIEW || parentRole == Role.HORIZONTAL_SCROLL_VIEW || nodeMatchesAnyClassByType(
            parent,
            CLASS_TOUCHWIZ_TWADAPTERVIEW
        )
    }

    private fun isLongClickable(node: AccessibilityNodeInfoCompat?): Boolean {
        return (node != null
                && (node.isLongClickable
                || supportsAnyAction(
            node,
            AccessibilityNodeInfoCompat.ACTION_LONG_CLICK
        )))
    }

    fun supportsAnyAction(
        node: AccessibilityNodeInfoCompat?,
        vararg actions: AccessibilityActionCompat
    ): Boolean {
        if (node == null) {
            return false
        }
        // Unwrap the node and compare AccessibilityActions because AccessibilityActions, unlike
        // AccessibilityActionCompats, are static (so checks for equality work correctly).
        val supportedActions = node.actionList
        for (action in actions) {
            if (supportedActions.contains(action)) {
                return true
            }
        }
        return false
    }


    private fun supportsAnyAction(
        node: AccessibilityNodeInfoCompat?,
        vararg actions: Int
    ): Boolean {
        if (node != null) {
            val supportedActions = node.actions
            for (action in actions) {
                if (supportedActions and action == action) {
                    return true
                }
            }
        }
        return false
    }

    private fun getText(node: AccessibilityNodeInfoCompat) = node.text

    fun supportsAction(node: AccessibilityNodeInfoCompat, action: Int): Boolean {
        // New actions in >= API 21 won't appear in getActions() but in getActionList().
        // On Lollipop+ devices, pre-API 21 actions will also appear in getActionList().
        val actions = node.actionList
        val size = actions.size
        for (i in 0 until size) {
            val actionCompat = actions[i]
            if (actionCompat.id == action) {
                return true
            }
        }
        return false
    }

    fun hasValidRangeInfo(node: AccessibilityNodeInfoCompat?): Boolean {
        if (node == null) {
            return false
        }
        val rangeInfo = node.rangeInfo ?: return false
        val maxProgress = rangeInfo.max
        val minProgress = rangeInfo.min
        val currentProgress = rangeInfo.current
        val diffProgress = maxProgress - minProgress
        return diffProgress > 0.0f && currentProgress >= minProgress && currentProgress <= maxProgress
    }

    fun toCompat(
        nodeInfo: AccessibilityNodeInfo
    ): AccessibilityNodeInfoCompat {
        return AccessibilityNodeInfoCompat.wrap(nodeInfo)
    }

    fun isScrollable(node: AccessibilityNodeInfoCompat?): Boolean {
        return supportsAnyAction(
            node,
            AccessibilityActionCompat.ACTION_SCROLL_FORWARD,
            AccessibilityActionCompat.ACTION_SCROLL_BACKWARD,
            AccessibilityActionCompat.ACTION_SCROLL_DOWN,
            AccessibilityActionCompat.ACTION_SCROLL_UP,
            AccessibilityActionCompat.ACTION_SCROLL_RIGHT,
            AccessibilityActionCompat.ACTION_SCROLL_LEFT
        )
    }

    fun nodeMatchesAnyClassByType(
        node: AccessibilityNodeInfoCompat,
        vararg referenceClasses: Class<*>?
    ): Boolean {

        for (referenceClass in referenceClasses) {
            if (ClassLoadingCache.checkInstanceOf(node.className, referenceClass)) {
                return true
            }
        }

        return false
    }

    fun getClass(className: String): Class<*>? {
        if (className.isEmpty()) {
            return null
        }

        return try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()

            null
        }
    }

    private fun hasVisibleChildren(node: AccessibilityNodeInfoCompat): Boolean {
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null && child.isVisibleToUser) {
                return true
            }
        }

        return false
    }

    fun hasMatchingAncestor(
        node: AccessibilityNodeInfoCompat?,
        filter:
            (AccessibilityNodeInfoCompat) -> Boolean
    ): Boolean {
        return (node != null) && (getMatchingAncestor(
            node,
            filter
        ) != null)
    }

    /**
     * Returns the first ancestor of `node` that matches the `filter`. Returns `null` if no nodes match.
     */
    fun getMatchingAncestor(
        node: AccessibilityNodeInfoCompat,
        filter: (AccessibilityNodeInfoCompat) -> Boolean
    ): AccessibilityNodeInfoCompat? {
        return getMatchingAncestor(node, null, filter)
    }

    /**
     * Returns the first ancestor of `node` that matches the `filter`, terminating the
     * search once it reaches `end`. The search is exclusive of both `node` and `end`. Returns `null` if no nodes match.
     */
    private fun getMatchingAncestor(
        sourceNode: AccessibilityNodeInfoCompat?,
        end: AccessibilityNodeInfoCompat?,
        filter: (AccessibilityNodeInfoCompat) -> Boolean
    ): AccessibilityNodeInfoCompat? {
        if (sourceNode == null) {
            return null
        }
        var node = sourceNode
        val ancestors = java.util.HashSet<AccessibilityNodeInfoCompat?>()

        ancestors.add(node)
        node = node.parent

        while (node != null) {
            if (!ancestors.add(node)) {
                // Already seen this node, so abort!
                return null
            }

            if (end != null && node == end) {
                // Reached the end node, so abort!
                return null
            }

            if (filter(node)) {
                return node
            }

            node = node.parent
        }

        return null
    }

    fun hasRequestInitialAccessibilityFocus(node: AccessibilityNodeInfoCompat): Boolean {
        var hasRequestInitialAccessibilityFocus: Boolean =
            node.hasRequestInitialAccessibilityFocus()
        // In the early version of AndroidX, the property was retrieved from the AccessibilityNodeInfo.
        // See b/279108748 for details.
        if (!hasRequestInitialAccessibilityFocus && BuildVersionUtils.isAtLeastU) {
            val unwrap = node.unwrap()
            if (unwrap != null) {
                hasRequestInitialAccessibilityFocus = unwrap.hasRequestInitialAccessibilityFocus()
            }
        }
        return hasRequestInitialAccessibilityFocus
    }

    /**
     * Gets the text of a `node` by returning the content description (if available) or by
     * returning the text.
     *
     * @param node The node.
     * @return The node text.
     */
    fun getNodeText(node: AccessibilityNodeInfoCompat?): CharSequence? {
        if (node == null) {
            return null
        }

        // Prefer content description over text.
        // TODO: Why are we checking the trimmed length?
        val contentDescription = node.contentDescription
        if (!TextUtils.isEmpty(contentDescription)
            && (contentDescription.trimmedLength() > 0)
        ) {
            return contentDescription
        }

        val text = getText(node)
        if (!TextUtils.isEmpty(text) && (text.trimmedLength() > 0)) {
            return text
        }

        return null
    }

    /** Returns the root node of the tree containing `node`.  */
    fun getRoot(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
        val window = getWindow(node)
        if (window != null) {
            val  root =AccessibilityWindowInfoUtils.getRoot(window)
            if (root != null) {
                return root
            }
        }

        val visitedNodes: MutableSet<AccessibilityNodeInfoCompat> = java.util.HashSet()
        var current: AccessibilityNodeInfoCompat? = null
        var parent = node

        do {
            if (current != null) {
                if (visitedNodes.contains(current)) {
                    return null
                }
                visitedNodes.add(current)
            }

            current = parent
            parent = current!!.parent
        } while (parent != null)

        return current
    }

    private const val SYSTEM_ACTION_MAX = 0x01FFFFFF

    fun isCustomAction(action: AccessibilityActionCompat): Boolean {
        return action.id > SYSTEM_ACTION_MAX
    }

    private const val TITLE_ACTION_DISMISS = "Dismiss"
    private const val TITLE_ACTION_EXPAND = "Expand"
    private const val TITLE_ACTION_COLLAPSE = "Collapse"
    

    

    fun getCustomActions(node: AccessibilityNodeInfoCompat): List<String> {
        val customActions = mutableListOf<String>()
        populateCustomMenuItemsForNode(node, customActions, true, mutableSetOf())
        return customActions
    }

    private fun populateCustomMenuItemsForNode(
        node: AccessibilityNodeInfoCompat?,
        menu: MutableList<String>,
        includeAncestors: Boolean,
        visitedNodes: MutableSet<AccessibilityNodeInfoCompat>
    ) {
        if (node == null) {
            return
        }
        if (visitedNodes.contains(node)) {
            Log.w(TAG, "View node tree contains a loop.")
            return
        }
        visitedNodes.add(node)

        for (action in node.actionList) {
            var label: CharSequence = ""
            val id = action.id

            if (isCustomAction(action)) {
                label = action.label ?: ""
            } else if (id == AccessibilityNodeInfoCompat.ACTION_DISMISS) {
                label = TITLE_ACTION_DISMISS
            } else if (id == AccessibilityNodeInfoCompat.ACTION_EXPAND) {
                label = TITLE_ACTION_EXPAND
            } else if (id == AccessibilityNodeInfoCompat.ACTION_COLLAPSE) {
                label = TITLE_ACTION_COLLAPSE
            }

            Log.d(TAG, "Processing action: id=" + id + ", label=" + label)

            if (label.isNotEmpty()) {
                menu.add(label.toString())
            }
        }

        if (!includeAncestors) {
            return
        }

        if (menu.isEmpty()) {
            populateCustomMenuItemsForNode(
                node.parent, menu, /* includeAncestors= */ true, visitedNodes
            )
        }
    }
}