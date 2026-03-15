package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidMerger
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack

class CobblestoneFactory(location: Location) : PowerBlock(location, maxStorage = 2) {

    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "cobblestone_factory"
        const val BLOCK_ID_ACTIVE = "cobblestone_factory_active"
        const val POWER_COST = 2

        private val ADJACENT_FACES = listOf(
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
        )

        val descriptor = BlockDescriptor(
            baseBlockId = BLOCK_ID,
            displayName = "Cobblestone Factory",
            description = "Machine - consumes $POWER_COST power + water + lava → cobblestone",
            placementType = PlacementType.SIMPLE,
            directionalVariants = emptyMap(),
            allRegistrableIds = listOf(BLOCK_ID, BLOCK_ID_ACTIVE),
            constructor = { loc, _ -> CobblestoneFactory(loc) }
        )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String = when {
        currentPower >= POWER_COST -> BLOCK_ID_ACTIVE
        else -> BLOCK_ID
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

        if (currentPower < POWER_COST) return

        val fluidRegistry = FluidBlockRegistry.instance ?: return

        // Check that BOTH water and lava are available before consuming either
        var waterSource: Pair<com.coderjoe.atlas.fluid.FluidBlock, BlockFace>? = null
        var lavaSource: Pair<com.coderjoe.atlas.fluid.FluidBlock, BlockFace>? = null

        for (face in ADJACENT_FACES) {
            val source = fluidRegistry.getAdjacentFluidBlock(location, face) ?: continue
            if (waterSource == null && hasFluidAvailable(source, face, FluidType.WATER)) {
                waterSource = Pair(source, face)
            } else if (lavaSource == null && hasFluidAvailable(source, face, FluidType.LAVA)) {
                lavaSource = Pair(source, face)
            }
            if (waterSource != null && lavaSource != null) break
        }

        if (waterSource == null || lavaSource == null) return

        // Both available — consume atomically
        pullFluid(waterSource.first, waterSource.second)
        pullFluid(lavaSource.first, lavaSource.second)
        removePower(POWER_COST)

        val world = location.world ?: return
        val dropLocation = location.clone().add(0.5, 1.0, 0.5)
        world.dropItemNaturally(dropLocation, ItemStack(Material.COBBLESTONE))

        plugin.logger.atlasInfo("CobblestoneFactory at ${location.blockX},${location.blockY},${location.blockZ} produced 1 cobblestone")
    }

    private fun hasFluidAvailable(source: com.coderjoe.atlas.fluid.FluidBlock, face: BlockFace, fluidType: FluidType): Boolean {
        return when (source) {
            is FluidPump -> source.canRemoveFluidFrom(face.oppositeFace) && source.storedFluid == fluidType
            is FluidPipe -> source.hasFluid() && source.storedFluid == fluidType
            is FluidContainer -> source.canRemoveFluidFrom(face.oppositeFace) && source.storedFluid == fluidType
            is FluidMerger -> source.hasFluid() && source.storedFluid == fluidType
            else -> false
        }
    }

    private fun pullFluid(source: com.coderjoe.atlas.fluid.FluidBlock, face: BlockFace) {
        when (source) {
            is FluidPump -> source.removeFluid()
            is FluidPipe -> source.removeFluid()
            is FluidContainer -> source.removeFluid()
            is FluidMerger -> source.removeFluid()
        }
    }
}
