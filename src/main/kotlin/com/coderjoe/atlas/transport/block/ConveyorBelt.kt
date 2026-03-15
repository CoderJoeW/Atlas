package com.coderjoe.atlas.transport.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.transport.TransportBlock
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Item

class ConveyorBelt(location: Location, override val facing: BlockFace) : TransportBlock(location) {
    companion object {
        const val BLOCK_ID = "atlas:conveyor_belt"
        private const val MOVE_DISTANCE = 1.0

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Conveyor Belt",
                description = "Moves items forward in the facing direction",
                placementType = PlacementType.DIRECTIONAL,
                constructor = { loc, face -> ConveyorBelt(loc, face) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override val updateIntervalTicks: Long = 20L

    override fun getVisualStateBlockId(): String = BLOCK_ID

    override fun transportUpdate() {
        val world = location.world ?: return

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
