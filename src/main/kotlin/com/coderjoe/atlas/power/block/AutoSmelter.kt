package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Item
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack

class AutoSmelter(location: Location, facing: BlockFace = BlockFace.NORTH) : PowerBlock(location, maxStorage = 2) {
    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L
    override val baseBlockId: String = BLOCK_ID

    var direction: BlockFace = if (facing == BlockFace.SELF) BlockFace.NORTH else facing

    override val facing: BlockFace get() = direction

    companion object {
        const val BLOCK_ID = "auto_smelter"
        const val POWER_PER_SMELT = 2
        private const val MOVE_DISTANCE = 1.0

        val DIRECTIONAL_IDS =
            mapOf(
                BlockFace.NORTH to "auto_smelter_north",
                BlockFace.SOUTH to "auto_smelter_south",
                BlockFace.EAST to "auto_smelter_east",
                BlockFace.WEST to "auto_smelter_west",
            )

        val POWERED_IDS =
            mapOf(
                BlockFace.NORTH to "auto_smelter_north_on",
                BlockFace.SOUTH to "auto_smelter_south_on",
                BlockFace.EAST to "auto_smelter_east_on",
                BlockFace.WEST to "auto_smelter_west_on",
            )

        val ID_TO_FACING: Map<String, BlockFace> =
            buildMap {
                DIRECTIONAL_IDS.forEach { (face, id) -> put(id, face) }
                POWERED_IDS.forEach { (face, id) -> put(id, face) }
            }

        val ALL_VARIANT_IDS: List<String> = DIRECTIONAL_IDS.values.toList() + POWERED_IDS.values.toList()

        fun facingFromBlockId(blockId: String): BlockFace? = ID_TO_FACING[blockId]

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Auto Smelter",
                description = "Smelts items passing through, consumes 2 power per item",
                placementType = PlacementType.DIRECTIONAL,
                directionalVariants = DIRECTIONAL_IDS,
                allRegistrableIds = ALL_VARIANT_IDS,
                constructor = { loc, face -> AutoSmelter(loc, face) },
            )

        fun getSmeltingResult(input: ItemStack): ItemStack? {
            return try {
                Bukkit.recipeIterator().asSequence()
                    .filterIsInstance<FurnaceRecipe>()
                    .find { it.inputChoice.test(input) }
                    ?.result
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun getVisualStateBlockId(): String {
        return if (hasPower()) {
            POWERED_IDS[direction]!!
        } else {
            DIRECTIONAL_IDS[direction]!!
        }
    }

    override fun powerUpdate() {
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

        val world = location.world ?: return

        // Scan for items on the belt surface (same as conveyor belt)
        val scanCenter = location.clone().add(0.5, 0.75, 0.5)
        val nearbyItems =
            world.getNearbyEntities(scanCenter, 0.5, 0.75, 0.5)
                .filterIsInstance<Item>()

        if (nearbyItems.isEmpty()) return

        val dx = direction.direction.x * MOVE_DISTANCE
        val dz = direction.direction.z * MOVE_DISTANCE

        for (item in nearbyItems) {
            // Try to smelt the item if we have power
            if (currentPower >= POWER_PER_SMELT) {
                val result = getSmeltingResult(item.itemStack)
                if (result != null) {
                    removePower(POWER_PER_SMELT)
                    val smeltedStack = result.clone()
                    smeltedStack.amount = item.itemStack.amount
                    item.itemStack = smeltedStack
                }
            }

            // Always move items forward (conveyor belt behavior)
            item.teleportAsync(item.location.add(dx, 0.0, dz))
        }
    }
}
