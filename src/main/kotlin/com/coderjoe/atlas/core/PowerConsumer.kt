package com.coderjoe.atlas.core

import org.bukkit.block.BlockFace

/**
 * A block that spends power without belonging to the power system itself.
 *
 * The fluid pump is the case this exists for. It pays a power cost for every extraction, but it
 * is registered as a fluid block, so a cable asking only the power registry saw nothing beside it
 * and drew no arm toward a pump it was actively feeding.
 */
interface PowerConsumer {
    /**
     * Whether power can be drawn in through [face], where [face] points from this block toward
     * the cable. Like the cable's own arms this describes the port, not the moment: a pump with
     * no source to work on still connects, because it will draw again the moment it has one.
     */
    fun drawsPowerFrom(face: BlockFace): Boolean
}
