package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace

class SmallBattery(location: Location, facing: BlockFace) : PowerBlock(location, maxStorage = 10) {

    override val facing: BlockFace = if (facing == BlockFace.SELF) BlockFace.DOWN else facing

    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "small_battery"

        val CHARGE_VARIANT_IDS = mapOf(
            0 to "small_battery",
            1 to "small_battery_low",
            2 to "small_battery_medium",
            3 to "small_battery_full"
        )

        val ALL_VARIANT_IDS: List<String> = CHARGE_VARIANT_IDS.values.toList()

        val descriptor = BlockDescriptor(
            baseBlockId = BLOCK_ID,
            displayName = "Small Battery",
            description = "Storage - holds up to 10 power",
            placementType = PlacementType.SIMPLE,
            directionalVariants = emptyMap(),
            allRegistrableIds = ALL_VARIANT_IDS,
            constructor = { loc, facing -> SmallBattery(loc, facing) }
        )
    }

    override val baseBlockId: String = BLOCK_ID

    private fun chargeLevel(): Int = when (currentPower) {
        0 -> 0
        in 1..3 -> 1
        in 4..7 -> 2
        else -> 3
    }

    override fun getVisualStateBlockId(): String =
        CHARGE_VARIANT_IDS[chargeLevel()]!!

    override fun powerUpdate() {
        if (!canAcceptPower()) return
        val registry = PowerBlockRegistry.instance ?: return

        // Pull from behind (opposite of facing), same as power cables
        val source = registry.getAdjacentPowerBlock(location, facing.oppositeFace)

        if (source != null && canAcceptPower() && source.hasPower()) {
            val pulled = source.removePower(1)
            if (pulled > 0) {
                addPower(pulled)
                plugin.logger.info("SmallBattery at ${location.blockX},${location.blockY},${location.blockZ} pulled $pulled power from ${source::class.simpleName} (now $currentPower/$maxStorage)")
            }
        }
    }
}
