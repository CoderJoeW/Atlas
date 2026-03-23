package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack

open class SmallDrill(location: Location, facing: BlockFace? = null, maxStorage: Int = 16) : PowerBlock(location, maxStorage) {
    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L

    var miningDirection: BlockFace = if (facing == null || facing == BlockFace.SELF) BlockFace.DOWN else facing
    var enabled: Boolean = true
    internal open val powerCost: Int = 8

    companion object {
        const val BLOCK_ID = "atlas:small_drill"
        private const val MAX_HORIZONTAL_RANGE = 64

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Small Drill",
                description = "Machine - consumes 8 power/s",
                placementType = PlacementType.DIRECTIONAL_OPPOSITE,
                constructor = { loc, facing -> SmallDrill(loc, facing) },
            )
    }

    override val baseBlockId: String = BLOCK_ID
    override val facing: BlockFace get() = miningDirection

    override fun getVisualStateBlockId(): String = BLOCK_ID

    fun toggleEnabled() {
        enabled = !enabled
    }

    override fun powerUpdate() {
        if (!enabled) return

        pullPowerFromNeighbors()

        if (currentPower < powerCost) return

        val world = location.world ?: return

        if (miningDirection == BlockFace.DOWN) {
            for (y in (location.blockY - 1) downTo world.minHeight) {
                val block = world.getBlockAt(location.blockX, y, location.blockZ)

                if (block.type == Material.AIR || block.type == Material.CAVE_AIR || block.type == Material.VOID_AIR) {
                    continue
                }

                if (block.type == Material.BEDROCK) return

                mineBlock(block)
                return
            }
        } else {
            val dx = miningDirection.modX
            val dz = miningDirection.modZ

            for (i in 1..MAX_HORIZONTAL_RANGE) {
                val block =
                    world.getBlockAt(
                        location.blockX + dx * i,
                        location.blockY,
                        location.blockZ + dz * i,
                    )

                if (block.type == Material.AIR || block.type == Material.CAVE_AIR || block.type == Material.VOID_AIR) {
                    continue
                }

                if (block.type == Material.BEDROCK) return

                mineBlock(block)
                return
            }
        }
    }

    internal open fun getBlockDrops(block: org.bukkit.block.Block): Collection<ItemStack> {
        val tool = ItemStack(Material.DIAMOND_PICKAXE)
        return block.getDrops(tool)
    }

    private fun mineBlock(block: org.bukkit.block.Block) {
        val world = location.world ?: return
        removePower(powerCost)
        val drops = getBlockDrops(block)
        block.setType(Material.AIR, false)

        val dropLocation = location.clone().add(0.5, 1.0, 0.5)
        for (item in drops) {
            world.dropItemNaturally(dropLocation, item)
        }
    }
}
