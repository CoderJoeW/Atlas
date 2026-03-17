package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace

class PowerMerger(location: Location, override val facing: BlockFace) : PowerBlock(location, maxStorage = 2) {
    override val canReceivePower: Boolean = false
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:power_merger"

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Power Merger",
                description = "Cable - merges power from all sides, outputs in facing direction",
                placementType = PlacementType.DIRECTIONAL,
                constructor = { loc, facing -> PowerMerger(loc, facing) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String = BLOCK_ID

    override fun powerUpdate() {
        val registry = PowerBlockRegistry.instance ?: return

        val inputFaces =
            listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)
                .filter { it != facing }

        for (face in inputFaces) {
            if (currentPower >= maxStorage) break
            val source = registry.getAdjacentPowerBlock(location, face) ?: continue
            if (source.hasPower()) {
                val pulled = source.removePower(1)
                if (pulled > 0) {
                    addPower(pulled)
                    plugin.logger.atlasInfo(
                        "PowerMerger at ${location.blockX},${location.blockY},${location.blockZ} " +
                            "pulled $pulled power from ${source::class.simpleName} at ${face.name} " +
                            "(now $currentPower/$maxStorage)",
                    )
                }
            }
        }

        updatePoweredState()
    }
}
