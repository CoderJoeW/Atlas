package com.coderjoe.atlas.fluid

import org.bukkit.block.BlockFace

/**
 * A block that takes fluid without belonging to the fluid system itself.
 *
 * The lava generator and the material factories are the cases this exists for. Each pays for
 * fluid delivered to it, but is registered as a power block, so a pipe asking only the fluid
 * registry saw nothing beside it and drew no arm toward something it was actively feeding.
 *
 * Fluid is pushed, never pulled: a run finds the consumers on its edge and offers them a unit,
 * the same way [com.coderjoe.atlas.core.PowerConsumer] works for power. A consumer therefore
 * needs somewhere to put what it is given, and spends from that in its own time.
 */
interface FluidConsumer {
    /**
     * Whether fluid can be pushed in through [face], where [face] points from this block toward
     * the pipe. Like a pipe's arms this describes the port, not the moment: a machine with
     * nothing to do with fluid yet still connects, because it will accept again the moment it
     * does.
     */
    fun drawsFluidFrom(face: BlockFace): Boolean

    /** Whether this block would take a unit of [type] right now. */
    fun wantsFluid(type: FluidType): Boolean

    /** Takes one unit of [type] in through [face]. Returns whether it was actually taken. */
    fun acceptFluid(
        face: BlockFace,
        type: FluidType,
    ): Boolean
}
