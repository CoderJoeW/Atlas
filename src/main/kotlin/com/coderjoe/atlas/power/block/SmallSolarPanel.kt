package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location
import org.bukkit.block.BlockFace

class SmallSolarPanel(location: Location): PowerBlock(location, maxStorage = 1) {

    override val canReceivePower: Boolean = false
    override val updateIntervalTicks: Long = 1200L

    companion object {
        const val BLOCK_ID = "small_solar_panel"

        val descriptor = BlockDescriptor(
            baseBlockId = BLOCK_ID,
            displayName = "Small Solar Panel",
            description = "Generator - produces 1 power/min during daytime",
            placementType = PlacementType.SIMPLE,
            directionalVariants = emptyMap(),
            allRegistrableIds = listOf(BLOCK_ID),
            constructor = { loc, _ -> SmallSolarPanel(loc) }
        )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String = when (currentPower) {
        0 -> "small_solar_panel"
        else -> "small_solar_panel_full"
    }

    override fun powerUpdate() {
        val world = location.world ?: return
        val isDaytime = world.time in 0..12000

        if (isDaytime) {
            val generated = addPower(1)
            if (generated > 0) {
                plugin.logger.atlasInfo("SmallSolarPanel at ${location.blockX},${location.blockY},${location.blockZ} generated $generated power (now $currentPower/$maxStorage)")
            }
        } else {
            plugin.logger.atlasInfo("SmallSolarPanel at ${location.blockX},${location.blockY},${location.blockZ} is not generating power because it is not daytime.")
        }

        // TODO: Implement power transfer to connected blocks
    }
}