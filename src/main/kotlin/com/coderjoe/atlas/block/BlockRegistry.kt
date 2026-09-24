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

    private val context = BlockContext(plugin, this)

    companion object {
        fun locationKey(location: Location): String {
            return "${location.world?.name}:${location.coordinates}"
        }
    }

    fun register(
        block: AtlasBlock,
        blockId: String,
    ) {
        track(block, blockId)
        block.start()
        plugin.logger.atlasInfo(
            """
            Registered ${block::class.simpleName} at ${block.location.coordinates}
            """.trimIndent(),
        )
    }

    /**
     * Indexes [block] and hands it its context without starting its tick tasks. [register] is
     * this plus [AtlasBlock.start]; tests call it alone to lay out a neighbourhood and drive each
     * block's update by hand.
     */
    internal fun track(
        block: AtlasBlock,
        blockId: String,
    ) {
        val key = locationKey(block.location)
        block.attach(context)
        blocks[key] = block
        blockIds[key] = blockId
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
