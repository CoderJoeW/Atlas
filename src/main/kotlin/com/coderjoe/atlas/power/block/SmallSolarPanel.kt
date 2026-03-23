package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location

class SmallSolarPanel(location: Location) : PowerBlock(location, maxStorage = 4) {
    override val canReceivePower: Boolean = false
    override val updateIntervalTicks: Long = 200L

    companion object {
        const val BLOCK_ID = "atlas:small_solar_panel"
        const val BLOCK_ID_FULL = "atlas:small_solar_panel_full"

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Small Solar Panel",
                description = "Generator - produces 2 power/10s during daytime",
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
            val generated = addPower(2)
            if (generated > 0) {
                plugin.logger.atlasInfo(
                    "SmallSolarPanel at ${location.coordinates} " +
                        "generated $generated power (now $currentPower/$maxStorage)",
                )
            }
        } else {
            plugin.logger.atlasInfo(
                "SmallSolarPanel at ${location.coordinates} " +
                    "is not generating power because it is not daytime.",
            )
        }
    }
}
