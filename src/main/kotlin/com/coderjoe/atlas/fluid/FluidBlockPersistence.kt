package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.core.BlockPersistence
import com.coderjoe.atlas.fluid.block.FluidContainer
import org.bukkit.plugin.java.JavaPlugin

class FluidBlockPersistence(plugin: JavaPlugin) {
    private val persistence = BlockPersistence<FluidBlock>(
        plugin = plugin,
        fileName = "fluid_blocks.yml",
        yamlKey = "fluid_blocks",
        factory = FluidBlockFactory,
        serialize = { block, _ ->
            val map = mutableMapOf<String, Any>(
                "fluidType" to block.storedFluid.name
            )
            if (block is FluidContainer) {
                map["storedAmount"] = block.storedAmount
            }
            map
        },
        restore = { block, data ->
            val fluidTypeName = data["fluidType"] as? String ?: "NONE"
            val fluidType = try { FluidType.valueOf(fluidTypeName) } catch (_: Exception) { FluidType.NONE }
            if (block is FluidContainer) {
                val storedAmount = (data["storedAmount"] as? Number)?.toInt()
                if (storedAmount != null) {
                    block.restoreState(fluidType, storedAmount)
                } else {
                    block.storedFluid = fluidType
                }
            } else {
                block.storedFluid = fluidType
            }
        }
    )

    fun save(registry: FluidBlockRegistry) = persistence.save(registry)
    fun load(registry: FluidBlockRegistry) = persistence.load(registry)
}
