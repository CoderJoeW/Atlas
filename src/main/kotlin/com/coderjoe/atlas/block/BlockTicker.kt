package com.coderjoe.atlas.block

import com.coderjoe.atlas.util.coordinates
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

/**
 * Runs every block's update and ambient effects from a single scheduler task.
 *
 * A timer per block meant thousands of scheduler entries in a large factory, fired in whatever
 * order the scheduler kept them, and every block on the same interval fired on the same tick.
 * Here each job sits in a lane for its interval, in the slot a stable hash of its location picks,
 * so a lane's work is spread evenly across the ticks of its interval and runs in the same order
 * every time. A tick only visits the one slot per lane that is due.
 *
 * Whole-run work that is not owned by any one block - a cable run moving its power - is added
 * with [addSystem] and runs once per interval no matter how many blocks make up the run.
 *
 * Scheduled blocks are also indexed by the chunk they sit in ([scheduledIn]), which is what
 * pausing and resuming a chunk's blocks will key on.
 */
class BlockTicker(private val plugin: JavaPlugin) {
    private class Job(
        val interval: Long,
        val slot: Int,
        val describe: () -> String,
        val action: () -> Unit,
    )

    data class ChunkKey(val world: String?, val x: Int, val z: Int) {
        companion object {
            fun of(location: Location) = ChunkKey(location.world?.name, location.blockX shr 4, location.blockZ shr 4)
        }
    }

    private val lanes = LinkedHashMap<Long, Array<MutableList<Job>>>()
    private val jobsByBlock = HashMap<AtlasBlock, List<Job>>()
    private val blocksByChunk = HashMap<ChunkKey, MutableSet<AtlasBlock>>()
    private var task: BukkitTask? = null

    /** Server ticks this ticker has run since it was created. */
    var currentTick: Long = 0L
        private set

    fun schedule(block: AtlasBlock) {
        unschedule(block)
        val seed = stableHash(block.location)
        val jobs = mutableListOf(job(block.updateIntervalTicks, seed, { "block tick at ${block.location.coordinates}" }, block::runUpdate))
        if (block.effectIntervalTicks > 0) {
            jobs += job(block.effectIntervalTicks, seed, { "block effects at ${block.location.coordinates}" }, block::runEffects)
        }
        jobs.forEach(::add)
        jobsByBlock[block] = jobs
        blocksByChunk.getOrPut(ChunkKey.of(block.location)) { LinkedHashSet() } += block
        ensureRunning()
    }

    fun unschedule(block: AtlasBlock) {
        val jobs = jobsByBlock.remove(block) ?: return
        jobs.forEach(::remove)
        val chunk = ChunkKey.of(block.location)
        val inChunk = blocksByChunk[chunk] ?: return
        inChunk -= block
        if (inChunk.isEmpty()) blocksByChunk -= chunk
    }

    fun isScheduled(block: AtlasBlock): Boolean = block in jobsByBlock

    fun scheduledIn(chunk: ChunkKey): Set<AtlasBlock> = blocksByChunk[chunk].orEmpty()

    /** Runs [action] once every [intervalTicks], in a slot spread by [name] like a block's. */
    fun addSystem(
        name: String,
        intervalTicks: Long,
        action: () -> Unit,
    ) {
        add(job(intervalTicks, name.hashCode(), { name }, action))
        ensureRunning()
    }

    /** Advances one server tick, running every job whose slot is due. One failing job never stops the rest. */
    fun tick() {
        currentTick++
        for ((interval, slots) in lanes.entries.toList()) {
            val due = slots[Math.floorMod(currentTick, interval).toInt()]
            if (due.isEmpty()) continue
            for (job in due.toList()) run(job)
        }
    }

    /** Drops every job and cancels the scheduler task; scheduling anything again restarts it. */
    fun stop() {
        task?.cancel()
        task = null
        lanes.clear()
        jobsByBlock.clear()
        blocksByChunk.clear()
    }

    private fun run(job: Job) {
        try {
            job.action()
        } catch (e: Exception) {
            plugin.logger.warning("Error in ${job.describe()}: ${e.message}")
        }
    }

    private fun job(
        interval: Long,
        seed: Int,
        describe: () -> String,
        action: () -> Unit,
    ): Job {
        val period = interval.coerceAtLeast(1L)
        return Job(period, Math.floorMod(seed.toLong(), period).toInt(), describe, action)
    }

    private fun add(job: Job) {
        val slots = lanes.getOrPut(job.interval) { Array(job.interval.toInt()) { ArrayList() } }
        slots[job.slot] += job
    }

    private fun remove(job: Job) {
        val slots = lanes[job.interval] ?: return
        slots[job.slot].remove(job)
    }

    private fun ensureRunning() {
        if (task != null) return
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable { tick() }, 1L, 1L)
    }

    private fun stableHash(location: Location): Int =
        (location.blockX * 73856093) xor (location.blockY * 19349663) xor (location.blockZ * 83492791)
}
