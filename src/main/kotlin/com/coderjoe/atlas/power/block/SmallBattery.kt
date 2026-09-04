package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location

/**
 * Passive storage for a power run.
 *
 * Every face behaves the same way: generators and cable runs push charge in, machines and cable
 * runs pull it back out. The battery never reaches for power itself and never picks a direction
 * to send it, so there is no hidden front to line up when placing one.
 */
class SmallBattery(location: Location) : PowerBlock(location, maxStorage = 50) {
    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L
    override val isStorage: Boolean = true

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
                description = "Storage - holds up to 50 power, fills and drains from any side",
                placementType = PlacementType.SIMPLE,
                additionalBlockIds = listOf(BLOCK_ID_LOW, BLOCK_ID_MEDIUM, BLOCK_ID_HIGH, BLOCK_ID_FULL),
                constructor = { loc, _ -> SmallBattery(loc) },
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

    /**
     * Storage does no work of its own - the charge readout still refreshes because the block tick
     * updates the visual state after every [powerUpdate].
     */
    override fun powerUpdate() = Unit
}
