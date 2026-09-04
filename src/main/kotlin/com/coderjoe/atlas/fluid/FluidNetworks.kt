package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.core.AtlasBlock
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.fluid.block.FluidPipe

/**
 * Finds the connected run of pipe a given pipe belongs to.
 *
 * Discovery is a flood fill done fresh each time rather than a cached graph kept in step with
 * every place and break. Runs are small and this is cheap, and it removes a whole class of bugs
 * where a stale edge outlives the pipe that made it.
 */
object FluidNetworks {
    fun networkFor(start: FluidPipe): FluidNetwork {
        val registry = FluidBlockRegistry.instance ?: return FluidNetwork(listOf(start))

        val found = LinkedHashMap<String, FluidPipe>()
        val queue = ArrayDeque<FluidPipe>()
        found[BlockRegistry.locationKey(start.location)] = start
        queue.add(start)

        while (queue.isNotEmpty()) {
            val pipe = queue.removeFirst()
            for (face in AtlasBlock.ADJACENT_FACES) {
                val neighbor = registry.getAdjacentBlock(pipe.location, face)
                if (neighbor !is FluidPipe) continue
                val key = BlockRegistry.locationKey(neighbor.location)
                if (found.putIfAbsent(key, neighbor) == null) {
                    queue.add(neighbor)
                }
            }
        }

        return FluidNetwork(found.values.toList())
    }
}
