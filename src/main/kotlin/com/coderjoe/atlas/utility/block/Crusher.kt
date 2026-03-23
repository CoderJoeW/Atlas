package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack

class Crusher(location: Location, facing: BlockFace = BlockFace.NORTH) : PowerBlock(location, maxStorage = 8) {
    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L
    override val baseBlockId: String = BLOCK_ID

    var direction: BlockFace = if (facing == BlockFace.SELF) BlockFace.NORTH else facing

    override val facing: BlockFace get() = direction

    companion object {
        const val BLOCK_ID = "atlas:crusher"
        const val POWER_PER_CRUSH = 4
        private const val MOVE_DISTANCE = 1.0
        private const val DROP_AMOUNT = 2

        val ORE_TO_DROP: Map<Material, Material> = mapOf(
            Material.IRON_ORE to Material.RAW_IRON,
            Material.GOLD_ORE to Material.RAW_GOLD,
            Material.COPPER_ORE to Material.RAW_COPPER,
            Material.COAL_ORE to Material.COAL,
            Material.DIAMOND_ORE to Material.DIAMOND,
            Material.EMERALD_ORE to Material.EMERALD,
            Material.LAPIS_ORE to Material.LAPIS_LAZULI,
            Material.REDSTONE_ORE to Material.REDSTONE,
            Material.DEEPSLATE_IRON_ORE to Material.RAW_IRON,
            Material.DEEPSLATE_GOLD_ORE to Material.RAW_GOLD,
            Material.DEEPSLATE_COPPER_ORE to Material.RAW_COPPER,
            Material.DEEPSLATE_COAL_ORE to Material.COAL,
            Material.DEEPSLATE_DIAMOND_ORE to Material.DIAMOND,
            Material.DEEPSLATE_EMERALD_ORE to Material.EMERALD,
            Material.DEEPSLATE_LAPIS_ORE to Material.LAPIS_LAZULI,
            Material.DEEPSLATE_REDSTONE_ORE to Material.REDSTONE,
        )

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Crusher",
                description = "Crushes ore blocks into double the ores, consumes $POWER_PER_CRUSH power per item",
                placementType = PlacementType.DIRECTIONAL,
                constructor = { loc, face -> Crusher(loc, face) },
            )
    }

    override fun getVisualStateBlockId(): String = BLOCK_ID

    override fun powerUpdate() {
        pullPowerFromNeighbors()

        val world = location.world ?: return

        val scanCenter = location.clone().add(0.5, 0.75, 0.5)
        val nearbyItems =
            world.getNearbyEntities(scanCenter, 0.5, 0.75, 0.5)
                .filterIsInstance<Item>()

        if (nearbyItems.isEmpty()) return

        val dx = direction.direction.x * MOVE_DISTANCE
        val dz = direction.direction.z * MOVE_DISTANCE

        for (item in nearbyItems) {
            if (currentPower >= POWER_PER_CRUSH) {
                val dropMaterial = ORE_TO_DROP[item.itemStack.type]
                if (dropMaterial != null) {
                    removePower(POWER_PER_CRUSH)
                    val crushedStack = ItemStack(dropMaterial, item.itemStack.amount * DROP_AMOUNT)
                    item.itemStack = crushedStack
                }
            }

            item.teleportAsync(item.location.add(dx, 0.0, dz))
        }

        updatePoweredState()
    }
}
