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

        fun facingFromBlockId(blockId: String): BlockFace? = ID_TO_FACING[blockId]
    }

    override val updateIntervalTicks: Long = 20L // 1 second

    override fun powerUpdate() {
        val registry = PowerBlockRegistry.instance ?: return
        val neighbor = registry.getAdjacentPowerBlock(location, facing) ?: return

        // Pull: if we have space, pull 1 power from the neighbor in our facing direction
        var pulledFrom: PowerBlock? = null
        if (canAcceptPower() && neighbor.hasPower()) {
            val pulled = neighbor.removePower(1)
            if (pulled > 0) {
                addPower(pulled)
                pulledFrom = neighbor
                plugin.logger.info("PowerCable at ${location.blockX},${location.blockY},${location.blockZ} pulled $pulled power from ${neighbor::class.simpleName} (now $currentPower/$maxStorage)")
            }
        }

        // Push: if we have power, push 1 to the neighbor (only if we didn't just pull from it)
        if (hasPower() && pulledFrom == null && neighbor.canAcceptPower()) {
            val removed = removePower(1)
            if (removed > 0) {
                neighbor.addPower(removed)
                plugin.logger.info("PowerCable at ${location.blockX},${location.blockY},${location.blockZ} pushed $removed power to ${neighbor::class.simpleName} (now $currentPower/$maxStorage)")
            }
        }
    }
}
