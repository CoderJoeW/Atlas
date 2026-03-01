package com.coderjoe.atlas.power

import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallSolarPanel
import org.bukkit.plugin.java.JavaPlugin

object PowerBlockInitializer {

    fun initialize(plugin: JavaPlugin) {
        plugin.logger.info("PowerBlockInitializer starting...")

        plugin.logger.info("Registering SmallSolarPanel...")
        PowerBlockFactory.register("small_solar_panel") { location, _ ->
            SmallSolarPanel(location)
        }

        plugin.logger.info("Registering PowerCable variants...")
        // Register all 6 directional variants
        for ((face, variantId) in PowerCable.DIRECTIONAL_IDS) {
            PowerBlockFactory.register(variantId) { location, facing ->
                PowerCable(location, facing)
            }
        }

        val registeredBlocks = PowerBlockFactory.getRegisteredBlockIds()
        plugin.logger.info("Initialized ${registeredBlocks.size} power block type(s): ${registeredBlocks.joinToString(", ")}")
        plugin.logger.info("PowerBlockInitializer complete")
    }
}
