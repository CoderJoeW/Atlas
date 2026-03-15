package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace

class SmallDrill(location: Location, facing: BlockFace? = null) : PowerBlock(location, maxStorage = 10) {
    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L

    var miningDirection: BlockFace = if (facing == null || facing == BlockFace.SELF) BlockFace.DOWN else facing
    var enabled: Boolean = true

    companion object {
        const val BLOCK_ID = "small_drill"
        private const val MAX_HORIZONTAL_RANGE = 64

        val DIRECTIONAL_IDS =
            mapOf(
                BlockFace.NORTH to "small_drill_north",
                BlockFace.SOUTH to "small_drill_south",
                BlockFace.EAST to "small_drill_east",
                BlockFace.WEST to "small_drill_west",
                BlockFace.UP to "small_drill_up",
                BlockFace.DOWN to "small_drill_down",
            )

        val ALL_DIRECTIONAL_IDS: List<String> = DIRECTIONAL_IDS.values.toList()

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Small Drill",
                description = "Machine - consumes 10 power/s",
                placementType = PlacementType.DIRECTIONAL_OPPOSITE,
                directionalVariants = DIRECTIONAL_IDS,
                allRegistrableIds = ALL_DIRECTIONAL_IDS,
                constructor = { loc, facing -> SmallDrill(loc, facing) },
            )
    }

    override val baseBlockId: String = BLOCK_ID
    override val facing: BlockFace get() = miningDirection

    override fun getVisualStateBlockId(): String = DIRECTIONAL_IDS[miningDirection] ?: BLOCK_ID

    fun toggleEnabled() {
        enabled = !enabled
    }

    override fun powerUpdate() {
        if (!enabled) return

        // Pull power from adjacent blocks
        if (canAcceptPower()) {
            val registry = PowerBlockRegistry.instance ?: return
            val neighbors = registry.getAdjacentPowerBlocks(location)
            for (neighbor in neighbors) {
                if (!canAcceptPower()) break
                if (neighbor.hasPower()) {
                    val pulled = neighbor.removePower(1)
                    if (pulled > 0) {
                        addPower(pulled)
                    }
                }
            }
        }

        if (currentPower < 10) return

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

    private fun mineBlock(block: org.bukkit.block.Block) {
        val world = location.world ?: return
        removePower(10)
        val drops = block.getDrops()
        block.setType(Material.AIR, false)

        val dropLocation = location.clone().add(0.5, 1.0, 0.5)
        for (item in drops) {
            world.dropItemNaturally(dropLocation, item)
        }
    }
}
