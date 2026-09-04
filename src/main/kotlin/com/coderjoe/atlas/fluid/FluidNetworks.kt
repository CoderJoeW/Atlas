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
 *
 * Touching pipe is not automatically the same run. A run takes its identity from the source
 * feeding it, and a lava run that meets a water run stays a separate network on either side of
 * where they meet - otherwise butting two lines together would silently blend them, and which
 * fluid won would come down to which provider the scan happened to reach first.
 */
object FluidNetworks {
    fun networkFor(start: FluidPipe): FluidNetwork {
        val registry = FluidBlockRegistry.instance ?: return FluidNetwork(listOf(start))

        val touching = touching(start, registry)
        val fluids = fluidsBySource(touching, registry)
        val own = fluids[key(start)] ?: FluidType.NONE
        return FluidNetwork(sameFluidRun(start, touching, fluids, own, registry))
    }

    private fun key(pipe: FluidPipe) = BlockRegistry.locationKey(pipe.location)

    /** Every pipe reachable from [start] through touching pipe, ignoring what any of it carries. */
    private fun touching(
        start: FluidPipe,
        registry: FluidBlockRegistry,
    ): Map<String, FluidPipe> {
        val found = LinkedHashMap<String, FluidPipe>()
        val queue = ArrayDeque<FluidPipe>()
        found[key(start)] = start
        queue.add(start)

        while (queue.isNotEmpty()) {
            val pipe = queue.removeFirst()
            for (face in AtlasBlock.ADJACENT_FACES) {
                val neighbor = registry.getAdjacentBlock(pipe.location, face)
                if (neighbor !is FluidPipe) continue
                if (found.putIfAbsent(key(neighbor), neighbor) == null) queue.add(neighbor)
            }
        }
        return found
    }

    /**
     * Labels each pipe with the fluid of the nearest source feeding it.
     *
     * This is a flood fill outward from every source at once, so where a lava line and a water
     * line meet, each pipe belongs to whichever source it sits closer to and the boundary falls
     * between them. A pipe no source reaches is left unlabelled and reads as carrying nothing,
     * which keeps an unfed run behaving as the single network it looks like.
     */
    private fun fluidsBySource(
        pipes: Map<String, FluidPipe>,
        registry: FluidBlockRegistry,
    ): Map<String, FluidType> {
        val labelled = HashMap<String, FluidType>()
        val queue = ArrayDeque<String>()

        for ((pipeKey, pipe) in pipes) {
            for (face in AtlasBlock.ADJACENT_FACES) {
                val neighbor = registry.getAdjacentBlock(pipe.location, face) ?: continue
                if (neighbor is FluidPipe) continue
                if (!neighbor.canProvideFluid(face.oppositeFace) || !neighbor.hasFluid()) continue

                // A pipe touching two sources at once has to pick one; the lower ordinal is an
                // arbitrary rule, but a stable one, so the run does not flicker between them.
                val existing = labelled[pipeKey]
                if (existing == null || neighbor.storedFluid.ordinal < existing.ordinal) {
                    labelled[pipeKey] = neighbor.storedFluid
                }
            }
            if (pipeKey in labelled) queue.add(pipeKey)
        }

        while (queue.isNotEmpty()) {
            val pipeKey = queue.removeFirst()
            val fluid = labelled[pipeKey] ?: continue
            val pipe = pipes[pipeKey] ?: continue
            for (face in AtlasBlock.ADJACENT_FACES) {
                val neighbor = registry.getAdjacentBlock(pipe.location, face)
                if (neighbor !is FluidPipe) continue
                val neighborKey = key(neighbor)
                if (neighborKey !in pipes || neighborKey in labelled) continue
                labelled[neighborKey] = fluid
                queue.add(neighborKey)
            }
        }
        return labelled
    }

    /** The pipes reachable from [start] without crossing into a run carrying something else. */
    private fun sameFluidRun(
        start: FluidPipe,
        pipes: Map<String, FluidPipe>,
        fluids: Map<String, FluidType>,
        own: FluidType,
        registry: FluidBlockRegistry,
    ): List<FluidPipe> {
        val found = LinkedHashMap<String, FluidPipe>()
        val queue = ArrayDeque<FluidPipe>()
        found[key(start)] = start
        queue.add(start)

        while (queue.isNotEmpty()) {
            val pipe = queue.removeFirst()
            for (face in AtlasBlock.ADJACENT_FACES) {
                val neighbor = registry.getAdjacentBlock(pipe.location, face)
                if (neighbor !is FluidPipe) continue
                val neighborKey = key(neighbor)
                if (neighborKey !in pipes) continue
                if ((fluids[neighborKey] ?: FluidType.NONE) != own) continue
                if (found.putIfAbsent(neighborKey, neighbor) == null) queue.add(neighbor)
            }
        }
        return found.values.toList()
    }
}
