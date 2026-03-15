package com.coderjoe.atlas.transport.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.transport.TransportBlock
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Item

class ConveyorBelt(location: Location, override val facing: BlockFace) : TransportBlock(location) {
    companion object {
        const val BLOCK_ID = "conveyor_belt"
        private const val MOVE_DISTANCE = 1.0

        val DIRECTIONAL_IDS =
            mapOf(
                BlockFace.NORTH to "conveyor_belt_north",
                BlockFace.SOUTH to "conveyor_belt_south",
                BlockFace.EAST to "conveyor_belt_east",
                BlockFace.WEST to "conveyor_belt_west",
            )

        val ID_TO_FACING = DIRECTIONAL_IDS.entries.associate { (face, id) -> id to face }

        fun facingFromBlockId(blockId: String): BlockFace? = ID_TO_FACING[blockId]

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Conveyor Belt",
                description = "Moves items forward in the facing direction",
                placementType = PlacementType.DIRECTIONAL,
                directionalVariants = DIRECTIONAL_IDS,
                allRegistrableIds = DIRECTIONAL_IDS.values.toList(),
                constructor = { loc, face -> ConveyorBelt(loc, face) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override val updateIntervalTicks: Long = 20L

    override fun getVisualStateBlockId(): String = DIRECTIONAL_IDS[facing]!!

    override fun transportUpdate() {
        val world = location.world ?: return

        // Belt surface is at y + 6/16 (0.375), scan items resting on top
        val scanCenter = location.clone().add(0.5, 0.75, 0.5)
        val nearbyItems =
            world.getNearbyEntities(scanCenter, 0.5, 0.75, 0.5)
                .filterIsInstance<Item>()

        if (nearbyItems.isEmpty()) return

        val dx = facing.direction.x * MOVE_DISTANCE
        val dz = facing.direction.z * MOVE_DISTANCE

        for (item in nearbyItems) {
            item.teleportAsync(item.location.add(dx, 0.0, dz))
        }
    }
}
