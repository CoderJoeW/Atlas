package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.fluid.block.FluidContainer
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class FluidBlockPersistence(private val plugin: JavaPlugin) {
    private val dataFile = File(plugin.dataFolder, "fluid_blocks.yml")

    fun save(registry: FluidBlockRegistry) {
        val config = YamlConfiguration()
        val fluidBlocksWithIds = registry.getAllFluidBlocksWithIds()

        plugin.logger.info("Saving ${fluidBlocksWithIds.size} fluid blocks to disk...")

        val blockDataList = mutableListOf<Map<String, Any>>()

        for ((fluidBlock, blockId) in fluidBlocksWithIds) {
            val data = FluidBlockData.fromFluidBlock(fluidBlock, blockId)
            val map = mutableMapOf<String, Any>(
                "blockId" to data.blockId,
                "world" to data.world,
                "x" to data.x,
                "y" to data.y,
                "z" to data.z,
                "fluidType" to data.fluidType
            )
            if (data.facing != null) {
                map["facing"] = data.facing
            }
            if (data.storedAmount != null) {
                map["storedAmount"] = data.storedAmount
            }
            blockDataList.add(map)
        }

        config.set("fluid_blocks", blockDataList)

        try {
            config.save(dataFile)
            plugin.logger.info("Successfully saved ${blockDataList.size} fluid blocks")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save fluid blocks: ${e.message}")
            e.printStackTrace()
        }
    }

    fun load(registry: FluidBlockRegistry) {
        if (!dataFile.exists()) {
            plugin.logger.info("No fluid blocks data file found, starting fresh")
            return
        }

        val config = YamlConfiguration.loadConfiguration(dataFile)
        val blockDataList = config.getMapList("fluid_blocks")

        plugin.logger.info("Loading ${blockDataList.size} fluid blocks from disk...")

        var loadedCount = 0
        var failedCount = 0

        for (blockDataMap in blockDataList) {
            try {
                val blockId = blockDataMap["blockId"] as? String ?: continue
                val world = blockDataMap["world"] as? String ?: continue
                val x = (blockDataMap["x"] as? Number)?.toInt() ?: continue
                val y = (blockDataMap["y"] as? Number)?.toInt() ?: continue
                val z = (blockDataMap["z"] as? Number)?.toInt() ?: continue
                val fluidType = blockDataMap["fluidType"] as? String ?: "NONE"
                val facing = blockDataMap["facing"] as? String
                val storedAmount = (blockDataMap["storedAmount"] as? Number)?.toInt()

                val data = FluidBlockData(blockId, world, x, y, z, fluidType, facing, storedAmount)
                val location = data.toLocation(plugin)

                if (location == null) {
                    plugin.logger.warning("Failed to load fluid block at $world $x,$y,$z - world not found")
                    failedCount++
                    continue
                }

                val fluidBlock = FluidBlockFactory.createFluidBlock(blockId, location, data.toBlockFace())

                if (fluidBlock != null) {
                    if (fluidBlock is FluidContainer && data.storedAmount != null) {
                        fluidBlock.restoreState(data.toFluidType(), data.storedAmount)
                    } else {
                        fluidBlock.storedFluid = data.toFluidType()
                    }
                    registry.registerFluidBlock(fluidBlock, blockId)
                    loadedCount++
                } else {
                    plugin.logger.warning("Failed to create fluid block for ID: $blockId at $x,$y,$z")
                    failedCount++
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load fluid block: ${e.message}")
                failedCount++
            }
        }

        plugin.logger.info("Loaded $loadedCount fluid blocks, $failedCount failed")
    }
}
