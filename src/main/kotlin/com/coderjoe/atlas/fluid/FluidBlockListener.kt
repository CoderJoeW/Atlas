package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
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

class FluidBlockListener(
    private val plugin: JavaPlugin,
    private val registry: FluidBlockRegistry
) : Listener {

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val key = FluidBlockRegistry.locationKey(event.block.location)
        if (registry.updatingLocations.contains(key)) return

        val mechanic = NexoBlocks.customBlockMechanic(event.block) ?: return
        val blockId = mechanic.itemID

        // Handle fluid_pump placement
        if (blockId == FluidPump.BLOCK_ID) {
            val location = event.block.location.clone()
            val fluidBlock = FluidBlockFactory.createFluidBlock(blockId, location)
            if (fluidBlock != null) {
                registry.registerFluidBlock(fluidBlock, blockId)
            } else {
                plugin.logger.warning("Failed to create fluid block for: $blockId")
            }
            return
        }

        // Handle fluid_pipe base item: swap to directional variant
        if (blockId == FluidPipe.BLOCK_ID) {
            val facing = getPlayerFacing(event)
            val variantId = FluidPipe.DIRECTIONAL_IDS[facing]
            if (variantId == null) {
                plugin.logger.warning("No directional variant for facing $facing")
                return
            }

            plugin.logger.info("Swapping fluid_pipe to directional variant: $variantId (facing $facing)")

            val location = event.block.location.clone()
            plugin.server.scheduler.runTask(plugin, Runnable {
                location.block.setType(Material.AIR, false)
                NexoBlocks.place(variantId, location)

                val fluidBlock = FluidBlockFactory.createFluidBlock(variantId, location, facing)
                if (fluidBlock != null) {
                    registry.registerFluidBlock(fluidBlock, variantId)
                } else {
                    plugin.logger.warning("Failed to create fluid block for variant: $variantId")
                }
            })
            return
        }

        // Handle directional variant placed directly
        val facing = FluidPipe.facingFromBlockId(blockId)
        if (facing != null) {
            plugin.logger.info("Directional fluid pipe placed: $blockId (facing $facing)")
            val fluidBlock = FluidBlockFactory.createFluidBlock(blockId, event.block.location, facing)
            if (fluidBlock != null) {
                registry.registerFluidBlock(fluidBlock, blockId)
            }
            return
        }

        // Handle any other registered fluid block
        if (FluidBlockFactory.isRegistered(blockId)) {
            val fluidBlock = FluidBlockFactory.createFluidBlock(blockId, event.block.location.clone())
            if (fluidBlock != null) {
                registry.registerFluidBlock(fluidBlock, blockId)
            }
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val key = FluidBlockRegistry.locationKey(event.block.location)
        if (registry.updatingLocations.contains(key)) return

        val fluidBlock = registry.unregisterFluidBlock(event.block.location) ?: return

        plugin.logger.info("Fluid block removed: ${fluidBlock::class.simpleName} at ${event.block.location}")

        val baseItemId = when (fluidBlock) {
            is FluidPump -> FluidPump.BLOCK_ID
            is FluidPipe -> FluidPipe.BLOCK_ID
            else -> null
        }

        if (baseItemId != null) {
            val itemBuilder = NexoItems.itemFromId(baseItemId)
            if (itemBuilder != null) {
                val dropLocation = event.block.location.add(0.5, 0.5, 0.5)
                event.block.world.dropItemNaturally(dropLocation, itemBuilder.build())
                event.isDropItems = false
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.player.isSneaking) return
        val block = event.clickedBlock ?: return
        val fluidBlock = registry.getFluidBlock(block.location) ?: return

        FluidBlockDialog.showFluidDialog(event.player, fluidBlock)
        event.isCancelled = true
    }

    private fun getPlayerFacing(event: BlockPlaceEvent): BlockFace {
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
