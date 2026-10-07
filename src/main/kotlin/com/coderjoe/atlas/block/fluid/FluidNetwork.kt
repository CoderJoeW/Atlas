package com.coderjoe.atlas.block.fluid

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRun
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
 *
 * The blocks touching the run are found once, when the run is discovered, as [edges]. Which of
 * them give or take fluid is asked fresh on every call, since that changes as fluid moves.
 */
class FluidNetwork(
    val pipes: List<FluidPipe>,
    private val edges: List<BlockRun.Edge>,
) {
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

    private var nextProviderIndex: Int = 0

    /** The blocks touching this run that can hand fluid in, and those that will take it out. */
    fun terminals(): Pair<List<Terminal>, List<Terminal>> {
        val providers = ArrayList<Terminal>()
        val acceptors = ArrayList<Terminal>()
        val seenProviders = HashSet<FluidBlock>()
        val seenAcceptors = HashSet<FluidBlock>()

        for (edge in edges) {
            val block = edge.block as? FluidBlock ?: continue
            val back = edge.faceTowardRun
            if (block.canProvideFluid(back) && seenProviders.add(block)) providers += Terminal(block, back)
            if (block.canAcceptFluid(back) && seenAcceptors.add(block)) acceptors += Terminal(block, back)
        }

        return providers to acceptors
    }

    /** The blocks on this run's edge that take fluid but are not fluid blocks themselves. */
    fun consumers(): List<ConsumerTerminal> {
        val found = ArrayList<ConsumerTerminal>()
        val seen = HashSet<AtlasBlock>()
        for (edge in edges) {
            if (edge.block is FluidBlock) continue
            val consumer = edge.block as? FluidConsumer ?: continue
            if (!consumer.drawsFluidFrom(edge.faceTowardRun) || !seen.add(edge.block)) continue
            found += ConsumerTerminal(edge.block, consumer, edge.faceTowardRun)
        }
        return found
    }

    /** The fluid a provider on this run has to offer right now, or [FluidType.NONE]. */
    fun availableFluid(): FluidType = availableFluid(terminals().first)

    private fun availableFluid(providers: List<Terminal>): FluidType =
        providers.firstOrNull { it.block.hasFluid() }?.block?.storedFluid ?: FluidType.NONE

    /**
     * One network tick: moves a unit if it can, then tells every pipe what the run is carrying.
     *
     * A run reads as carrying whenever a provider on it has something to give, not only in the
     * tick a unit happens to move. Otherwise a full pump with nothing drawing from it yet looks
     * exactly like a run with no source at all. If nothing moved, nothing changed, so the
     * providers found for the transfer still answer that.
     */
    fun tick(): FluidType {
        val (providers, acceptors) = terminals()
        val moved = transfer(providers, acceptors, consumers())
        val flowing = if (moved != FluidType.NONE) moved else availableFluid(providers)
        for (pipe in pipes) pipe.carrying = flowing
        return moved
    }

    /**
     * Moves one unit from a provider to whichever acceptor or consumer on the run will take it.
     * Returns what moved, or [FluidType.NONE] if nothing did - which is also what the run renders
     * as.
     */
    fun transfer(): FluidType {
        val (providers, acceptors) = terminals()
        return transfer(providers, acceptors, consumers())
    }

    private fun transfer(
        providers: List<Terminal>,
        acceptors: List<Terminal>,
        consumers: List<ConsumerTerminal>,
    ): FluidType {
        if (providers.isEmpty()) return FluidType.NONE

        for (i in providers.indices) {
            val provider = providers[(nextProviderIndex + i) % providers.size]
            if (provider.block.pushesFluid) continue
            if (!provider.block.hasFluid()) continue

            val offered = provider.block.storedFluid
            if (!canDeliver(offered, provider.block, acceptors, consumers)) continue

            provider.block.removeFluid()
            if (!deliver(offered, provider.block, acceptors, consumers)) {
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
    ): Boolean = canDeliver(type, excluding, terminals().second, consumers())

    private fun canDeliver(
        type: FluidType,
        excluding: FluidBlock?,
        acceptors: List<Terminal>,
        consumers: List<ConsumerTerminal>,
    ): Boolean {
        if (acceptors.any { it.block !== excluding && it.block.canAcceptFluid(it.faceTowardPipe, type) }) return true
        if (type == FluidType.NONE) return consumers.isNotEmpty()
        return consumers.any { it.consumer.wantsFluid(type) }
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
    ): Boolean = deliver(type, excluding, terminals().second, consumers())

    private fun deliver(
        type: FluidType,
        excluding: FluidBlock?,
        acceptors: List<Terminal>,
        consumers: List<ConsumerTerminal>,
    ): Boolean {
        if (type == FluidType.NONE) return false

        for (acceptor in acceptors) {
            if (acceptor.block === excluding) continue
            if (acceptor.block.canAcceptFluid(acceptor.faceTowardPipe, type) && acceptor.block.storeFluid(type)) {
                return true
            }
        }

        for (consumer in consumers) {
            if (consumer.consumer.wantsFluid(type) && consumer.consumer.acceptFluid(consumer.faceTowardPipe, type)) {
                return true
            }
        }
        return false
    }
}
