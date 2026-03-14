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
        const val BLOCK_ID = "power_merger"

        val DIRECTIONAL_IDS = mapOf(
            BlockFace.NORTH to "power_merger_north",
            BlockFace.SOUTH to "power_merger_south",
            BlockFace.EAST to "power_merger_east",
            BlockFace.WEST to "power_merger_west",
            BlockFace.UP to "power_merger_up",
            BlockFace.DOWN to "power_merger_down"
        )

        val POWERED_IDS = mapOf(
            BlockFace.NORTH to "power_merger_north_powered",
            BlockFace.SOUTH to "power_merger_south_powered",
            BlockFace.EAST to "power_merger_east_powered",
            BlockFace.WEST to "power_merger_west_powered",
            BlockFace.UP to "power_merger_up_powered",
            BlockFace.DOWN to "power_merger_down_powered"
        )

        val ID_TO_FACING = (DIRECTIONAL_IDS.entries.associate { (face, id) -> id to face } +
                POWERED_IDS.entries.associate { (face, id) -> id to face })

        fun facingFromBlockId(blockId: String): BlockFace? = ID_TO_FACING[blockId]

        val descriptor = BlockDescriptor(
            baseBlockId = BLOCK_ID,
            displayName = "Power Merger",
            description = "Cable - merges power from all sides, outputs in facing direction",
            placementType = PlacementType.DIRECTIONAL,
            directionalVariants = DIRECTIONAL_IDS,
            allRegistrableIds = DIRECTIONAL_IDS.values.toList() + POWERED_IDS.values.toList(),
            constructor = { loc, facing -> PowerMerger(loc, facing) }
        )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String =
        if (currentPower > 0) POWERED_IDS[facing]!!
        else DIRECTIONAL_IDS[facing]!!

    override fun powerUpdate() {
        val registry = PowerBlockRegistry.instance ?: return

        // Pull power from all faces except the output (facing) direction
        val inputFaces = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)
            .filter { it != facing }

        for (face in inputFaces) {
            if (currentPower >= maxStorage) break
            val source = registry.getAdjacentPowerBlock(location, face) ?: continue
            if (source.hasPower()) {
                val pulled = source.removePower(1)
                if (pulled > 0) {
                    addPower(pulled)
                    plugin.logger.atlasInfo("PowerMerger at ${location.blockX},${location.blockY},${location.blockZ} pulled $pulled power from ${source::class.simpleName} at ${face.name} (now $currentPower/$maxStorage)")
                }
            }
        }
    }
}
