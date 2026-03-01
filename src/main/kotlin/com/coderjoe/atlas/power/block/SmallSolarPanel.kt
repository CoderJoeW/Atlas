package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockFactory
import org.bukkit.Location

class SmallSolarPanel(location: Location): PowerBlock(location, maxStorage = 10) {

    override val canReceivePower: Boolean = false

    companion object {
        const val BLOCK_ID = "small_solar_panel"
    }

    override fun powerUpdate() {
        val world = location.world ?: return
        val isDaytime = world.time in 0..12000

        if (isDaytime) {
            val generated = addPower(1)
            if (generated > 0) {
                plugin.logger.info("SmallSolarPanel at ${location.blockX},${location.blockY},${location.blockZ} generated $generated power (now $currentPower/$maxStorage)")
            }
        } else {
            plugin.logger.info("SmallSolarPanel at ${location.blockX},${location.blockY},${location.blockZ} is not generating power because it is not daytime.")
        }

        // TODO: Implement power transfer to connected blocks
    }
}