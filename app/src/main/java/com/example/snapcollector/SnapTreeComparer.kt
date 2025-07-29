package com.example.snapcollector

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

object SnapTreeComparer {
    fun compareTrees(treeA: MutableList<SnapNode>, treeB: MutableList<SnapNode>): List<SnapIssue> {
        val report = mutableListOf<SnapIssue>()
        val path = ""
        compareTrees(treeA, treeB, report, path)
        return report
    }

    private fun compareTrees(
        treeA: MutableList<SnapNode>,
        treeB: MutableList<SnapNode>,
        report: MutableList<SnapIssue>,
        path: String
    ) {
        if (treeA.size != treeB.size) {
            clearTree(treeA, treeB, report)
        }
        for (i in treeA.indices) {
            compareNodes(treeA[i], treeB[i], report, "$path$i")
        }
    }

    private fun compareNodes(
        nodeA: SnapNode,
        nodeB: SnapNode,
        report: MutableList<SnapIssue>,
        path: String
    ) {
        val propertiesToCompare = SnapNode::class.memberProperties.filter {
            (it.name != "rect" && it.name != "children" && it.name != "propertyComparisonStrategies")
        }

        for (property in propertiesToCompare) {
            compareProperty(nodeA,nodeB,property,report,path)
            }
        compareTrees(nodeA.children, nodeB.children, report, "$path/")
    }

    private fun clearTree(
        treeA: MutableList<SnapNode>,
        treeB: MutableList<SnapNode>,
        report: MutableList<SnapIssue>
    ) {
        val (largerTree, smallerTree) = if (treeA.size >= treeB.size) Pair(treeA, treeB)
        else
            Pair(
                treeB,
                treeA
            )
        var extraNumber = largerTree.size - smallerTree.size
        for (i in smallerTree.indices) {

            if (smallerTree[i].text != largerTree[i].text) {
                if (removeRedundant(smallerTree, largerTree, extraNumber, i, report)) {
                    extraNumber--
                }
            }
            if (extraNumber == 0) {
                return
            }
        }
        if (extraNumber > 0) {
            while (extraNumber != 0) {
                report.add(
                    SnapIssue(
                        message = "${largerTree[largerTree.lastIndex].text} is redundant element",
                        rect = largerTree[largerTree.lastIndex].rect ?: SnapRect()
                    )
                )
                largerTree.removeAt(largerTree.lastIndex)
                extraNumber--
            }
        }
    }

    private fun removeRedundant(
        a: MutableList<SnapNode>,
        b: MutableList<SnapNode>,
        extraNumber: Int,
        removalCandidateIndex: Int,
        report: MutableList<SnapIssue>
    ): Boolean {
        val candidate = hashMapOf<Int, Int>()
        for (i in removalCandidateIndex..removalCandidateIndex + extraNumber) {
            val matchIndex = getMatchIndex(a[removalCandidateIndex], b[i])
            candidate[i] = matchIndex
        }
        val max = candidate.values.max()
        if (max == candidate[removalCandidateIndex]) {
            report.add(
                SnapIssue(
                    message = "${b[removalCandidateIndex].text} is redundant element",
                    rect = b[removalCandidateIndex].rect ?: SnapRect()
                )
            )
            b.removeAt(removalCandidateIndex)
            return true
        }
        return false
    }

    private fun getMatchIndex(a: SnapNode, b: SnapNode): Int {
        val propertiesToCompare = SnapNode::class.memberProperties.filter {
            (it.name != "rect" && it.name != "children" && it.name != "propertyComparisonStrategies")
        }
        var matchIndex = 0
        for (property in propertiesToCompare) {
            val valueA = property.get(a)
            val valueB = property.get(b)
            if (valueA != valueB) {
                matchIndex++
            }
        }
        return matchIndex
    }

    fun getScreenTitle(tree: List<SnapNode>): String? {
        for (node in tree) {
            if (node.text.isNotEmpty() && (node.role == Role.NONE)) {
                return extractTitle(node.text)
            }
            if (node.children.isNotEmpty()) {
                return getScreenTitle(node.children)
            }
        }
        return null
    }

    private fun extractTitle(title: String): String {
        var limit = 2
        val words = title.split(" ")
        if (words.size < limit) {
            limit = words.size
        }
        var resultTitle = ""
        for (i in 0 until limit) {
            resultTitle = "$resultTitle ${words[i]}"
        }
        return resultTitle
    }

    private fun compareProperty(
        nodeA: SnapNode,
        nodeB: SnapNode,
        property: KProperty1<SnapNode, *>,
        report: MutableList<SnapIssue>,
        path: String
    ) {
        val propertyName = property.name
        val strategy = nodeB.propertyComparisonStrategies[propertyName] ?: ComparisonStrategy.STRICT

        val valueA = property.get(nodeA)
        val valueB = property.get(nodeB)

        when (strategy) {
            ComparisonStrategy.STRICT -> {
                if (valueA != valueB) {
                    report.add(
                        SnapIssue(
                            "Property '$propertyName' differs. A: $valueA, B: $valueB",
                            nodeA.rect ?: SnapRect(),
                            path
                        )
                    )
                }
            }

            ComparisonStrategy.PRESENT -> {
                if ((propertyName == "role") && (valueA == Role.NONE)
                ) {
                    report.add(
                        SnapIssue(
                            "Role is not defined in node A.",
                            nodeA.rect ?: SnapRect(),
                            path
                        )
                    )
                }

                if ((propertyName == "text") && (valueA == null || (valueA is String && valueA.isEmpty()))) {
                    report.add(
                        SnapIssue(
                            "Text is missing in node A.",
                            nodeA.rect ?: SnapRect(),
                            path
                        )
                    )
                }
            }

            ComparisonStrategy.IGNORE ->{
                //  nothink
            }
        }


    }

}