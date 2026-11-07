package com.example.data.model

/**
 * The festival animations the launcher can draw on the Home screen.
 *
 * Each one has its own on/off toggle (Settings > Auto & Animations), and the Diwali theme simply
 * switches them all on. They only ever draw inside the launcher itself.
 */
enum class AnimationEffect(val key: String, val label: String) {
    LAMP("lamp", "Lamps"),
    FIRECRACKERS("firecrackers", "Firecrackers"),
    RANGOLI("rangoli", "Rangoli"),
    CHAKRI("chakri", "Chakri"),
    SKY_LANTERNS("sky_lanterns", "Sky lanterns"),
    PETALS("petals", "Flower petals");

    companion object {
        fun fromKey(key: String?): AnimationEffect? = entries.firstOrNull { it.key == key }

        /** Every effect, which is what the Diwali theme turns on. */
        val allKeys: Set<String> = entries.map { it.key }.toSet()
    }
}
