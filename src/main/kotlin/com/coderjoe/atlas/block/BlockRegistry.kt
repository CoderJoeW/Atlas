package com.coderjoe.atlas.block

import com.coderjoe.atlas.util.atlasInfo
import com.coderjoe.atlas.util.coordinates
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

class BlockRegistry(private val plugin: JavaPlugin) {
    private val blocks = ConcurrentHashMap<String, AtlasBlock>()
    private val blockIds = ConcurrentHashMap<String, String>()

    /** Locations Atlas is placing a block state at, so the listener ignores its own placements. */
    val updatingLocations: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        active = this
    }

    companion object {
        /**
         * The registry the running plugin built, for blocks that have to find a neighbour and
         * have nothing else to ask.
         *
         * Temporary, and the last global in the plugin. Phase 3 hands every block a context when
         * it is registered and this goes with it - it is here so that this step stays about the
         * index being single, not about how a block reaches it.
         */
        var active: BlockRegistry? = null
            private set

        /** Tests build a registry per case and must not be shown the last case's. */
        internal fun clearActive() {
            active = null
        }

        fun locationKey(location: Location): String {
            return "${location.world?.name}:${location.coordinates}"
        }
    }

    fun register(
        block: AtlasBlock,
        blockId: String,
    ) {
        val key = locationKey(block.location)
        blocks[key] = block
        blockIds[key] = blockId
        block.start()
        plugin.logger.atlasInfo(
            """
            Registered ${block::class.simpleName} at ${block.location.coordinates}
            """.trimIndent(),
        )
    }

    fun unregister(location: Location): AtlasBlock? {
        val key = locationKey(location)
        val block = blocks.remove(key)
        blockIds.remove(key)
        block?.stop()
        if (block != null) {
            plugin.logger.atlasInfo("Unregistered ${block::class.simpleName} at ${location.coordinates}")
        }
        return block
    }

    fun getBlock(location: Location): AtlasBlock? {
        return blocks[locationKey(location)]
    }

    fun getAdjacentBlock(
        location: Location,
        face: BlockFace,
    ): AtlasBlock? {
        val offset = face.direction
        return getBlock(
            Location(
                location.world,
                (location.blockX + offset.blockX).toDouble(),
                (location.blockY + offset.blockY).toDouble(),
                (location.blockZ + offset.blockZ).toDouble(),
            ),
        )
    }

    /**
     * The neighbour on [face] when it is a [T], and null when that square is empty or holds
     * something else.
     *
     * This is what every call site that used to reach into its own system's registry wants: a
     * cable asking for the power block beside it must still get null for the belt beside it.
     */
    inline fun <reified T> adjacentOf(
        location: Location,
        face: BlockFace,
    ): T? {
        return getAdjacentBlock(location, face) as? T
    }

    fun getAdjacentBlocks(location: Location): List<AtlasBlock> {
        val offsets =
            listOf(
                intArrayOf(1, 0, 0),
                intArrayOf(-1, 0, 0),
                intArrayOf(0, 1, 0),
                intArrayOf(0, -1, 0),
                intArrayOf(0, 0, 1),
                intArrayOf(0, 0, -1),
            )
        return offsets.mapNotNull { (dx, dy, dz) ->
            getBlock(
                Location(
                    location.world,
                    (location.blockX + dx).toDouble(),
                    (location.blockY + dy).toDouble(),
                    (location.blockZ + dz).toDouble(),
                ),
            )
        }
    }

    fun getAllBlocksWithIds(): List<Pair<AtlasBlock, String>> {
        return blocks.entries.mapNotNull { entry ->
            val block = entry.value
            val blockId = blockIds[entry.key]
            if (blockId != null) Pair(block, blockId) else null
        }
    }

    fun getAllBlocks(): Collection<AtlasBlock> {
        return blocks.values
    }

    fun stopAll() {
        plugin.logger.atlasInfo("Stopping ${blocks.size} blocks...")
        blocks.values.forEach { it.stop() }
        blocks.clear()
    }
}
