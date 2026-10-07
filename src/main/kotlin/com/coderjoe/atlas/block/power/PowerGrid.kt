package com.coderjoe.atlas.block.power

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.RunCache

/**
 * Every power network in one registry, each kept until the shape of its run changes.
 *
 * The ticker runs [tick] once per interval and each network transfers exactly once in it, however
 * many cables it is made of.
 */
class PowerGrid(registry: BlockRegistry) {
    companion object {
        const val TICK_INTERVAL = 20L

        fun of(registry: BlockRegistry): PowerGrid = registry.system(PowerGrid::class.java, ::PowerGrid)
    }

    private val runs = RunCache(PowerCable::class.java) { PowerNetwork(it.members, it.edges) }

    init {
        registry.ticker.addSystem("power networks", TICK_INTERVAL, ::tick)
    }

    fun networkFor(cable: PowerCable): PowerNetwork = runs.runFor(cable)

    fun networks(): List<PowerNetwork> = runs.runs()

    fun tick() {
        for (network in networks()) network.tick()
    }

    internal fun placed(cable: PowerCable) = runs.placed(cable)

    internal fun removed(cable: PowerCable) = runs.removed(cable)

    internal fun neighborChanged(cable: PowerCable) = runs.invalidate(cable)
}
