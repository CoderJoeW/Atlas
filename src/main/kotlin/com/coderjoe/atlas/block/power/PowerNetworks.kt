package com.coderjoe.atlas.block.power

/**
 * Finds the network a given cable belongs to.
 *
 * Each connected run is discovered by one flood fill and then shared by all of its cables until a
 * cable, or a block against one, is placed or broken - see [PowerGrid]. Asking again in between
 * costs a map lookup rather than another walk of the run.
 */
object PowerNetworks {
    fun networkFor(start: PowerCable): PowerNetwork = start.grid.networkFor(start)
}
