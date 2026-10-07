package com.coderjoe.atlas.block.power

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRun
import com.coderjoe.atlas.block.capability.PowerConsumer
import org.bukkit.block.BlockFace

/**
 * One connected run of cable, together with everything hanging off it.
 *
 * Cables carry no charge of their own. Each tick the network takes power straight from the
 * producers on its edge and hands it to the consumers on its edge, so a run moves power end to
 * end in a single tick no matter how long it is, and no cable needs to know which way is
 * "forward". Splitting and merging fall out of the shape of the run for free.
 *
 * The blocks touching the run are found once, when the run is discovered, as [edges]. Which of
 * them give or take power is asked fresh on every call, since that changes as charge moves.
 */
class PowerNetwork(
    val cables: List<PowerCable>,
    private val edges: List<BlockRun.Edge>,
) {
    private companion object {
        /**
         * The smallest charge difference between two batteries that a single unit can usefully
         * close. Below this they are level enough to leave alone - see [canBalance].
         */
        const val MIN_BALANCE_GAP = 2
    }

    /**
     * A block on the edge of the network, paired with the face that points from it back at the
     * cable it touches - the face both [PowerBlock.canOutputToward] and [PowerBlock.canAcceptFrom]
     * are asked about.
     */
    data class Terminal(val block: PowerBlock, val faceTowardCable: BlockFace)

    /**
     * A consumer from another system sitting on the run's edge.
     *
     * Kept apart from [Terminal] because it is not a power block: it has no charge to give back
     * and no storage to report, so it can only ever be pushed to. The fluid pump is the one so
     * far, which is why this is a second list rather than an abstraction over both.
     */
    data class ConsumerTerminal(
        val block: AtlasBlock,
        val consumer: PowerConsumer,
        val faceTowardCable: BlockFace,
    )

    private var nextSourceIndex: Int = 0

    fun terminals(): Pair<List<Terminal>, List<Terminal>> {
        val sources = ArrayList<Terminal>()
        val sinks = ArrayList<Terminal>()
        val seenSources = HashSet<PowerBlock>()
        val seenSinks = HashSet<PowerBlock>()

        for (edge in edges) {
            val block = edge.block as? PowerBlock ?: continue
            val back = edge.faceTowardRun
            if (block.hasPower() && block.canOutputToward(back) && seenSources.add(block)) {
                sources += Terminal(block, back)
            }
            if (block.canAcceptPower() && block.canAcceptFrom(back) && seenSinks.add(block)) {
                sinks += Terminal(block, back)
            }
        }

        return sources to sinks
    }

    /** The blocks on this run's edge that take power but are not power blocks themselves. */
    fun consumers(): List<ConsumerTerminal> {
        val found = ArrayList<ConsumerTerminal>()
        val seen = HashSet<AtlasBlock>()
        for (edge in edges) {
            if (edge.block is PowerBlock) continue
            val consumer = edge.block as? PowerConsumer ?: continue
            if (!consumer.drawsPowerFrom(edge.faceTowardRun) || !seen.add(edge.block)) continue
            found += ConsumerTerminal(edge.block, consumer, edge.faceTowardRun)
        }
        return found
    }

    /** Whether any producer on this run has power to give, whether or not anything is drawing it. */
    fun hasSupply(): Boolean = terminals().first.isNotEmpty()

    /**
     * Moves as much power as the edge blocks will give and take, one unit at a time so that no
     * single consumer can starve the rest. Returns the total moved.
     */
    fun transfer(): Int {
        val (sources, sinks) = terminals()
        return transfer(sources, sinks, consumers())
    }

    /**
     * One network tick: moves what it can, then marks every cable lit or dark.
     *
     * A run is lit whenever a generator on it has charge, not only in the tick power happens to
     * move. Otherwise a full solar panel with nothing drawing from it yet looks exactly like a run
     * with no generator at all. If nothing moved, nothing changed, so the sources found for the
     * transfer still answer that.
     */
    fun tick(): Int {
        val (sources, sinks) = terminals()
        val moved = transfer(sources, sinks, consumers())
        val live = moved > 0 || sources.any { it.block.hasPower() }
        for (cable in cables) cable.carrying = live
        return moved
    }

    private fun transfer(
        sources: List<Terminal>,
        sinks: List<Terminal>,
        consumers: List<ConsumerTerminal>,
    ): Int {
        if (sources.isEmpty() || (sinks.isEmpty() && consumers.isEmpty())) return 0

        var moved = 0
        // an upper bound on the work available, so a refusing pair can never spin forever
        var rounds = sources.sumOf { it.block.currentPower }

        while (rounds-- > 0) {
            var progressed = false

            for (sink in sinks) {
                if (!sink.block.canAcceptPower()) continue

                val source = takeFromNextSource(sources, sink) ?: continue
                val accepted = sink.block.addPowerFrom(sink.faceTowardCable, 1)
                if (accepted > 0) {
                    moved += accepted
                    progressed = true
                } else {
                    // hand the unit back to whoever it came from
                    source.block.addPower(1)
                }
            }

            for (consumer in consumers) {
                if (!consumer.consumer.wantsPower()) continue

                val source = takeFromNextSource(sources, null) ?: continue
                val accepted = consumer.consumer.acceptPower(consumer.faceTowardCable, 1)
                if (accepted > 0) {
                    moved += accepted
                    progressed = true
                } else {
                    source.block.addPower(1)
                }
            }

            if (!progressed) break
        }

        return moved
    }

    /**
     * Takes up to [amount] straight off the run's producers, for consumers that ask a cable for
     * power rather than waiting to be pushed - machines and the fluid pump both work this way.
     */
    fun draw(amount: Int): Int {
        if (amount <= 0) return 0
        val (sources, _) = terminals()
        if (sources.isEmpty()) return 0

        var drawn = 0
        for (source in sources) {
            if (drawn >= amount) break
            drawn += source.block.removePowerToward(source.faceTowardCable, amount - drawn)
        }
        return drawn
    }

    /**
     * Whether a unit may move from [source] to [sink].
     *
     * Anything that is not storage is unrestricted: generators feed, machines are fed. Storage is
     * the awkward case, because a battery is both a source and a sink on every run it touches, so
     * a pair of them would otherwise hand the same unit back and forth forever.
     *
     * Batteries therefore only feed each other **downhill, and only while the gap is worth
     * closing**. A single unit narrows the gap only when it is 2 or more: at a gap of exactly 1
     * the move just swaps which battery is ahead, and the pair would oscillate for as long as the
     * run existed. Requiring a gap of 2 makes every move strictly reduce the difference, so a
     * bank settles level - within one unit - and then stops on its own.
     */
    private fun canBalance(
        source: PowerBlock,
        sink: PowerBlock,
    ): Boolean {
        if (!source.isStorage || !sink.isStorage) return true
        return source.currentPower - sink.currentPower >= MIN_BALANCE_GAP
    }

    /**
     * Debits a single unit from the next source with anything to give, skipping [sink] itself.
     *
     * [sink] is null when the unit is bound for a consumer from another system, which is never
     * also a source and never storage, so neither check applies.
     */
    private fun takeFromNextSource(
        sources: List<Terminal>,
        sink: Terminal?,
    ): Terminal? {
        for (i in sources.indices) {
            val source = sources[(nextSourceIndex + i) % sources.size]
            if (sink != null && source.block === sink.block) continue
            if (sink != null && !canBalance(source.block, sink.block)) continue
            if (source.block.removePowerToward(source.faceTowardCable, 1) > 0) {
                nextSourceIndex = (nextSourceIndex + i + 1) % sources.size
                return source
            }
        }
        return null
    }
}
