package com.coderjoe.atlas.power.persistence

import com.coderjoe.atlas.power.PowerBlockData
import com.coderjoe.atlas.power.PowerBlockFactory
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Handles saving and loading PowerBlock data to/from disk
 */
class PowerBlockPersistence(private val plugin: JavaPlugin) {
    private val dataFile = File(plugin.dataFolder, "power_blocks.yml")

    fun save(registry: PowerBlockRegistry) {
        val config = YamlConfiguration()
        val powerBlocksWithIds = registry.getAllPowerBlocksWithIds()

        plugin.logger.info("Saving ${powerBlocksWithIds.size} power blocks to disk...")

        val blockDataList = mutableListOf<Map<String, Any>>()

        for ((powerBlock, blockId) in powerBlocksWithIds) {
            val data = PowerBlockData.fromPowerBlock(powerBlock, blockId)
            val map = mutableMapOf<String, Any>(
                "blockId" to data.blockId,
                "world" to data.world,
                "x" to data.x,
                "y" to data.y,
                "z" to data.z,
                "currentPower" to data.currentPower
            )
            if (data.facing != null) {
                map["facing"] = data.facing
            }
            blockDataList.add(map)
        }

        config.set("power_blocks", blockDataList)

        try {
            config.save(dataFile)
            plugin.logger.info("Successfully saved ${blockDataList.size} power blocks")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save power blocks: ${e.message}")
            e.printStackTrace()
        }
    }

    fun load(registry: PowerBlockRegistry) {
        if (!dataFile.exists()) {
            plugin.logger.info("No power blocks data file found, starting fresh")
            return
        }

        val config = YamlConfiguration.loadConfiguration(dataFile)
        val blockDataList = config.getMapList("power_blocks")

        plugin.logger.info("Loading ${blockDataList.size} power blocks from disk...")

        var loadedCount = 0
        var failedCount = 0

        for (blockDataMap in blockDataList) {
            try {
                val blockId = blockDataMap["blockId"] as? String ?: continue
                val world = blockDataMap["world"] as? String ?: continue
                val x = blockDataMap["x"] as? Int ?: continue
                val y = blockDataMap["y"] as? Int ?: continue
                val z = blockDataMap["z"] as? Int ?: continue
                val currentPower = blockDataMap["currentPower"] as? Int ?: 0
                val facing = blockDataMap["facing"] as? String

                val data = PowerBlockData(blockId, world, x, y, z, currentPower, facing)
                val location = data.toLocation(plugin)

                if (location == null) {
                    plugin.logger.warning("Failed to load power block at $world $x,$y,$z - world not found")
                    failedCount++
                    continue
                }

                val powerBlock = PowerBlockFactory.createPowerBlock(blockId, location, data.toBlockFace())

                if (powerBlock != null) {
                    powerBlock.currentPower = currentPower
                    registry.registerPowerBlock(powerBlock, blockId)
                    loadedCount++
                } else {
                    plugin.logger.warning("Failed to create power block for ID: $blockId at $x,$y,$z")
                    failedCount++
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load power block: ${e.message}")
                failedCount++
            }
        }

        plugin.logger.info("Loaded $loadedCount power blocks, $failedCount failed")
    }
}
