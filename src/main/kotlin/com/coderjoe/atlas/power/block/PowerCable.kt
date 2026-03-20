package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace

class PowerCable(location: Location, override val facing: BlockFace) : PowerBlock(location, maxStorage = 1) {
    companion object {
        const val BLOCK_ID = "atlas:power_cable"

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Power Cable",
                description = "Cable - transfers power in facing direction",
                placementType = PlacementType.DIRECTIONAL,
                constructor = { loc, facing -> PowerCable(loc, facing) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override val updateIntervalTicks: Long = 20L

    override fun getVisualStateBlockId(): String = BLOCK_ID

    override fun powerUpdate() {
        val registry = PowerBlockRegistry.instance ?: return

        val source = registry.getAdjacentPowerBlock(location, facing.oppositeFace)

        if (source != null && canAcceptPower() && source.hasPower()) {
            val pulled = source.removePower(1)
            if (pulled > 0) {
                addPower(pulled)
                plugin.logger.atlasInfo(
                    "PowerCable at ${location.coordinates} " +
                        "pulled $pulled power from ${source::class.simpleName} (now $currentPower/$maxStorage)",
                )
            }
        }

        updatePoweredState()
    }
}
