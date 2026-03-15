package com.coderjoe.atlas.core

import com.coderjoe.atlas.atlasInfo
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

open class BlockRegistry<T : AtlasBlock>(protected val plugin: JavaPlugin) {
    protected val blocks = ConcurrentHashMap<String, T>()
    protected val blockIds = ConcurrentHashMap<String, String>()
    val updatingLocations: MutableSet<String> = ConcurrentHashMap.newKeySet()

    companion object {
        fun locationKey(location: Location): String {
            return "${location.world?.name}:${location.blockX},${location.blockY},${location.blockZ}"
        }
    }

    fun register(
        block: T,
        blockId: String,
    ) {
        val key = locationKey(block.location)
        blocks[key] = block
        blockIds[key] = blockId
        block.start()
        plugin.logger.atlasInfo(
            """
            Registered ${block::class.simpleName} at ${block.location.blockX},${block.location.blockY},${block.location.blockZ}
            """.trimIndent(),
        )
    }

    fun unregister(location: Location): T? {
        val key = locationKey(location)
        val block = blocks.remove(key)
        blockIds.remove(key)
        block?.stop()
        if (block != null) {
            plugin.logger.atlasInfo("Unregistered ${block::class.simpleName} at ${location.blockX},${location.blockY},${location.blockZ}")
        }
        return block
    }

    fun getBlock(location: Location): T? {
        return blocks[locationKey(location)]
    }

    fun getAdjacentBlock(
        location: Location,
        face: BlockFace,
    ): T? {
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

    fun getAdjacentBlocks(location: Location): List<T> {
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

    fun getAllBlocksWithIds(): List<Pair<T, String>> {
        return blocks.entries.mapNotNull { entry ->
            val block = entry.value
            val blockId = blockIds[entry.key]
            if (blockId != null) Pair(block, blockId) else null
        }
    }

    fun getAllBlocks(): Collection<T> {
        return blocks.values
    }

    fun stopAll() {
        plugin.logger.atlasInfo("Stopping ${blocks.size} blocks...")
        blocks.values.forEach { it.stop() }
        blocks.clear()
    }
}
