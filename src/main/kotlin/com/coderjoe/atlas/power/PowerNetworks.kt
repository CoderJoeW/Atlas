package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.AtlasBlock
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.power.block.PowerCable

/**
 * Finds the connected run of cable a given cable belongs to.
 *
 * Discovery is a flood fill done fresh each time rather than a cached graph kept in step with
 * every place and break. Runs are small and this is cheap, and it removes a whole class of bugs
 * where a stale edge outlives the cable that made it.
 */
object PowerNetworks {
    fun networkFor(start: PowerCable): PowerNetwork {
        val registry = PowerBlockRegistry.instance ?: return PowerNetwork(listOf(start))

        val found = LinkedHashMap<String, PowerCable>()
        val queue = ArrayDeque<PowerCable>()
        found[BlockRegistry.locationKey(start.location)] = start
        queue.add(start)

        while (queue.isNotEmpty()) {
            val cable = queue.removeFirst()
            for (face in AtlasBlock.ADJACENT_FACES) {
                val neighbor = registry.getAdjacentBlock(cable.location, face)
                if (neighbor !is PowerCable) continue
                val key = BlockRegistry.locationKey(neighbor.location)
                if (found.putIfAbsent(key, neighbor) == null) {
                    queue.add(neighbor)
                }
            }
        }

        return PowerNetwork(found.values.toList())
    }
}
