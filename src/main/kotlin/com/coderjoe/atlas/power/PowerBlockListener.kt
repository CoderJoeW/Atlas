package com.coderjoe.atlas.power

import com.nexomc.nexo.api.NexoBlocks
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Listens for block placement and breaking to manage PowerBlock lifecycle
 * Automatically detects and registers any block registered with PowerBlockFactory
 */
class PowerBlockListener(
    private val plugin: JavaPlugin,
    private val registry: PowerBlockRegistry
) : Listener {

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        plugin.logger.info("Block placed event triggered at ${event.block.location}")

        val mechanic = NexoBlocks.customBlockMechanic(event.block)
        if (mechanic == null) {
            plugin.logger.info("Not a Nexo custom block")
            return
        }

        val blockId = mechanic.itemID
        plugin.logger.info("Nexo block placed with ID: $blockId")

        plugin.logger.info("Registered power blocks: ${PowerBlockFactory.getRegisteredBlockIds()}")

        if (PowerBlockFactory.isRegistered(blockId)) {
            plugin.logger.info("Power block placed: $blockId at ${event.block.location}")

            val powerBlock = PowerBlockFactory.createPowerBlock(blockId, event.block.location)

            if (powerBlock != null) {
                registry.registerPowerBlock(powerBlock, blockId)
            } else {
                plugin.logger.warning("Failed to create power block for ID: $blockId")
            }
        } else {
            plugin.logger.info("Block ID '$blockId' is not registered as a power block")
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val mechanic = NexoBlocks.customBlockMechanic(event.block)

        if (mechanic != null) {
            plugin.logger.info("Block broken: ${mechanic.itemID} at ${event.block.location}")
        }

        val powerBlock = registry.unregisterPowerBlock(event.block.location)

        if (powerBlock != null) {
            plugin.logger.info("Power block removed with ${powerBlock.currentPower}/${powerBlock.maxStorage} power")
        }
    }
}
