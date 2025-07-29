package com.example.snapcollector

import kotlinx.serialization.Serializable
enum class ComparisonStrategy {
    STRICT,
    PRESENT,
    IGNORE
}

@Serializable
data class SnapIssue(
    val message: String,
    val rect: SnapRect,
    val path: String = ""
)

@Serializable
data class SnapRect(
    val left: Int =-1,
    val top: Int =-1,
    val right: Int =-1,
    val bottom: Int =-1
) {
    fun isOverlapping(other: SnapRect): Boolean {
        return !(left >= other.right || right <= other.left || top >= other.bottom || bottom <= other.top)
    }
}


@Serializable
data class SnapRange(
    val min: Float,
    val max: Float,
    val current: Float
)

@Serializable
data class SnapNode(
    val text: String = "",
    val hint: String = "",
    val role: Role = Role.NONE,
    val actionable: Boolean = false,
    val heading: Boolean = false,
    val checked: Boolean = false,
    val selected: Boolean = false,
    val roleDescription: String = "",
    val stateDescription: String = "",
    val rect: SnapRect? = null,
    val range: SnapRange? = null,
    val children: MutableList<SnapNode> =mutableListOf(),
    val actions: List<String> = listOf(),
    val propertyComparisonStrategies: Map<String, ComparisonStrategy> = emptyMap()
)

