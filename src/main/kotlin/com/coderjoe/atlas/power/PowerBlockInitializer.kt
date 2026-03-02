package com.coderjoe.atlas.power

import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallDrill
import com.coderjoe.atlas.power.block.SmallSolarPanel
import org.bukkit.plugin.java.JavaPlugin

object PowerBlockInitializer {

    fun initialize(plugin: JavaPlugin) {
        plugin.logger.info("PowerBlockInitializer starting...")

        plugin.logger.info("Registering SmallSolarPanel...")
        PowerBlockFactory.register("small_solar_panel") { location, _ ->
            SmallSolarPanel(location)
        }

        plugin.logger.info("Registering SmallDrill...")
        PowerBlockFactory.register("small_drill") { location, _ ->
            SmallDrill(location)
        }

        plugin.logger.info("Registering SmallBattery variants...")
        for (variantId in SmallBattery.ALL_VARIANT_IDS) {
            PowerBlockFactory.register(variantId) { location, facing ->
                SmallBattery(location, facing)
            }
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
