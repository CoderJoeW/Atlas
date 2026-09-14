package com.coderjoe.atlas.block.power.mine

import com.coderjoe.atlas.block.AtlasBlocks
import com.coderjoe.atlas.block.power.PowerBlock
import com.coderjoe.atlas.block.pushRoundRobinTo
import com.coderjoe.atlas.block.transport.block.ConveyorBelt
import com.coderjoe.atlas.craftengine.CraftEngineHelper
import com.coderjoe.atlas.util.atlasInfo
import com.coderjoe.atlas.util.coordinates
import org.bukkit.Location
import org.bukkit.Material
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
 * Idle and digging are one block definition with a `stage` property rather than two blocks, the
 * shape the factories use for `powered` - but an int rather than a boolean, because a bore takes
 * between 200 and 1000 ticks and has to read as progress rather than as a light switch.
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

    /**
     * Which step of the bore the ore block in the pit is showing: [IDLE_STAGE] while the mine is
     * not drilling, then one of the [DIGGING_STAGES] steps above it, each shrinking the ore a
     * little further. This is the whole progress read - see [drillStageFor].
     */
    var drillStage: Int = IDLE_STAGE
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

    companion object {
        /** The faces a shaft mouth can open toward. The model has no up or down variant. */
        val HORIZONTAL_FACES = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)

        /** The `stage` value for a mine that is not drilling: the ore whole, and unlit. */
        const val IDLE_STAGE = 1

        /**
         * How many steps a bore is shown in, counting up from [IDLE_STAGE] + 1.
         *
         * Every mine config declares `stage` as `range: 1~5` and gives each step its own
         * appearance per facing, so changing this means regenerating all seven. Nothing throws if
         * they drift - the mine just freezes on one appearance - so `MineTest` pins them together.
         */
        const val DIGGING_STAGES = 4
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
                outputFaces = ADJACENT_FACES,
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
            showDrillStage(IDLE_STAGE)
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

        showDrillStage(drillStageFor(drillTicksRemaining))

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
     * How far into the bore [ticksRemaining] leaves the mine, as a `stage` value.
     *
     * Idle is [IDLE_STAGE] rather than a stage of its own kind, so a starved mine reads as doing
     * nothing - which is true. The inherited [updatePoweredState] answers "holds any charge at
     * all" instead, a lie the whole time a mine is fed too slowly: a netherite mine costs 30 a
     * haul, so a trickle leaves it sitting at 1-29 power looking like it is cutting, with the ore
     * lit, while producing nothing.
     *
     * The digging steps divide the cycle evenly, so a bore first shows whole-but-lit, then loses a
     * quarter of the ore at each of 25%, 50% and 75%.
     */
    private fun drillStageFor(ticksRemaining: Long): Int {
        if (!isCutting) return IDLE_STAGE
        val step = ((cycleTicks - ticksRemaining) * DIGGING_STAGES / cycleTicks).toInt()
        return IDLE_STAGE + 1 + step.coerceIn(0, DIGGING_STAGES - 1)
    }

    /**
     * Eats the ore block in the pit away a step at a time as the bore runs down, which is what
     * makes a long cycle read as work in progress rather than a silent countdown.
     *
     * This replaced a fake vanilla break overlay sent with `Player.sendBlockDamage`, which could
     * never have worked: the client draws a crack by re-tessellating the block model at that
     * position with the crumbling texture, and every mine appearance forces `state: barrier` so it
     * costs nothing from the exhausted auto-state pools. A barrier has no model to crumble, so the
     * packets arrived and rendered nothing. That goes for any entity-rendered Atlas block.
     */
    private fun showDrillStage(stage: Int) {
        drillStage = stage
        CraftEngineHelper.setIntProperty(location, "stage", stage)
    }
}
