package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.core.AtlasBlock
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.fluid.block.FluidPipe
import org.bukkit.block.BlockFace

/**
 * One connected run of pipe, together with everything hanging off it.
 *
 * Pipes carry no fluid of their own. Each tick the network takes a unit straight from a provider
 * on its edge and hands it to an acceptor on its edge, so a run moves fluid end to end in a
 * single tick no matter how long it is, and no pipe needs to know which way is "forward".
 * Branching and joining fall out of the shape of the run for free.
 */
class FluidNetwork(val pipes: List<FluidPipe>) {
    /**
     * A block on the edge of the network, paired with the face that points from it back at the
     * pipe it touches - the face [FluidBlock.canProvideFluid] is asked about.
     */
    data class Terminal(val block: FluidBlock, val faceTowardPipe: BlockFace)

    /**
     * The pipe that runs the transfer for the whole network.
     *
     * Every pipe in a run discovers the same set, so one of them has to be picked to act, or the
     * transfer would run once per pipe. The lowest location key is stable and needs no shared
     * state to agree on.
     */
    val leader: FluidPipe? get() = pipes.minByOrNull { BlockRegistry.locationKey(it.location) }

    private var nextProviderIndex: Int = 0

    /** The blocks touching this run that can hand fluid in, and those that will take it out. */
    fun terminals(): Pair<List<Terminal>, List<Terminal>> {
        val registry = FluidBlockRegistry.instance ?: return emptyList<Terminal>() to emptyList()
        val providers = LinkedHashMap<String, Terminal>()
        val acceptors = LinkedHashMap<String, Terminal>()

        for (pipe in pipes) {
            for (face in AtlasBlock.ADJACENT_FACES) {
                val neighbor = registry.getAdjacentBlock(pipe.location, face) ?: continue
                if (neighbor is FluidPipe) continue

                val back = face.oppositeFace
                val key = BlockRegistry.locationKey(neighbor.location)
                if (neighbor.canProvideFluid(back)) {
                    providers.putIfAbsent(key, Terminal(neighbor, back))
                }
                if (neighbor.canAcceptFluid(back)) {
                    acceptors.putIfAbsent(key, Terminal(neighbor, back))
                }
            }
        }

        return providers.values.toList() to acceptors.values.toList()
    }

    /** The fluid a provider on this run has to offer right now, or [FluidType.NONE]. */
    fun availableFluid(): FluidType = terminals().first.firstOrNull { it.block.hasFluid() }?.block?.storedFluid ?: FluidType.NONE

    /**
     * Moves one unit from a provider to an acceptor that will take it. Returns what moved, or
     * [FluidType.NONE] if nothing did - which is also what the run renders as.
     */
    fun transfer(): FluidType {
        val (providers, acceptors) = terminals()
        if (providers.isEmpty() || acceptors.isEmpty()) return FluidType.NONE

        for (i in providers.indices) {
            val provider = providers[(nextProviderIndex + i) % providers.size]
            if (!provider.block.hasFluid()) continue

            val offered = provider.block.storedFluid
            val taker =
                acceptors.firstOrNull { it.block !== provider.block && it.block.canAcceptFluid(it.faceTowardPipe, offered) }
                    ?: continue

            provider.block.removeFluid()
            if (!taker.block.storeFluid(offered)) {
                // refused after all - hand it straight back rather than destroying it
                provider.block.storeFluid(offered)
                continue
            }
            nextProviderIndex = (nextProviderIndex + i + 1) % providers.size
            return offered
        }
        return FluidType.NONE
    }

    /**
     * Takes one unit off the run's providers, for consumers that ask a pipe for fluid rather than
     * waiting to be filled - the lava generator works this way.
     */
    fun draw(): FluidType {
        val (providers, _) = terminals()
        for (provider in providers) {
            if (provider.block.hasFluid()) {
                return provider.block.removeFluid()
            }
        }
        return FluidType.NONE
    }

    /** Hands one unit to whichever acceptor on the run will take it. */
    fun deliver(type: FluidType): Boolean {
        if (type == FluidType.NONE) return false
        val (_, acceptors) = terminals()
        for (acceptor in acceptors) {
            if (acceptor.block.canAcceptFluid(acceptor.faceTowardPipe, type) && acceptor.block.storeFluid(type)) {
                return true
            }
        }
        return false
    }
}
