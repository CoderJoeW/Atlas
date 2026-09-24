package com.coderjoe.atlas.block.fluid

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidConsumer
import com.coderjoe.atlas.block.capability.FluidType
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
     * A consumer from another system sitting on the run's edge.
     *
     * Kept apart from [Terminal] because it is not a fluid block: it has nothing to give back and
     * no fluid state to report, so it can only ever be pushed to. Mirrors
     * [com.coderjoe.atlas.block.power.PowerNetwork.ConsumerTerminal].
     */
    data class ConsumerTerminal(
        val block: AtlasBlock,
        val consumer: FluidConsumer,
        val faceTowardPipe: BlockFace,
    )

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
        val providers = LinkedHashMap<String, Terminal>()
        val acceptors = LinkedHashMap<String, Terminal>()

        for (pipe in pipes) {
            for (face in AtlasBlock.ADJACENT_FACES) {
                val neighbor = pipe.neighbor(face) as? FluidBlock ?: continue
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

    /** The blocks on this run's edge that take fluid but are not fluid blocks themselves. */
    fun consumers(): List<ConsumerTerminal> {
        val found = LinkedHashMap<String, ConsumerTerminal>()
        for (pipe in pipes) {
            for (face in AtlasBlock.ADJACENT_FACES) {
                val neighbor = pipe.neighbor(face) ?: continue
                if (neighbor is FluidBlock) continue
                val consumer = neighbor as? FluidConsumer ?: continue

                val back = face.oppositeFace
                if (!consumer.drawsFluidFrom(back)) continue
                found.putIfAbsent(
                    BlockRegistry.locationKey(neighbor.location),
                    ConsumerTerminal(neighbor, consumer, back),
                )
            }
        }
        return found.values.toList()
    }

    /** The fluid a provider on this run has to offer right now, or [FluidType.NONE]. */
    fun availableFluid(): FluidType = terminals().first.firstOrNull { it.block.hasFluid() }?.block?.storedFluid ?: FluidType.NONE

    /**
     * Moves one unit from a provider to whichever acceptor or consumer on the run will take it.
     * Returns what moved, or [FluidType.NONE] if nothing did - which is also what the run renders
     * as.
     */
    fun transfer(): FluidType {
        val (providers, _) = terminals()
        if (providers.isEmpty()) return FluidType.NONE

        for (i in providers.indices) {
            val provider = providers[(nextProviderIndex + i) % providers.size]
            if (provider.block.pushesFluid) continue
            if (!provider.block.hasFluid()) continue

            val offered = provider.block.storedFluid
            if (!canDeliver(offered, excluding = provider.block)) continue

            provider.block.removeFluid()
            if (!deliver(offered, excluding = provider.block)) {
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
     * Takes one unit off the run's providers directly, bypassing [transfer]'s push.
     *
     * This is what [FluidPipe.removeFluid] delegates to - a pipe carries nothing of its own, so
     * anything that queries one as a plain [FluidBlock] (rather than going through a
     * [FluidConsumer] push) needs the network to answer on its behalf.
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

    /**
     * Whether anything on this run would take a unit of [type] right now, counting the consumers
     * on its edge as well as its fluid blocks.
     *
     * [FluidType.NONE] asks only whether the run has somewhere to send things at all, which is
     * what a pipe reports when it is being sized up rather than actually handed a unit.
     *
     * [excluding] drops one block from the search, so a provider the run is draining is never
     * offered its own unit straight back.
     */
    fun canDeliver(
        type: FluidType,
        excluding: FluidBlock? = null,
    ): Boolean {
        val (_, acceptors) = terminals()
        if (acceptors.any { it.block !== excluding && it.block.canAcceptFluid(it.faceTowardPipe, type) }) return true
        if (type == FluidType.NONE) return consumers().isNotEmpty()
        return consumers().any { it.consumer.wantsFluid(type) }
    }

    /**
     * Hands one unit to whichever acceptor or consumer on the run will take it.
     *
     * Fluid blocks are offered it first and consumers only after, so a tank on the run banks what
     * a machine is not ready for rather than the unit being burned on whichever of the two the
     * scan happened to reach first.
     */
    fun deliver(
        type: FluidType,
        excluding: FluidBlock? = null,
    ): Boolean {
        if (type == FluidType.NONE) return false

        val (_, acceptors) = terminals()
        for (acceptor in acceptors) {
            if (acceptor.block === excluding) continue
            if (acceptor.block.canAcceptFluid(acceptor.faceTowardPipe, type) && acceptor.block.storeFluid(type)) {
                return true
            }
        }

        for (consumer in consumers()) {
            if (consumer.consumer.wantsFluid(type) && consumer.consumer.acceptFluid(consumer.faceTowardPipe, type)) {
                return true
            }
        }
        return false
    }
}
