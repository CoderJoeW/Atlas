package com.coderjoe.atlas.core

import org.bukkit.Location
import org.bukkit.block.BlockFace

/**
 * Every live block registry, so one system can find a block another system owns.
 *
 * Systems normally keep to themselves - power blocks live in the power registry, fluid blocks in
 * the fluid one - but a few blocks straddle them, and a lookup that only searched its own
 * registry silently missed those.
 *
 * Registries are keyed by their own class, so rebuilding one - which the tests do per case -
 * replaces the previous instance rather than leaving it behind holding stale blocks.
 */
object AtlasBlocks {
    private val registries = LinkedHashMap<Class<*>, BlockRegistry<*>>()

    internal fun register(registry: BlockRegistry<*>) {
        registries[registry.javaClass] = registry
    }

    /** Drops every registry. Tests build fresh ones per case and must not see the last case's. */
    fun clear() = registries.clear()

    fun at(location: Location): AtlasBlock? = registries.values.firstNotNullOfOrNull { it.getBlock(location) }

    fun adjacent(
        location: Location,
        face: BlockFace,
    ): AtlasBlock? {
        val offset = face.direction
        return at(
            Location(
                location.world,
                (location.blockX + offset.blockX).toDouble(),
                (location.blockY + offset.blockY).toDouble(),
                (location.blockZ + offset.blockZ).toDouble(),
            ),
        )
    }
}
