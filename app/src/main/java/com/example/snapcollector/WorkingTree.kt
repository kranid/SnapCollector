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

/** Tree that represents Accessibility node hierarchy. It lets reorder the structure of the tree.  */
class WorkingTree(node: AccessibilityNodeInfoCompat, parent: WorkingTree?) {
    private val node: AccessibilityNodeInfoCompat


    var parent: WorkingTree?
    val children: MutableList<WorkingTree> = ArrayList()


    init {
        this.node = node
        this.parent = parent
    }


    fun getNode(): AccessibilityNodeInfoCompat {
        return node
    }

    fun addChild(node: WorkingTree) {
        children.add(node)
    }

    fun removeChild(child: WorkingTree): Boolean {
        return children.remove(child)
    }

    /** Checks whether subTree is a descendant of this WorkingTree node.  */
    fun hasDescendant(tree: WorkingTree?): Boolean {
        if (ancestorsHaveLoop()) {
            return false
        }

        // For each ancestor of target descendant node...
        var subTree = tree
        while (subTree != null) {
            val node: AccessibilityNodeInfoCompat = subTree.getNode()

            // If ancestor is this working tree node... target is descendant of this node.
            if (this.node == node) {
                return true
            }

            subTree = subTree.parent
        }

        return false
    }

    /** Checks whether subTree is a descendant of this WorkingTree node.  */
    fun ancestorsHaveLoop(): Boolean {
        val visitedNodes: MutableSet<AccessibilityNodeInfoCompat> =
            HashSet()

        // For each ancestor node...
        var workNode: WorkingTree? = this
        while (workNode != null) {
            val accessNode: AccessibilityNodeInfoCompat = workNode.getNode()
            if (visitedNodes.contains(accessNode)) {
                return true
            }
            visitedNodes.add(accessNode)
            workNode = workNode.parent
        }
        return false
    }

    fun swapChild(swappedChild: WorkingTree, newChild: WorkingTree) {
        val position = children.indexOf(swappedChild)
        if (position < 0) {
            return
        }

        children[position] = newChild
    }

    val next: WorkingTree?
        get() {
            if (children.isNotEmpty()) {
                return children[0]
            }

            var startNode: WorkingTree? = this
            while (startNode != null) {
                val nextSibling = startNode.nextSibling
                if (nextSibling != null) {
                    return nextSibling
                }

                startNode = startNode.parent
            }

            return null
        }

    val nextSibling: WorkingTree?
        get() {
            val parent = parent ?: return null

            var currentIndex = parent.children.indexOf(this)
            if (currentIndex < 0) {
                return null
            }

            currentIndex++

            if (currentIndex >= parent.children.size) {
                // it was last child
                return null
            }

            return parent.children[currentIndex]
        }

    val previous: WorkingTree?
        get() {
            val previousSibling = previousSibling
            if (previousSibling != null) {
                return previousSibling.lastNode
            }

            return parent
        }

    val previousSibling: WorkingTree?
        get() {
            val parent = parent ?: return null

            var currentIndex = parent.children.indexOf(this)
            if (currentIndex < 0) {
                return null
            }

            currentIndex--

            if (currentIndex < 0) {
                // it was first child
                return null
            }

            return parent.children[currentIndex]
        }

    val lastNode: WorkingTree
        get() {
            var node = this
            while (node.children.isNotEmpty()) {
                node = node.children.last()
            }

            return node
        }

    val root: WorkingTree?
        get() {
            var root: WorkingTree? = this
            var parent: WorkingTree? = null
            while ((root?.parent?.also { parent = it }) != null) {
                root = parent
            }

            return root
        }


    companion object {
        private const val TAG = "WorkingTree"
    }
}
