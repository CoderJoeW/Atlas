package com.coderjoe.atlas.block

import com.coderjoe.atlas.block.deposit.DepositMap
import com.coderjoe.atlas.util.atlasInfo
import com.coderjoe.atlas.util.coordinates
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

class BlockRegistry(
    private val plugin: JavaPlugin,
    deposits: DepositMap = DepositMap.UNIFORM,
) {
    private val blocks = ConcurrentHashMap<String, AtlasBlock>()
    private val blockIds = ConcurrentHashMap<String, String>()

    /** Locations Atlas is placing a block state at, so the listener ignores its own placements. */
    val updatingLocations: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val context = BlockContext(plugin, this, deposits)

    /** The one scheduler task every registered block is updated from. */
    val ticker = BlockTicker(plugin)

    private val systems = ConcurrentHashMap<Class<*>, Any>()

    /**
     * Neighbour lookups made through [getAdjacentBlock] - what every block's view of its
     * surroundings goes through, so the clearest single measure of how simulation work grows.
     */
    var adjacentLookups: Long = 0L
        private set

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
     * Indexes [block] and hands it its context without putting it on the [ticker]. [register] is
     * this plus [AtlasBlock.start]; tests call it alone to lay out a neighbourhood and drive each
     * block's update by hand.
     */
    internal fun track(
        block: AtlasBlock,
        blockId: String,
    ) {
        val key = locationKey(block.location)
        block.attach(context)
        val replaced = blocks.put(key, block)
        blockIds[key] = blockId
        if (replaced != null && replaced !== block) {
            replaced.stop()
            replaced.onRemoved()
        }
        block.onPlaced()
        notifyNeighbors(block.location)
    }

    fun unregister(location: Location): AtlasBlock? {
        val key = locationKey(location)
        val block = blocks.remove(key)
        blockIds.remove(key)
        if (block != null) {
            block.stop()
            block.onRemoved()
            notifyNeighbors(location)
            plugin.logger.atlasInfo("Unregistered ${block::class.simpleName} at ${location.coordinates}")
        }
        return block
    }

    /**
     * The one [type] shared by every block in this registry, made by [create] the first time it is
     * asked for. A block family keeps state that spans many blocks - a cable run's network - here,
     * so this package never has to know about the families built on it.
     */
    fun <T : Any> system(
        type: Class<T>,
        create: (BlockRegistry) -> T,
    ): T = type.cast(systems.getOrPut(type) { create(this) })

    private fun notifyNeighbors(location: Location) {
        for (face in AtlasBlock.ADJACENT_FACES) {
            getAdjacentBlock(location, face)?.onNeighborChanged()
        }
    }

    fun getBlock(location: Location): AtlasBlock? {
        return blocks[locationKey(location)]
    }

    fun getAdjacentBlock(
        location: Location,
        face: BlockFace,
    ): AtlasBlock? {
        adjacentLookups++
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
        blockIds.clear()
        ticker.stop()
        systems.clear()
    }
}
