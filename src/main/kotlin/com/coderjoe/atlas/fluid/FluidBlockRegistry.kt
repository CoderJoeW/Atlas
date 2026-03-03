package com.coderjoe.atlas.fluid

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

class FluidBlockRegistry(private val plugin: JavaPlugin) {
    private val fluidBlocks = ConcurrentHashMap<String, FluidBlock>()
    private val blockIds = ConcurrentHashMap<String, String>()

    companion object {
        var instance: FluidBlockRegistry? = null
            private set
    }

    init {
        instance = this
    }

    fun registerFluidBlock(fluidBlock: FluidBlock, blockId: String) {
        val key = locationKey(fluidBlock.location)
        fluidBlocks[key] = fluidBlock
        blockIds[key] = blockId
        fluidBlock.start()
        plugin.logger.info("Registered ${fluidBlock::class.simpleName} at ${fluidBlock.location.blockX},${fluidBlock.location.blockY},${fluidBlock.location.blockZ}")
    }

    fun unregisterFluidBlock(location: Location): FluidBlock? {
        val key = locationKey(location)
        val fluidBlock = fluidBlocks.remove(key)
        blockIds.remove(key)
        fluidBlock?.stop()
        if (fluidBlock != null) {
            plugin.logger.info("Unregistered ${fluidBlock::class.simpleName} at ${location.blockX},${location.blockY},${location.blockZ}")
        }
        return fluidBlock
    }

    fun getFluidBlock(location: Location): FluidBlock? {
        return fluidBlocks[locationKey(location)]
    }

    fun getAdjacentFluidBlock(location: Location, face: BlockFace): FluidBlock? {
        val offset = face.direction
        return getFluidBlock(Location(location.world,
            (location.blockX + offset.blockX).toDouble(),
            (location.blockY + offset.blockY).toDouble(),
            (location.blockZ + offset.blockZ).toDouble()))
    }

    fun stopAll() {
        plugin.logger.info("Stopping ${fluidBlocks.size} fluid blocks...")
        fluidBlocks.values.forEach { it.stop() }
        fluidBlocks.clear()
    }

    fun getAllFluidBlocksWithIds(): List<Pair<FluidBlock, String>> {
        return fluidBlocks.entries.mapNotNull { entry ->
            val fluidBlock = entry.value
            val blockId = blockIds[entry.key]
            if (blockId != null) Pair(fluidBlock, blockId) else null
        }
    }

    private fun locationKey(location: Location): String {
        return "${location.world?.name}:${location.blockX},${location.blockY},${location.blockZ}"
    }
}
