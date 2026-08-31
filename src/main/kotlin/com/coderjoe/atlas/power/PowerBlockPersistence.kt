package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.BlockPersistence
import com.coderjoe.atlas.core.BlockPersister
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.utility.block.ExperienceExtractor
import com.coderjoe.atlas.utility.block.SmallDrill
import org.bukkit.plugin.java.JavaPlugin

class PowerBlockPersistence(plugin: JavaPlugin) : BlockPersister<PowerBlock> {
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
                if (block is ExperienceExtractor) {
                    map["storedXp"] = block.storedXp
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
                if (block is ExperienceExtractor) {
                    block.storedXp = (data["storedXp"] as? Number)?.toDouble() ?: 0.0
                }
            },
        )

    override fun save(registry: BlockRegistry<PowerBlock>) = persistence.save(registry)

    override fun load(registry: BlockRegistry<PowerBlock>) = persistence.load(registry)
}
