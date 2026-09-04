package com.coderjoe.atlas.transport.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.transport.TransportBlock
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.entity.Item
import org.bukkit.util.Vector

/**
 * Carries dropped items along its facing direction.
 *
 * Items are driven by velocity rather than teleported: the belt tops up their speed every couple
 * of ticks and lets vanilla physics do the rest, so they glide, collide with whatever is in the
 * way on their own, and can be picked up by a hopper underneath like any other dropped item.
 */
class ConveyorBelt(location: Location, override val facing: BlockFace) : TransportBlock(location) {
    companion object {
        const val BLOCK_ID = "atlas:conveyor_belt"

        /** Blocks per tick an item is pushed along the belt. */
        private const val BELT_SPEED = 0.08

        /**
         * How hard an item off the centre line is steered back onto it. Without this, anything
         * that arrives at an angle drifts to the edge and falls off the side of the belt.
         */
        private const val CENTRING = 0.2

        /** Half-extents of the box above the belt that counts as "on" it. */
        private const val REACH_HORIZONTAL = 0.5
        private const val REACH_VERTICAL = 0.75

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Conveyor Belt",
                description = "Moves items forward in the facing direction, into a container if one is ahead",
                placementType = PlacementType.DIRECTIONAL,
                showFacingInDisplayName = true,
                constructor = { loc, face -> ConveyorBelt(loc, face) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    /**
     * Fast enough that items keep a steady speed - dropped items lose momentum to friction, so a
     * once-a-second top-up would read as stuttering rather than a belt.
     */
    override val updateIntervalTicks: Long = 2L

    override fun getVisualStateBlockId(): String = BLOCK_ID

    override fun transportUpdate() {
        val world = location.world ?: return

        val centre = location.clone().add(0.5, 0.75, 0.5)
        val items =
            world.getNearbyEntities(centre, REACH_HORIZONTAL, REACH_VERTICAL, REACH_HORIZONTAL)
                .filterIsInstance<Item>()
        if (items.isEmpty()) return

        val destination = containerAhead()
        for (item in items) {
            if (destination != null && deposit(destination, item)) continue
            ride(item, centre)
        }
    }

    /** Drives [item] along the belt, steering it back toward the centre line as it goes. */
    private fun ride(
        item: Item,
        centre: Location,
    ) {
        val along = facing.direction
        // the belt only steers across its own run; the other axis is the direction of travel
        val acrossX = if (facing.modX != 0) 0.0 else (centre.x - item.location.x) * CENTRING
        val acrossZ = if (facing.modZ != 0) 0.0 else (centre.z - item.location.z) * CENTRING

        // vertical motion is left alone so items still fall onto and settle on the belt
        item.velocity = Vector(along.x * BELT_SPEED + acrossX, item.velocity.y, along.z * BELT_SPEED + acrossZ)
    }

    /** The container the belt points at, if any. */
    private fun containerAhead(): Container? = location.block.getRelative(facing).state as? Container

    /**
     * Tries to put [item] into [container]. Returns whether anything was taken - a full container
     * takes nothing, and the item stays on the belt rather than vanishing.
     */
    private fun deposit(
        container: Container,
        item: Item,
    ): Boolean {
        val offered = item.itemStack
        val leftovers = container.inventory.addItem(offered)
        if (leftovers.isEmpty()) {
            item.remove()
            return true
        }

        val remaining = leftovers.values.first()
        if (remaining.amount == offered.amount) return false

        item.itemStack = remaining
        return true
    }
}
