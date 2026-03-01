package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace

class PowerCable(location: Location, val facing: BlockFace) : PowerBlock(location, maxStorage = 1) {

    companion object {
        const val BLOCK_ID = "power_cable"

        val DIRECTIONAL_IDS = mapOf(
            BlockFace.NORTH to "power_cable_north",
            BlockFace.SOUTH to "power_cable_south",
            BlockFace.EAST to "power_cable_east",
            BlockFace.WEST to "power_cable_west",
            BlockFace.UP to "power_cable_up",
            BlockFace.DOWN to "power_cable_down"
        )

        val ID_TO_FACING = DIRECTIONAL_IDS.entries.associate { (face, id) -> id to face }

        val POWERED_IDS = mapOf(
            BlockFace.NORTH to "power_cable_north_powered",
            BlockFace.SOUTH to "power_cable_south_powered",
            BlockFace.EAST to "power_cable_east_powered",
            BlockFace.WEST to "power_cable_west_powered",
            BlockFace.UP to "power_cable_up_powered",
            BlockFace.DOWN to "power_cable_down_powered"
        )

        fun facingFromBlockId(blockId: String): BlockFace? = ID_TO_FACING[blockId]
    }

    override val updateIntervalTicks: Long = 20L // 1 second

    override fun getVisualStateBlockId(): String =
        if (currentPower > 0) POWERED_IDS[facing]!!
        else DIRECTIONAL_IDS[facing]!!

    override fun powerUpdate() {
        val registry = PowerBlockRegistry.instance ?: return

        // The arrow/facing direction shows where power flows TO.
        // Pull from behind (opposite of facing) and push forward (facing direction).
        val source = registry.getAdjacentPowerBlock(location, facing.oppositeFace)
        val destination = registry.getAdjacentPowerBlock(location, facing)

        // Pull from behind: if we have space and the source has power
        if (source != null && canAcceptPower() && source.hasPower()) {
            val pulled = source.removePower(1)
            if (pulled > 0) {
                addPower(pulled)
                plugin.logger.info("PowerCable at ${location.blockX},${location.blockY},${location.blockZ} pulled $pulled power from ${source::class.simpleName} (now $currentPower/$maxStorage)")
            }
        }

        // Push forward: if we have power and the destination can accept it
        if (destination != null && hasPower() && destination.canAcceptPower()) {
            val removed = removePower(1)
            if (removed > 0) {
                destination.addPower(removed)
                plugin.logger.info("PowerCable at ${location.blockX},${location.blockY},${location.blockZ} pushed $removed power to ${destination::class.simpleName} (now $currentPower/$maxStorage)")
            }
        }
    }
}
