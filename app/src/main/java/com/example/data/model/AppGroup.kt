package com.example.data.model

import java.util.UUID

/**
 * A bundle of apps that takes a single slot in the favourites bar.
 *
 * A group is unnamed on purpose: it is just a handful of apps packed into one slot so the bar's
 * five slots can hold up to twenty apps. Groups never appear in the drawer.
 */
data class AppGroup(
    val id: String = UUID.randomUUID().toString(),
    /** Package names, in the order they are shown inside the slot. At most [FavoriteSlots.MAX_APPS_PER_GROUP]. */
    val packages: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = packages.isEmpty()
}

/** The favourites bar holds either an app (its package name) or a group (see [GROUP_PREFIX]). */
object FavoriteSlots {
    const val GROUP_PREFIX = "group:"

    /** Slots in the bar: apps and groups together. */
    const val MAX_SLOTS = 5

    /** Apps inside one group. */
    const val MAX_APPS_PER_GROUP = 4

    /** 5 slots x 4 apps. */
    const val MAX_TOTAL_APPS = MAX_SLOTS * MAX_APPS_PER_GROUP

    fun forGroup(groupId: String): String = GROUP_PREFIX + groupId

    fun isGroup(slot: String): Boolean = slot.startsWith(GROUP_PREFIX)

    fun groupId(slot: String): String = slot.removePrefix(GROUP_PREFIX)
}
