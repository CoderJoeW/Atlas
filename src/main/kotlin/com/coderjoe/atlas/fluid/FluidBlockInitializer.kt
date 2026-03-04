package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import org.bukkit.plugin.java.JavaPlugin

object FluidBlockInitializer {

    fun initialize(plugin: JavaPlugin) {
        plugin.logger.info("FluidBlockInitializer starting...")

        plugin.logger.info("Registering FluidPump...")
        FluidBlockFactory.register(FluidPump.BLOCK_ID) { location, _ ->
            FluidPump(location)
        }
        FluidBlockFactory.register(FluidPump.BLOCK_ID_ACTIVE) { location, _ ->
            FluidPump(location)
        }
        FluidBlockFactory.register(FluidPump.BLOCK_ID_ACTIVE_LAVA) { location, _ ->
            FluidPump(location)
        }

        plugin.logger.info("Registering FluidPipe variants...")
        for ((face, variantId) in FluidPipe.DIRECTIONAL_IDS) {
            FluidBlockFactory.register(variantId) { location, facing ->
                FluidPipe(location, facing)
            }
        }
        for ((face, variantId) in FluidPipe.WATER_FILLED_IDS) {
            FluidBlockFactory.register(variantId) { location, facing ->
                FluidPipe(location, facing)
            }
        }
        for ((face, variantId) in FluidPipe.LAVA_FILLED_IDS) {
            FluidBlockFactory.register(variantId) { location, facing ->
                FluidPipe(location, facing)
            }
        }

        val registeredBlocks = FluidBlockFactory.getRegisteredBlockIds()
        plugin.logger.info("Initialized ${registeredBlocks.size} fluid block type(s): ${registeredBlocks.joinToString(", ")}")
        plugin.logger.info("FluidBlockInitializer complete")
    }
}
