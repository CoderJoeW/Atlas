package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace

class MultiPowerCable(location: Location, override val facing: BlockFace) : PowerBlock(location, maxStorage = 10) {

    companion object {
        const val BLOCK_ID = "multi_power_cable"

        val DIRECTIONAL_IDS = mapOf(
            BlockFace.NORTH to "multi_power_cable_north",
            BlockFace.SOUTH to "multi_power_cable_south",
            BlockFace.EAST to "multi_power_cable_east",
            BlockFace.WEST to "multi_power_cable_west",
            BlockFace.UP to "multi_power_cable_up",
            BlockFace.DOWN to "multi_power_cable_down"
        )

        val POWERED_IDS = mapOf(
            BlockFace.NORTH to "multi_power_cable_north_powered",
            BlockFace.SOUTH to "multi_power_cable_south_powered",
            BlockFace.EAST to "multi_power_cable_east_powered",
            BlockFace.WEST to "multi_power_cable_west_powered",
            BlockFace.UP to "multi_power_cable_up_powered",
            BlockFace.DOWN to "multi_power_cable_down_powered"
        )

        val ID_TO_FACING = (DIRECTIONAL_IDS.entries.associate { (face, id) -> id to face } +
                POWERED_IDS.entries.associate { (face, id) -> id to face })

        fun facingFromBlockId(blockId: String): BlockFace? = ID_TO_FACING[blockId]

        val descriptor = BlockDescriptor(
            baseBlockId = BLOCK_ID,
            displayName = "Multi Power Cable",
            description = "Cable - distributes power to all adjacent faces",
            placementType = PlacementType.DIRECTIONAL,
            directionalVariants = DIRECTIONAL_IDS,
            allRegistrableIds = DIRECTIONAL_IDS.values.toList() + POWERED_IDS.values.toList(),
            constructor = { loc, facing -> MultiPowerCable(loc, facing) }
        )
    }

    override val baseBlockId: String = BLOCK_ID

    override val updateIntervalTicks: Long = 20L // 1 second

    override fun getVisualStateBlockId(): String =
        if (currentPower > 0) POWERED_IDS[facing]!!
        else DIRECTIONAL_IDS[facing]!!

    override fun powerUpdate() {
        val registry = PowerBlockRegistry.instance ?: return

        // Pull power from behind (opposite of facing direction)
        val source = registry.getAdjacentPowerBlock(location, facing.oppositeFace)

        if (source != null && canAcceptPower() && source.hasPower()) {
            val remaining = maxStorage - currentPower
            val pulled = source.removePower(minOf(remaining, source.currentPower))
            if (pulled > 0) {
                addPower(pulled)
                plugin.logger.atlasInfo("MultiPowerCable at ${location.blockX},${location.blockY},${location.blockZ} pulled $pulled power (now $currentPower/$maxStorage)")
            }
        }

        // Push 1 power to each adjacent power block on the other 5 faces
        if (!hasPower()) return

        val outputFaces = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)
            .filter { it != facing.oppositeFace }

        for (face in outputFaces) {
            if (!hasPower()) break
            val target = registry.getAdjacentPowerBlock(location, face) ?: continue
            if (target.canAcceptPower()) {
                val pushed = removePower(1)
                if (pushed > 0) {
                    target.addPower(pushed)
                    plugin.logger.atlasInfo("MultiPowerCable at ${location.blockX},${location.blockY},${location.blockZ} pushed $pushed power to ${target::class.simpleName} at ${face.name}")
                }
            }
        }
    }
}
