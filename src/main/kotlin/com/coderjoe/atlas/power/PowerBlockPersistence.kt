package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.BlockPersistence
import com.coderjoe.atlas.utility.block.SmallDrill
import org.bukkit.plugin.java.JavaPlugin

class PowerBlockPersistence(plugin: JavaPlugin) {
    private val persistence =
        BlockPersistence<PowerBlock>(
            plugin = plugin,
            fileName = "power_blocks.yml",
            yamlKey = "power_blocks",
            factory = PowerBlockFactory,
            serialize = { block, _ ->
                val map =
                    mutableMapOf<String, Any>(
                        "currentPower" to block.currentPower,
                    )
                if (block is SmallDrill) {
                    map["enabled"] = block.enabled
                }
                map
            },
            restore = { block, data ->
                block.currentPower = (data["currentPower"] as? Number)?.toInt() ?: 0
                if (block is SmallDrill) {
                    val enabled = data["enabled"] as? Boolean
                    if (enabled != null) {
                        block.enabled = enabled
                    }
                }
            },
        )

    fun save(registry: PowerBlockRegistry) = persistence.save(registry)

    fun load(registry: PowerBlockRegistry) = persistence.load(registry)
}
