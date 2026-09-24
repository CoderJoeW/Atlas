package com.coderjoe.atlas.block.power.factory

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidConsumer
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.capability.ItemInlet
import com.coderjoe.atlas.block.power.PowerBlock
import com.coderjoe.atlas.block.pushRoundRobinTo
import com.coderjoe.atlas.craftengine.CraftEngineHelper
import com.coderjoe.atlas.util.atlasInfo
import com.coderjoe.atlas.util.coordinates
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack

/**
 * A material factory: turns stored power plus pushed-in water and lava into one item per haul.
 *
 * Cobblestone and obsidian differ only in tier - storage, power cost and what comes out - so both
 * live here and the two subclasses supply nothing but numbers, the same split [com.coderjoe.atlas.block.power.mine.Mine] uses for the
 * seven ore rigs.
 */
abstract class MaterialFactory(
    location: Location,
    maxStorage: Int,
) : PowerBlock(location, maxStorage), FluidConsumer {
    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L

    protected abstract val powerCost: Int
    protected abstract val outputMaterial: Material

    /**
     * A factory never hands power back to the network.
     *
     * Without this it is a sink and a source both, and a run feeding several factories can drain
     * one that has nearly saved up enough for a haul into a neighbour that has not - the same
     * siphon [com.coderjoe.atlas.block.power.mine.Mine.canOutputToward] guards against.
     */
    override fun canOutputToward(face: BlockFace): Boolean = false

    /**
     * A factory has no dedicated inlet, so a pipe against any face reads as plumbed in - the same
     * way [com.coderjoe.atlas.block.power.LavaGenerator] answers this for power.
     */
    override fun drawsFluidFrom(face: BlockFace): Boolean = true

    /** One unit of each fluid is all a haul ever needs, so there is nowhere to put a second. */
    override fun wantsFluid(type: FluidType): Boolean =
        when (type) {
            FluidType.WATER -> !hasWater
            FluidType.LAVA -> !hasLava
            FluidType.NONE -> false
        }

    override fun writeSaveData(data: MutableMap<String, Any>) {
        super.writeSaveData(data)
        data["hasWater"] = hasWater
        data["hasLava"] = hasLava
    }

    override fun readSaveData(data: Map<String, Any?>) {
        super.readSaveData(data)
        hasWater = data["hasWater"] as? Boolean ?: false
        hasLava = data["hasLava"] as? Boolean ?: false
    }

    /**
     * Banks a pushed unit until both fluids are on hand - fluid arrives whenever a pipe network's
     * own tick pushes it, not in step with this factory's tick, so it has to be held rather than
     * used the instant it lands.
     */
    override fun acceptFluid(
        face: BlockFace,
        type: FluidType,
    ): Boolean {
        when (type) {
            FluidType.WATER -> {
                if (hasWater) return false
                hasWater = true
            }
            FluidType.LAVA -> {
                if (hasLava) return false
                hasLava = true
            }
            FluidType.NONE -> return false
        }
        // A run pushes on its own tick, not this factory's, so the porthole is lit here rather
        // than waiting up to a second for the next [powerUpdate] to notice the unit landed
        renderLamps()
        return true
    }

    var hasWater: Boolean = false
        private set

    var hasLava: Boolean = false
        private set

    override fun getVisualStateBlockId(): String = baseBlockId

    /** Round-robins hauls across every attached conveyor belt, so several belts share the output. */
    private var nextBeltIndex: Int = 0

    /**
     * Where a haul lands with nothing attached: the middle of the block directly above the
     * factory, clear of the machine.
     */
    private fun dropLocation(): Location = location.clone().add(0.5, 1.5, 0.5)

    /**
     * Where the next haul lands: an attached conveyor belt if one is this round's turn, round-
     * robining across every face that has one so several belts split the output evenly, or the
     * loose drop above the factory when nothing is attached. Mirrors [com.coderjoe.atlas.block.power.mine.Mine.haulDestination].
     */
    private fun outputDestination(): Location {
        var destination: Location? = null

        nextBeltIndex =
            pushRoundRobinTo(
                outputFaces = ADJACENT_FACES,
                startIndex = nextBeltIndex,
                getAdjacent = { face -> BlockRegistry.active?.getAdjacentBlock(location, face) },
                hasResource = { true },
                isCandidate = { target -> target is ItemInlet },
                tryPush = { target, _ ->
                    destination = (target as ItemInlet).itemDropLocation()
                    true
                },
                stopAfterFirstCandidate = true,
            )

        return destination ?: dropLocation()
    }

    /** Remembered so the block state is only rewritten when one of the three lamps changes. */
    private var renderedLamps: List<Boolean>? = null

    /**
     * The three lamps on every side face: the left porthole for water, the right for lava, and
     * the amber bar under them for power.
     *
     * Each reports only its own ingredient, so a factory part-way to a haul shows which half it is
     * still waiting on rather than lighting both windows together. A haul needs all three at once
     * and spends them in the same tick, so all-lit is a state the block passes through rather than
     * rests in.
     *
     * Power is gated on affording a haul rather than the inherited [updatePoweredState]'s
     * [hasPower] - any charge at all - because the obsidian factory (25 a haul, 50 stored) would
     * otherwise read as ready for most of the time it spends filling up.
     */
    private fun renderLamps() {
        val lamps = listOf(hasWater, hasLava, currentPower >= powerCost)
        if (lamps == renderedLamps) return

        CraftEngineHelper.setBooleanProperties(
            location,
            mapOf("water" to lamps[0], "lava" to lamps[1], "powered" to lamps[2]),
        )
        renderedLamps = lamps
    }

    override fun powerUpdate() {
        pullPowerFromNeighbors()

        if (currentPower >= powerCost && hasWater && hasLava) {
            hasWater = false
            hasLava = false
            removePower(powerCost)

            location.world?.dropItem(outputDestination(), ItemStack(outputMaterial))

            plugin.logger.atlasInfo(
                "${this::class.simpleName} at ${location.coordinates} " +
                    "produced 1 ${outputMaterial.name.lowercase()}",
            )
        }

        // After the haul, not before, so the windows go dark on the tick the fluids are spent
        renderLamps()
    }
}
