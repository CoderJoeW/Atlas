package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location

class SmallSolarPanel(location: Location) : PowerBlock(location, maxStorage = 1) {
    override val canReceivePower: Boolean = false
    override val updateIntervalTicks: Long = 1200L

    companion object {
        const val BLOCK_ID = "atlas:small_solar_panel"
        const val BLOCK_ID_FULL = "atlas:small_solar_panel_full"

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Small Solar Panel",
                description = "Generator - produces 1 power/min during daytime",
                placementType = PlacementType.SIMPLE,
                additionalBlockIds = listOf(BLOCK_ID_FULL),
                constructor = { loc, _ -> SmallSolarPanel(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String =
        when (currentPower) {
            0 -> BLOCK_ID
            else -> BLOCK_ID_FULL
        }

    override fun powerUpdate() {
        val world = location.world ?: return
        val isDaytime = world.time in 0..12000

        if (isDaytime) {
            val generated = addPower(1)
            if (generated > 0) {
                plugin.logger.atlasInfo(
                    "SmallSolarPanel at ${location.blockX},${location.blockY},${location.blockZ} " +
                        "generated $generated power (now $currentPower/$maxStorage)",
                )
            }
        } else {
            plugin.logger.atlasInfo(
                "SmallSolarPanel at ${location.blockX},${location.blockY},${location.blockZ} " +
                    "is not generating power because it is not daytime.",
            )
        }
    }
}
