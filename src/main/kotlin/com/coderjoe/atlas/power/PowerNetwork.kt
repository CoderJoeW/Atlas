package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.AtlasBlock
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.power.block.PowerCable
import org.bukkit.block.BlockFace

/**
 * One connected run of cable, together with everything hanging off it.
 *
 * Cables carry no charge of their own. Each tick the network takes power straight from the
 * producers on its edge and hands it to the consumers on its edge, so a run moves power end to
 * end in a single tick no matter how long it is, and no cable needs to know which way is
 * "forward". Splitting and merging fall out of the shape of the run for free.
 */
class PowerNetwork(val cables: List<PowerCable>) {
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
     * The cable that runs the transfer for the whole network.
     *
     * Every cable in a run discovers the same set, so one of them has to be picked to act, or the
     * transfer would run once per cable. The lowest location key is stable and needs no shared
     * state to agree on.
     */
    val leader: PowerCable? get() = cables.minByOrNull { BlockRegistry.locationKey(it.location) }

    private var nextSourceIndex: Int = 0

    fun terminals(): Pair<List<Terminal>, List<Terminal>> {
        val registry = PowerBlockRegistry.instance ?: return emptyList<Terminal>() to emptyList()
        val sources = LinkedHashMap<String, Terminal>()
        val sinks = LinkedHashMap<String, Terminal>()

        for (cable in cables) {
            for (face in AtlasBlock.ADJACENT_FACES) {
                val neighbor = registry.getAdjacentBlock(cable.location, face) ?: continue
                if (neighbor is PowerCable) continue

                val back = face.oppositeFace
                val key = BlockRegistry.locationKey(neighbor.location)
                if (neighbor.hasPower() && neighbor.canOutputToward(back)) {
                    sources.putIfAbsent(key, Terminal(neighbor, back))
                }
                if (neighbor.canAcceptPower() && neighbor.canAcceptFrom(back)) {
                    sinks.putIfAbsent(key, Terminal(neighbor, back))
                }
            }
        }

        return sources.values.toList() to sinks.values.toList()
    }

    /** Whether any producer on this run has power to give, whether or not anything is drawing it. */
    fun hasSupply(): Boolean = terminals().first.isNotEmpty()

    /**
     * Moves as much power as the edge blocks will give and take, one unit at a time so that no
     * single consumer can starve the rest. Returns the total moved.
     */
    fun transfer(): Int {
        val (sources, sinks) = terminals()
        if (sources.isEmpty() || sinks.isEmpty()) return 0

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

    /** Debits a single unit from the next source with anything to give, skipping [sink] itself. */
    private fun takeFromNextSource(
        sources: List<Terminal>,
        sink: Terminal,
    ): Terminal? {
        for (i in sources.indices) {
            val source = sources[(nextSourceIndex + i) % sources.size]
            if (source.block === sink.block) continue
            if (!canBalance(source.block, sink.block)) continue
            if (source.block.removePowerToward(source.faceTowardCable, 1) > 0) {
                nextSourceIndex = (nextSourceIndex + i + 1) % sources.size
                return source
            }
        }
        return null
    }
}
