package com.coderjoe.atlas.power

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry to track all active power blocks in the world
 */
class PowerBlockRegistry(private val plugin: JavaPlugin) {
    private val powerBlocks = ConcurrentHashMap<String, PowerBlock>()
    private val blockIds = ConcurrentHashMap<String, String>() // Maps location key to block ID
    val updatingLocations: MutableSet<String> = ConcurrentHashMap.newKeySet()

    companion object {
        var instance: PowerBlockRegistry? = null
            private set

        fun locationKey(location: Location): String {
            return "${location.world?.name}:${location.blockX},${location.blockY},${location.blockZ}"
        }
    }

    init {
        instance = this
    }

    /**
     * Registers and starts a power block
     */
    fun registerPowerBlock(powerBlock: PowerBlock, blockId: String) {
        val key = locationKey(powerBlock.location)
        powerBlocks[key] = powerBlock
        blockIds[key] = blockId
        powerBlock.start()
        plugin.logger.info("Registered ${powerBlock::class.simpleName} at ${powerBlock.location.blockX},${powerBlock.location.blockY},${powerBlock.location.blockZ}")
    }

    /**
     * Unregisters and stops a power block at the given location
     */
    fun unregisterPowerBlock(location: Location): PowerBlock? {
        val key = locationKey(location)
        val powerBlock = powerBlocks.remove(key)
        blockIds.remove(key)
        powerBlock?.stop()
        if (powerBlock != null) {
            plugin.logger.info("Unregistered ${powerBlock::class.simpleName} at ${location.blockX},${location.blockY},${location.blockZ}")
        }
        return powerBlock
    }

    /**
     * Gets a power block at the given location
     */
    fun getPowerBlock(location: Location): PowerBlock? {
        return powerBlocks[locationKey(location)]
    }

    /**
     * Stops all power blocks (called on plugin disable)
     */
    fun stopAll() {
        plugin.logger.info("Stopping ${powerBlocks.size} power blocks...")
        powerBlocks.values.forEach { it.stop() }
        powerBlocks.clear()
    }

    /**
     * Gets all registered power blocks
     */
    fun getAllPowerBlocks(): Collection<PowerBlock> {
        return powerBlocks.values
    }

    /**
     * Gets all power blocks with their block IDs for persistence
     */
    fun getAllPowerBlocksWithIds(): List<Pair<PowerBlock, String>> {
        return powerBlocks.entries.mapNotNull { entry ->
            val powerBlock = entry.value
            val blockId = blockIds[entry.key]
            if (blockId != null) {
                Pair(powerBlock, blockId)
            } else {
                null
            }
        }
    }

    /**
     * Gets the power block adjacent to the given location in the specified direction
     */
    fun getAdjacentPowerBlock(location: Location, face: BlockFace): PowerBlock? {
        val offset = face.direction
        return getPowerBlock(Location(location.world,
            (location.blockX + offset.blockX).toDouble(),
            (location.blockY + offset.blockY).toDouble(),
            (location.blockZ + offset.blockZ).toDouble()))
    }

    /**
     * Gets all power blocks adjacent (6 directions) to the given location
     */
    fun getAdjacentPowerBlocks(location: Location): List<PowerBlock> {
        val offsets = listOf(
            intArrayOf(1, 0, 0), intArrayOf(-1, 0, 0),
            intArrayOf(0, 1, 0), intArrayOf(0, -1, 0),
            intArrayOf(0, 0, 1), intArrayOf(0, 0, -1)
        )
        return offsets.mapNotNull { (dx, dy, dz) ->
            getPowerBlock(Location(location.world, (location.blockX + dx).toDouble(), (location.blockY + dy).toDouble(), (location.blockZ + dz).toDouble()))
        }
    }

}
