package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.CraftEngineHelper
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

    /**
     * Whether the last tick completed a haul, which is what the digging appearance shows.
     *
     * Set before the ore is dropped so it reflects the decision rather than the drop's success.
     */
    var isCutting: Boolean = false
        private set

    /** Power drawn per haul. Charged only on a tick that actually produces ore. */
    abstract val powerPerHaul: Int

    /** What a completed bore drops. */
    abstract val output: Material

    companion object {
        /** The faces a shaft mouth can open toward. The model has no up or down variant. */
        val HORIZONTAL_FACES = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)
    }

    /**
     * Where a haul lands: the middle of the block directly above the mine, so it drops clear of
     * the machine and onto a conveyor belt placed in that block.
     */
    internal fun dropLocation(): Location = location.clone().add(0.5, 1.5, 0.5)

    /**
     * A mine never hands power back to the network.
     *
     * Without this it is listed as a source as well as a sink, and on a shared run one mine can
     * siphon another's buffer a unit at a time - a netherite mine banking 29 of the 30 it needs
     * can be drained by a coal mine next door and never complete a bore.
     */
    override fun canOutputToward(face: BlockFace): Boolean = false

    override fun getVisualStateBlockId(): String = baseBlockId

    override fun powerUpdate() {
        pullPowerFromNeighbors()

        // Resolve the world before spending anything. Location.world is a weak reference, and the
        // tick only stops when the block is unregistered - so on a server that unloads a world the
        // mine would keep charging itself for hauls that can never be dropped.
        val world = location.world
        if (world == null) {
            isCutting = false
            showCutting(false)
            return
        }

        val cutting = currentPower >= powerPerHaul
        isCutting = cutting
        if (cutting) {
            removePower(powerPerHaul)
            world.dropItem(dropLocation(), ItemStack(output))
            plugin.logger.atlasInfo(
                "${this::class.simpleName} at ${location.coordinates} " +
                    "produced 1 ${output.name.lowercase()}",
            )
        }

        showCutting(cutting)
    }

    /**
     * Shows the digging state only on the ticks that actually complete a haul.
     *
     * The inherited [updatePoweredState] answers "holds any charge at all", which for a mine is a
     * lie the whole time it is being fed too slowly: a netherite mine costs 30 a haul, so a trickle
     * leaves it sitting at 1-29 power looking like it is cutting, with the ore lit, while producing
     * nothing. Gating on affordability instead means a starved mine reads as idle, which is true.
     */
    private fun showCutting(cutting: Boolean) {
        CraftEngineHelper.setBooleanProperty(location, "powered", cutting)
    }
}
