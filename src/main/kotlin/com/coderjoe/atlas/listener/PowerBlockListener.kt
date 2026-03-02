package com.coderjoe.atlas.listener

import com.coderjoe.atlas.power.PowerBlockFactory
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallSolarPanel
import com.coderjoe.atlas.ui.PowerBlockDialog
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
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

        // Handle power_cable base item: swap to directional variant
        if (blockId == PowerCable.BLOCK_ID) {
            val facing = getPlayerFacing(event)
            val variantId = PowerCable.DIRECTIONAL_IDS[facing]
            if (variantId == null) {
                plugin.logger.warning("No directional variant for facing $facing")
                return
            }

            plugin.logger.info("Swapping power_cable to directional variant: $variantId (facing $facing)")

            // Schedule the swap for next tick so it doesn't conflict with the place event
            val location = event.block.location.clone()
            plugin.server.scheduler.runTask(plugin, Runnable {
                // Set to air without using NexoBlocks.remove() to prevent item drops
                location.block.setType(Material.AIR, false)
                NexoBlocks.place(variantId, location)

                val powerBlock = PowerBlockFactory.createPowerBlock(variantId, location, facing)
                if (powerBlock != null) {
                    registry.registerPowerBlock(powerBlock, variantId)
                } else {
                    plugin.logger.warning("Failed to create power block for variant: $variantId")
                }
            })
            return
        }

        // Handle directional variant placed directly (e.g., from Nexo)
        val facing = PowerCable.facingFromBlockId(blockId)
        if (facing != null) {
            plugin.logger.info("Directional power cable placed: $blockId (facing $facing)")
            val powerBlock = PowerBlockFactory.createPowerBlock(blockId, event.block.location, facing)
            if (powerBlock != null) {
                registry.registerPowerBlock(powerBlock, blockId)
            } else {
                plugin.logger.warning("Failed to create power block for ID: $blockId")
            }
            return
        }

        plugin.logger.info("Registered power blocks: ${PowerBlockFactory.getRegisteredBlockIds()}")

        if (PowerBlockFactory.isRegistered(blockId)) {
            plugin.logger.info("Power block placed: $blockId at ${event.block.location}")

            val powerBlock = PowerBlockFactory.createPowerBlock(blockId, event.block.location.clone())

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

            // Manually drop the base item for visual-state variants
            // since Nexo may not handle drops for programmatically-placed blocks
            val baseItemId = when (powerBlock) {
                is PowerCable -> PowerCable.BLOCK_ID
                is SmallSolarPanel -> SmallSolarPanel.BLOCK_ID
                else -> null
            }

            if (baseItemId != null) {
                val itemBuilder = NexoItems.itemFromId(baseItemId)
                if (itemBuilder != null) {
                    val dropLocation = event.block.location.add(0.5, 0.5, 0.5)
                    event.block.world.dropItemNaturally(dropLocation, itemBuilder.build())
                    event.isDropItems = false // prevent any default drops
                } else {
                    plugin.logger.warning("Could not find Nexo item for $baseItemId")
                }
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.player.isSneaking) return // allow placing blocks against power blocks
        val block = event.clickedBlock ?: return
        val powerBlock = registry.getPowerBlock(block.location) ?: return

        PowerBlockDialog.showPowerDialog(event.player, powerBlock)
        event.isCancelled = true
    }

    private fun getPlayerFacing(event: BlockPlaceEvent): BlockFace {
        // Use the face the player clicked on — the cable faces away from the surface
        // e.g., clicking the top of a block places a cable facing UP
        val against = event.blockAgainst.location
        val placed = event.block.location
        val dx = placed.blockX - against.blockX
        val dy = placed.blockY - against.blockY
        val dz = placed.blockZ - against.blockZ

        return when {
            dy > 0 -> BlockFace.UP
            dy < 0 -> BlockFace.DOWN
            dx > 0 -> BlockFace.EAST
            dx < 0 -> BlockFace.WEST
            dz > 0 -> BlockFace.SOUTH
            dz < 0 -> BlockFace.NORTH
            else -> event.player.facing
        }
    }
}
