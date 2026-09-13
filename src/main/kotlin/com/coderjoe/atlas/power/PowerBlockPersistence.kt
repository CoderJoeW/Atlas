package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.BlockPersistence
import com.coderjoe.atlas.core.BlockPersister
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.utility.block.MaterialFactory
import org.bukkit.plugin.java.JavaPlugin

class PowerBlockPersistence(plugin: JavaPlugin) : BlockPersister<PowerBlock> {
    private val persistence =
        BlockPersistence<PowerBlock>(
            plugin = plugin,
            fileName = "power_blocks.yml",
            yamlKey = "power_blocks",
            factory = PowerBlockFactory,
            serialize = { block, _ ->
                mutableMapOf<String, Any>(
                    "currentPower" to block.currentPower,
                ).apply {
                    // A factory banks each fluid until the other arrives, and now shows which it
                    // is holding, so dropping that on a restart would visibly undo a half-filled
                    // machine as well as quietly eating the unit a pump already spent power on.
                    if (block is MaterialFactory) {
                        put("hasWater", block.hasWater)
                        put("hasLava", block.hasLava)
                    }
                }
            },
            restore = { block, data ->
                block.currentPower = (data["currentPower"] as? Number)?.toInt() ?: 0
                if (block is MaterialFactory) {
                    block.restoreFluids(
                        water = data["hasWater"] as? Boolean ?: false,
                        lava = data["hasLava"] as? Boolean ?: false,
                    )
                }
            },
        )

    override fun save(registry: BlockRegistry<PowerBlock>) = persistence.save(registry)

    override fun load(registry: BlockRegistry<PowerBlock>) = persistence.load(registry)
}
