/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.snapcollector

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

class OrderedTraversalController {
    private var speakingNodesCache: MutableMap<AccessibilityNodeInfoCompat, Boolean>? = null

    private var tree: WorkingTree? = null
    private val nodeTreeMap: MutableMap<AccessibilityNodeInfoCompat?, WorkingTree> = LinkedHashMap()

    private var initialFocusNode: AccessibilityNodeInfoCompat? = null

    fun setSpeakingNodesCache(speakingNodesCache: MutableMap<AccessibilityNodeInfoCompat, Boolean>?) {
        this.speakingNodesCache = speakingNodesCache
    }

    /**
     * before start next traversal node search the controller must be initialized. The initialisation
     * step includes traversal through all accessibility nodes hierarchy to collect information about
     * traversal order of separate subtrees and moving subtries that has custom before/after traverse
     * view order
     *
     * @param compatRoot - accessibility node that serves as root node for tree hierarchy the
     * controller works with
     * @param includeChildrenOfNodesWithWebActions whether to calculator order for nodes that support
     * web actions. Although TalkBack uses the naviagation order specified by the nodes, Switch
     * Access needs to know about all nodes at the time the tree is being created.
     */
    fun initOrder(
         compatRoot: AccessibilityNodeInfoCompat?,
        includeChildrenOfNodesWithWebActions: Boolean
    ) :WorkingTree? {
        if (compatRoot == null) {
            return null
        }

        val boundsCalculator = NodeCachedBoundsCalculator()
        boundsCalculator.setSpeakingNodesCache(speakingNodesCache)
        tree = createWorkingTree(
                compatRoot,
                null,
                boundsCalculator,
                includeChildrenOfNodesWithWebActions
            )
        reorderTree()
        return tree
    }

    /**
     * Creates tree that reproduces AccessibilityNodeInfoCompat tree hierarchy
     *
     * @param rootNode root node that is starting point for tree reproduction
     * @param parent parent WorkingTree node for subtree that would be returned in this method
     * @param includeChildrenOfNodesWithWebActions whether to calculator order for nodes that support
     * web actions. Although TalkBack uses the naviagation order specified by the nodes, Switch
     * Access needs to know about all nodes at the time the tree is being created.
     * @return subtree that reproduces accessibility node hierarchy
     */
    private fun createWorkingTree(
        rootNode: AccessibilityNodeInfoCompat,
        parent: WorkingTree?,
        boundsCalculator: NodeCachedBoundsCalculator,
        includeChildrenOfNodesWithWebActions: Boolean
    ): WorkingTree? {
        if (nodeTreeMap.containsKey(rootNode)) {
            return null
        }

        val tree = WorkingTree(rootNode, parent)
        nodeTreeMap[rootNode] = tree

        // When we reach a node that supports web navigation, we traverse using the web navigation
        // actions, so we should not try to determine the ordering of its descendants.
      //  if (!includeChildrenOfNodesWithWebActions && WebInterfaceUtils.supportsWebActions(rootNode)) {
//            return tree
//        }

        val iterator =
            ReorderedChildrenIterator.createAscendingIterator(rootNode, boundsCalculator)
        if (iterator == null) {
            return null
        }
        while (iterator!!.hasNext()) {
            val child: AccessibilityNodeInfoCompat = iterator.next()
            val childSubTree =
                createWorkingTree(
                    child,
                    tree,
                    boundsCalculator,
                    includeChildrenOfNodesWithWebActions
                )
            if (childSubTree != null) {
                tree.addChild(childSubTree)
            }
        }
        return tree
    }

    /**
     * reorder previously created tree according to after/before view traversal order on separate
     * nodes
     */
    private fun reorderTree() {
        for (subtree in nodeTreeMap.values  ) {
            val node: AccessibilityNodeInfoCompat = subtree.getNode()
            if (AccessibilityNodeInfoUtils.hasRequestInitialAccessibilityFocus(node)) {
                    // TODO: Add test case after Roboletric in Google3 supports API 34.
                initialFocusNode = node
            }
            val beforeNode: AccessibilityNodeInfoCompat? = node.traversalBefore
            if (beforeNode != null) {
                val targetTree = nodeTreeMap[beforeNode]
                moveNodeBefore(subtree, targetTree)
            } else {
                val afterNode: AccessibilityNodeInfoCompat? = node.traversalAfter
                if (afterNode != null) {
                    val targetTree = nodeTreeMap[afterNode]
                    moveNodeAfter(subtree, targetTree)
                }
            }
        }
    }

    /** Moves movingTree before targetTree.  */
    private fun moveNodeBefore(
         movingTree: WorkingTree?,
         targetTree: WorkingTree?
    ) {
        if (movingTree == null || targetTree == null) {
            return
        }

        if (movingTree.hasDescendant(targetTree)) {
            // no operation if move child before parent
            return
        }

        // Find subtree to move.
        val movingTreeRoot = getParentsThatAreMovedBeforeOrSameNode(movingTree)

        // Find destination for movingTreeRoot.
        val parent = targetTree.parent
        if (movingTreeRoot.hasDescendant(parent)) {
            return  // Moving movingTreeRoot under its own descendant would create a loop.
        }

        // Unlink moving subtree from tree.
        detachSubtreeFromItsParent(movingTreeRoot)

        // swap target node with moving node on targets node parent children list
        parent?.swapChild(targetTree, movingTreeRoot)

        movingTreeRoot.parent = parent

        // add target node as last child of moving node
        movingTree.addChild(targetTree)
        targetTree.parent = movingTree
    }

    /**
     * This method is called before moving subtree. It checks if parent of that node was moved on its
     * place because it has before property to that node. In that case parent node should be moved
     * with movingTree node.
     *
     * @return top node that should be moved with movingTree node.
     */
    private fun getParentsThatAreMovedBeforeOrSameNode(movingTree: WorkingTree): WorkingTree {
        val parent = movingTree.parent ?: return movingTree

        val parentNode: AccessibilityNodeInfoCompat = parent.getNode()
        val parentNodeBefore: AccessibilityNodeInfoCompat = parentNode.traversalBefore
            ?: return movingTree

        if (parentNodeBefore == movingTree.getNode()) {
            return getParentsThatAreMovedBeforeOrSameNode(parent)
        }

        return movingTree
    }

    private fun detachSubtreeFromItsParent(subtree: WorkingTree?) {
        val movingTreeParent = subtree!!.parent
        movingTreeParent?.removeChild(subtree)
        subtree.parent = null
    }

    private fun moveNodeAfter(
         movingTree: WorkingTree,
         targetTree: WorkingTree?
    ) {
        var movingTree: WorkingTree? = movingTree
        if (movingTree == null || targetTree == null) {
            return
        }

        if (movingTree.hasDescendant(targetTree)) {
            return  // Moving movingTree under its own descendant would create a loop.
        }
        movingTree = getParentsThatAreMovedBeforeOrSameNode(movingTree)
        if (movingTree.hasDescendant(targetTree)) {
            return  // Moving movingTree under its own descendant would create a loop.
        }
        detachSubtreeFromItsParent(movingTree)
        targetTree.addChild(movingTree)
        movingTree.parent = targetTree
    }


    fun findNext(node: AccessibilityNodeInfoCompat?): AccessibilityNodeInfoCompat? {
        val tree = nodeTreeMap[node] ?: return null

        val nextTree = tree.next
        if (nextTree != null) {
            return nextTree.getNode()
        }

        return null
    }


    fun findPrevious(node: AccessibilityNodeInfoCompat?): AccessibilityNodeInfoCompat? {
        val tree = nodeTreeMap[node] ?: return null

        val prevTree = tree.previous
        if (prevTree != null) {
            return prevTree.getNode()
        }

        return null
    }

    /** Searches first node to be focused  */

    fun findFirst(): AccessibilityNodeInfoCompat? {
        if (tree == null) {
            return null
        }

        return tree!!.root!!.getNode()
    }

    fun findFirst(rootNode: AccessibilityNodeInfoCompat?): AccessibilityNodeInfoCompat? {
        if (rootNode == null) {
            return null
        }

        val tree = nodeTreeMap[rootNode] ?: return null

        return tree.getNode()
    }

    fun findInitial(rootNode: AccessibilityNodeInfoCompat?): AccessibilityNodeInfoCompat? {
        if (rootNode == null) {
            return null
        }

        val tree = nodeTreeMap[rootNode] ?: return null

        if (tree.hasDescendant(nodeTreeMap[initialFocusNode])) {
            return initialFocusNode
        }

        return null
    }

    /** Searches last node to be focused  */
    fun findLast(): AccessibilityNodeInfoCompat? {
        if (tree == null) {
            return null
        }

        return tree!!.root!!.lastNode.getNode()
    }

    fun findLast(rootNode: AccessibilityNodeInfoCompat?): AccessibilityNodeInfoCompat? {
        if (rootNode == null) {
            return null
        }

        val tree = nodeTreeMap[rootNode] ?: return null

        return tree.lastNode.getNode()
    }



    companion object {
        private const val TAG = "OrderedTraversalCont"


         // Returns the string contains the attribute [AccessibilityNodeInfo.getTraversalBefore]
         // and [AccessibilityNodeInfo.getTraversalAfter] of the target node.
        private fun getCustomizedTraversalNodeString(node: AccessibilityNodeInfoCompat): String {
            val builder = StringBuilder()
            val beforeNode: AccessibilityNodeInfoCompat? = node.traversalBefore
            val afterNode: AccessibilityNodeInfoCompat? = node.traversalAfter
            if (beforeNode != null) {
                builder.append(" before:")
                builder.append(beforeNode.hashCode())
            }
            if (afterNode != null) {
                builder.append(" after:")
                builder.append(afterNode.hashCode())
            }
            return builder.toString()
        }
    }
}
