package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.Hopper

class ExperienceExtractor(
    location: Location,
    facing: BlockFace = BlockFace.NORTH,
) : PowerBlock(location, maxStorage = 12) {
    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L
    override val baseBlockId: String = BLOCK_ID

    var direction: BlockFace = if (facing == BlockFace.SELF) BlockFace.NORTH else facing

    override val facing: BlockFace get() = direction

    var storedXp: Double = 0.0

    companion object {
        const val BLOCK_ID = "atlas:experience_extractor"
        const val BLOCK_ID_ACTIVE = "atlas:experience_extractor_active"
        const val POWER_PER_EXTRACT = 3
        const val MAX_XP_BUFFER = 10.0
        const val DEFAULT_XP = 0.01

        val XP_VALUES: Map<Material, Double> =
            mapOf(
                // Ores - 1 XP
                Material.COAL_ORE to 1.0,
                Material.DEEPSLATE_COAL_ORE to 1.0,
                Material.IRON_ORE to 1.0,
                Material.DEEPSLATE_IRON_ORE to 1.0,
                Material.COPPER_ORE to 1.0,
                Material.DEEPSLATE_COPPER_ORE to 1.0,
                Material.GOLD_ORE to 1.0,
                Material.DEEPSLATE_GOLD_ORE to 1.0,
                Material.NETHER_GOLD_ORE to 1.0,
                Material.REDSTONE_ORE to 1.0,
                Material.DEEPSLATE_REDSTONE_ORE to 1.0,
                Material.LAPIS_ORE to 1.0,
                Material.DEEPSLATE_LAPIS_ORE to 1.0,
                Material.NETHER_QUARTZ_ORE to 1.0,
                // Raw ores & minerals - 1 XP
                Material.RAW_IRON to 1.0,
                Material.RAW_GOLD to 1.0,
                Material.RAW_COPPER to 1.0,
                Material.COAL to 1.0,
                Material.CHARCOAL to 1.0,
                Material.LAPIS_LAZULI to 1.0,
                Material.REDSTONE to 1.0,
                Material.QUARTZ to 1.0,
                // Cooked food - 2 XP
                Material.COOKED_BEEF to 2.0,
                Material.COOKED_PORKCHOP to 2.0,
                Material.COOKED_CHICKEN to 2.0,
                Material.COOKED_MUTTON to 2.0,
                Material.COOKED_RABBIT to 2.0,
                Material.COOKED_SALMON to 2.0,
                Material.COOKED_COD to 2.0,
                Material.BAKED_POTATO to 2.0,
                // Monster drops - 3 XP
                Material.BONE to 3.0,
                Material.GUNPOWDER to 3.0,
                Material.SPIDER_EYE to 3.0,
                Material.ROTTEN_FLESH to 3.0,
                Material.SLIME_BALL to 3.0,
                Material.MAGMA_CREAM to 3.0,
                Material.PHANTOM_MEMBRANE to 3.0,
                Material.GHAST_TEAR to 3.0,
                // Valuable ores & drops - 5 XP
                Material.DIAMOND_ORE to 5.0,
                Material.DEEPSLATE_DIAMOND_ORE to 5.0,
                Material.EMERALD_ORE to 5.0,
                Material.DEEPSLATE_EMERALD_ORE to 5.0,
                Material.BLAZE_ROD to 5.0,
                Material.ENDER_PEARL to 5.0,
                Material.DIAMOND to 5.0,
                Material.EMERALD to 5.0,
                Material.SHULKER_SHELL to 5.0,
                // Very valuable - 8 XP
                Material.NETHER_STAR to 8.0,
                Material.ENCHANTED_BOOK to 8.0,
            )

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Experience Extractor",
                description = "Extracts XP from items via hopper, outputs Liquid Experience fluid",
                placementType = PlacementType.DIRECTIONAL,
                additionalBlockIds = listOf(BLOCK_ID_ACTIVE),
                constructor = { loc, face -> ExperienceExtractor(loc, face) },
            )
    }

    override fun getVisualStateBlockId(): String =
        when {
            storedXp > 0.0 || currentPower > 0 -> BLOCK_ID_ACTIVE
            else -> BLOCK_ID
        }

    override fun powerUpdate() {
        pullPowerFromNeighbors()

        pushXpFluid()
        pullFromHoppers()

        updatePoweredState()
    }

    private fun pushXpFluid() {
        if (storedXp < 1.0) return

        val fluidRegistry = FluidBlockRegistry.instance ?: return
        val target = fluidRegistry.getAdjacentFluidBlock(location, facing) ?: return

        if (target.storeFluid(FluidType.EXPERIENCE)) {
            storedXp -= 1.0
            plugin.logger.atlasInfo(
                "ExperienceExtractor at ${location.coordinates} " +
                    "pushed 1 experience fluid (buffer: $storedXp)",
            )
        }
    }

    private fun pullFromHoppers() {
        if (storedXp >= MAX_XP_BUFFER) return
        if (currentPower < POWER_PER_EXTRACT) return

        val world = location.world ?: return

        for (face in ADJACENT_FACES) {
            if (storedXp >= MAX_XP_BUFFER) break
            if (currentPower < POWER_PER_EXTRACT) break

            val adjacentBlock =
                world.getBlockAt(
                    location.blockX + face.modX,
                    location.blockY + face.modY,
                    location.blockZ + face.modZ,
                )

            val hopper = adjacentBlock.state as? Hopper ?: continue
            val inventory = hopper.inventory

            for (slot in 0 until inventory.size) {
                if (storedXp >= MAX_XP_BUFFER) break
                if (currentPower < POWER_PER_EXTRACT) break

                val stack = inventory.getItem(slot) ?: continue
                if (stack.type == Material.AIR) continue
                val xpValue = XP_VALUES[stack.type] ?: DEFAULT_XP

                removePower(POWER_PER_EXTRACT)
                storedXp += xpValue

                if (stack.amount > 1) {
                    stack.amount = stack.amount - 1
                    inventory.setItem(slot, stack)
                } else {
                    inventory.setItem(slot, null)
                }

                plugin.logger.atlasInfo(
                    "ExperienceExtractor at ${location.coordinates} " +
                        "consumed ${stack.type.name}, gained $xpValue XP (buffer: $storedXp)",
                )
            }
        }
    }
}
