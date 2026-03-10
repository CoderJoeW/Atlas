package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace

class PowerCable(location: Location, override val facing: BlockFace) : PowerBlock(location, maxStorage = 1) {

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

        val descriptor = BlockDescriptor(
            baseBlockId = BLOCK_ID,
            displayName = "Power Cable",
            description = "Cable - transfers power in facing direction",
            placementType = PlacementType.DIRECTIONAL,
            directionalVariants = DIRECTIONAL_IDS,
            allRegistrableIds = DIRECTIONAL_IDS.values.toList(),
            constructor = { loc, facing -> PowerCable(loc, facing) }
        )
    }

    override val baseBlockId: String = BLOCK_ID

    override val updateIntervalTicks: Long = 20L // 1 second

    override fun getVisualStateBlockId(): String =
        if (currentPower > 0) POWERED_IDS[facing]!!
        else DIRECTIONAL_IDS[facing]!!

    override fun powerUpdate() {
        val registry = PowerBlockRegistry.instance ?: return

        // Pull network: cables only pull from behind (opposite of facing direction).
        // Downstream cables and consumers pull from us in their own update cycles.
        val source = registry.getAdjacentPowerBlock(location, facing.oppositeFace)

        if (source != null && canAcceptPower() && source.hasPower()) {
            val pulled = source.removePower(1)
            if (pulled > 0) {
                addPower(pulled)
                plugin.logger.atlasInfo("PowerCable at ${location.blockX},${location.blockY},${location.blockZ} pulled $pulled power from ${source::class.simpleName} (now $currentPower/$maxStorage)")
            }
        }
    }
}
