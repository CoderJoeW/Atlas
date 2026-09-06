package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.AtlasBlock
import com.coderjoe.atlas.core.AtlasBlocks
import com.coderjoe.atlas.core.CraftEngineHelper
import com.coderjoe.atlas.core.pushRoundRobinTo
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.transport.block.ConveyorBelt
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack

/**
 * A mine: a timbered shaft mouth that turns stored power straight into ore.
 *
 * Every mine works the same way and differs only in what it digs, how much power a haul costs and
 * how long the bore takes: the rarer the ore, the slower and thirstier the rig. The machine does
 * not touch the world around it - the shaft is fiction - so a mine can be built anywhere a cable
 * reaches and never runs a deposit dry.
 *
 * The two visual states in the design chart are idle and digging, and they are one block
 * definition with a `powered` property rather than two: [updatePoweredState] flips it, exactly as
 * the factories do.
 */
abstract class Mine(
    location: Location,
    maxStorage: Int,
    facing: BlockFace = BlockFace.NORTH,
) : PowerBlock(location, maxStorage) {
    override val canReceivePower: Boolean = true

    /**
     * Ticks fast so power is pulled every tick regardless of how long a haul takes to drill -
     * [cycleTicks] used to double as this interval, which meant an overpowered mine could only
     * ever pull power once per haul, capping how quickly it could bank up for the next one.
     */
    override val updateIntervalTicks: Long = 20L

    /**
     * Which way the shaft mouth opens.
     *
     * Purely cosmetic - a mine draws power from any side and drops ore straight up - but the
     * portal has a front, so it has to point somewhere. Placement resolves this to the face
     * looking back at the player.
     */
    var direction: BlockFace = if (facing in HORIZONTAL_FACES) facing else BlockFace.NORTH

    override val facing: BlockFace get() = direction

    /**
     * Whether a haul is actively being drilled, which is what the digging appearance shows for
     * the whole [cycleTicks] duration - not just the tick it finishes on.
     */
    var isCutting: Boolean = false
        private set

    /** Power drawn per haul. Charged the moment a haul is committed, not when it completes. */
    abstract val powerPerHaul: Int

    /**
     * How long a committed haul takes to finish, no matter how much power is banked.
     *
     * This is what actually limits a single mine's throughput to one haul per cycle. Power can
     * now be pulled every tick, so without this a mine sitting on a fat battery would otherwise
     * have nothing left to gate it - the whole reason a second mine of the same tier is worth
     * building is that a single one cannot drill faster than this, however much power it is fed.
     */
    abstract val cycleTicks: Long

    /** What a completed bore drops. */
    abstract val output: Material

    /** Round-robins hauls across every attached conveyor belt, so several belts share the output. */
    private var nextBeltIndex: Int = 0

    /** Ticks left on the haul in progress. Zero means the mine is idle, waiting on power. */
    private var drillTicksRemaining: Long = 0L

    /**
     * A stable, fake "cracker" id for [Player.sendBlockDamage] - keyed to this mine's own
     * position rather than a real entity, so repeated updates replace the same overlay instead of
     * layering a new one every tick. [Player.sendBlockDamage]'s own doc notes the id "can be one
     * that does not associate directly with an existing or loaded entity."
     */
    private val breakOverlaySourceId: Int = location.hashCode()

    /**
     * Whether the overlay was showing last tick, so a mine that finishes a haul without enough
     * power for the next one sends one last update clearing its crack, instead of leaving it
     * frozen fully-broken on screen indefinitely.
     */
    private var wasShowingBreakOverlay: Boolean = false

    companion object {
        /** The faces a shaft mouth can open toward. The model has no up or down variant. */
        val HORIZONTAL_FACES = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)

        /** How far away a player still sees the crack overlay update. */
        private const val OVERLAY_VISIBILITY_RADIUS = 48.0
        private const val OVERLAY_VISIBILITY_RADIUS_SQUARED = OVERLAY_VISIBILITY_RADIUS * OVERLAY_VISIBILITY_RADIUS
    }

    /**
     * Where a haul lands: the middle of the block directly above the mine, so it drops clear of
     * the machine and onto a conveyor belt placed in that block.
     */
    internal fun dropLocation(): Location = location.clone().add(0.5, 1.5, 0.5)

    /**
     * Where the next haul lands: an attached conveyor belt if one is this round's turn, round-
     * robining across every face that has one so several belts split the output evenly instead of
     * one hogging every haul, or the loose drop above the mine - the landing spot from before
     * belts could be wired in directly - when nothing is attached.
     */
    internal fun haulDestination(): Location {
        var destination: Location? = null

        nextBeltIndex =
            pushRoundRobinTo(
                outputFaces = AtlasBlock.ADJACENT_FACES,
                startIndex = nextBeltIndex,
                getAdjacent = { face -> AtlasBlocks.adjacent(location, face) },
                hasResource = { true },
                isCandidate = { target -> target is ConveyorBelt },
                tryPush = { target, _ ->
                    destination = (target as ConveyorBelt).location.clone().add(0.5, 0.75, 0.5)
                    true
                },
                stopAfterFirstCandidate = true,
            )

        return destination ?: dropLocation()
    }

    /**
     * A mine never hands power back to the network.
     *
     * Without this it is listed as a source as well as a sink, and on a shared run one mine can
     * siphon another's buffer a unit at a time - a netherite mine banking 29 of the 30 it needs
     * can be drained by a coal mine next door and never complete a bore.
     */
    override fun canOutputToward(face: BlockFace): Boolean = false

    override fun getVisualStateBlockId(): String = baseBlockId

    override fun powerUpdate() {
        pullPowerFromNeighbors()

        // Resolve the world before spending anything. Location.world is a weak reference, and the
        // tick only stops when the block is unregistered - so on a server that unloads a world the
        // mine would keep charging itself for hauls that can never be dropped.
        val world = location.world
        if (world == null) {
            isCutting = false
            showCutting(false)
            return
        }

        var completedHaul = false
        if (drillTicksRemaining > 0) {
            drillTicksRemaining -= updateIntervalTicks
            if (drillTicksRemaining <= 0) {
                drillTicksRemaining = 0
                completedHaul = true
            }
        }

        // Falls through from a haul that just finished this same tick, so a fully powered mine
        // starts its next drill immediately instead of idling for one tick between hauls.
        if (drillTicksRemaining <= 0) {
            isCutting =
                if (currentPower >= powerPerHaul) {
                    removePower(powerPerHaul)
                    drillTicksRemaining = cycleTicks
                    true
                } else {
                    false
                }
        }

        showCutting(isCutting)
        updateBreakOverlay(world)

        // Resolved after every state transition above, so a failure here (or a future change
        // that makes dropping fallible) can never leave the mine's own bookkeeping half-applied.
        if (completedHaul) {
            world.dropItem(haulDestination(), ItemStack(output))
            plugin.logger.atlasInfo(
                "${this::class.simpleName} at ${location.coordinates} " +
                    "produced 1 ${output.name.lowercase()}",
            )
        }
    }

    /**
     * Shows the digging state for as long as a haul is actively being drilled, not just the tick
     * it completes on.
     *
     * The inherited [updatePoweredState] answers "holds any charge at all", which for a mine is a
     * lie the whole time it is being fed too slowly: a netherite mine costs 30 a haul, so a trickle
     * leaves it sitting at 1-29 power looking like it is cutting, with the ore lit, while producing
     * nothing. Gating on an active drill instead means a starved mine reads as idle, which is true.
     */
    private fun showCutting(cutting: Boolean) {
        CraftEngineHelper.setBooleanProperty(location, "powered", cutting)
    }

    /**
     * Fakes the vanilla break-progress crack texture over the mine's own block, climbing from
     * bare to fully cracked across the drill's duration so a haul in progress reads as a player
     * actively chipping at the ore rather than a machine quietly ticking a timer down. Skips
     * sending anything while idle, except the one update that clears an already-shown crack the
     * tick a finished haul leaves the mine without enough power to start another.
     */
    private fun updateBreakOverlay(world: World) {
        if (!isCutting && !wasShowingBreakOverlay) return

        val progress = if (isCutting) 1f - (drillTicksRemaining.toFloat() / cycleTicks.toFloat()) else 0f

        for (player in world.players) {
            if (player.location.distanceSquared(location) <= OVERLAY_VISIBILITY_RADIUS_SQUARED) {
                player.sendBlockDamage(location, progress, breakOverlaySourceId)
            }
        }

        wasShowingBreakOverlay = isCutting
    }
}
