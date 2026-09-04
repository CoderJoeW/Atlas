package com.coderjoe.atlas.core

import org.bukkit.block.BlockFace

/**
 * A block that spends power without belonging to the power system itself.
 *
 * The fluid pump is the case this exists for. It pays a power cost for every extraction, but it
 * is registered as a fluid block, so a cable asking only the power registry saw nothing beside it
 * and drew no arm toward a pump it was actively feeding.
 *
 * Power is pushed, never pulled: a run finds the consumers on its edge and offers them charge,
 * the same way it feeds a machine that is a power block. A consumer therefore needs somewhere to
 * put what it is given, and spends from that buffer in its own time.
 */
interface PowerConsumer {
    /**
     * Whether power can be pushed in through [face], where [face] points from this block toward
     * the cable. Like a cable's arms this describes the port, not the moment: a pump with no
     * source to work on still connects, because it will draw again the moment it has one.
     */
    fun drawsPowerFrom(face: BlockFace): Boolean

    /** Whether there is room in the buffer for more right now. */
    fun wantsPower(): Boolean

    /** Takes up to [amount] in through [face]. Returns how much was actually taken. */
    fun acceptPower(
        face: BlockFace,
        amount: Int,
    ): Int
}
