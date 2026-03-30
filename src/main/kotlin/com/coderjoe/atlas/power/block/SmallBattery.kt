package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace

class SmallBattery(location: Location, facing: BlockFace) : PowerBlock(location, maxStorage = 50) {
    override val facing: BlockFace = if (facing == BlockFace.SELF) BlockFace.DOWN else facing

    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:small_battery"
        const val BLOCK_ID_LOW = "atlas:small_battery_low"
        const val BLOCK_ID_MEDIUM = "atlas:small_battery_medium"
        const val BLOCK_ID_HIGH = "atlas:small_battery_high"
        const val BLOCK_ID_FULL = "atlas:small_battery_full"

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Small Battery",
                description = "Storage - holds up to 50 power",
                placementType = PlacementType.SIMPLE,
                additionalBlockIds = listOf(BLOCK_ID_LOW, BLOCK_ID_MEDIUM, BLOCK_ID_HIGH, BLOCK_ID_FULL),
                constructor = { loc, facing -> SmallBattery(loc, facing) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    private fun chargeLevel(): Int =
        when (currentPower) {
            0 -> 0
            in 1..12 -> 1
            in 13..25 -> 2
            in 26..37 -> 3
            else -> 4
        }

    override fun getVisualStateBlockId(): String =
        when (chargeLevel()) {
            0 -> BLOCK_ID
            1 -> BLOCK_ID_LOW
            2 -> BLOCK_ID_MEDIUM
            3 -> BLOCK_ID_HIGH
            else -> BLOCK_ID_FULL
        }

    override fun powerUpdate() {
        if (!canAcceptPower()) return
        val registry = PowerBlockRegistry.instance ?: return

        val source = registry.getAdjacentPowerBlock(location, facing.oppositeFace)

        if (source != null && canAcceptPower() && source.hasPower()) {
            val pulled = source.removePower(1)
            if (pulled > 0) {
                addPower(pulled)
                plugin.logger.atlasInfo(
                    "SmallBattery at ${location.coordinates} " +
                        "pulled $pulled power from ${source::class.simpleName} (now $currentPower/$maxStorage)",
                )
            }
        }
    }
}
