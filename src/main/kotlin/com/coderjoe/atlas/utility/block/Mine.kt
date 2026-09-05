package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack

/**
 * A mine: a timbered shaft mouth that turns stored power straight into ore.
 *
 * Every mine works the same way and differs only in what it digs, how much power a haul costs and
 * how long the bore takes: the rarer the ore, the slower and thirstier the rig. The machine does
 * not touch the world around it - the shaft is fiction - so a mine can be built anywhere a cable
 * reaches and never runs a deposit dry.
 *
 * The two visual states in the design chart are idle and digging, and they are one block
 * definition with a `powered` property rather than two: [updatePoweredState] flips it, exactly as
 * the factories do.
 */
abstract class Mine(
    location: Location,
    maxStorage: Int,
    facing: BlockFace = BlockFace.NORTH,
) : PowerBlock(location, maxStorage) {
    override val canReceivePower: Boolean = true

    /**
     * Which way the shaft mouth opens.
     *
     * Purely cosmetic - a mine draws power from any side and drops ore straight up - but the
     * portal has a front, so it has to point somewhere. Placement resolves this to the face
     * looking back at the player.
     */
    var direction: BlockFace = if (facing in HORIZONTAL_FACES) facing else BlockFace.NORTH

    override val facing: BlockFace get() = direction

    /** Power drawn per haul. Charged only on a tick that actually produces ore. */
    abstract val powerPerHaul: Int

    /** What a completed bore drops. */
    abstract val output: Material

    companion object {
        /** The faces a shaft mouth can open toward. The model has no up or down variant. */
        val HORIZONTAL_FACES = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)
    }

    /**
     * Where a haul lands: one block above the pad, so it falls clear of the machine and onto
     * whatever the player has run alongside it.
     */
    internal fun dropLocation(): Location = location.clone().add(0.5, 1.5, 0.5)

    override fun getVisualStateBlockId(): String = baseBlockId

    override fun powerUpdate() {
        pullPowerFromNeighbors()

        if (currentPower >= powerPerHaul) {
            removePower(powerPerHaul)
            location.world?.dropItem(dropLocation(), ItemStack(output))
            plugin.logger.atlasInfo(
                "${this::class.simpleName} at ${location.coordinates} " +
                    "produced 1 ${output.name.lowercase()}",
            )
        }

        updatePoweredState()
    }
}
