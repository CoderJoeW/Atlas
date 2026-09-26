package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.deposit.DepositMap
import com.coderjoe.atlas.block.deposit.Ore
import com.coderjoe.atlas.block.deposit.Purity
import org.bukkit.Chunk
import org.bukkit.ChunkSnapshot
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * How much of each [Ore] a chunk held when Atlas first saw it, kept in the chunk's own persistent
 * data so it is saved, moved and deleted along with the chunk.
 *
 * A chunk is counted once and never again. A freshly generated chunk is counted straight out of
 * world generation, before any player can touch it, so what players later dig out or place never
 * changes its record. A chunk that existed before Atlas was installed is counted as it stands the
 * first time it loads.
 *
 * Each ore is recorded on its own, so when [Ore] gains an ore, a chunk that predates it has just that
 * ore counted and added. Its other counts stay the untouched ones from when it was first surveyed.
 *
 * An ore its dimension never generates - quartz in the Overworld, anything in the End - is recorded
 * as none without looking. The rest are counted on [counter], off the main thread, over a snapshot;
 * only taking the snapshot and writing the result happen on it. One worker keeps up with about a
 * thousand chunks a second, and its queue is capped so a pregeneration burst cannot pile up snapshots
 * in memory. A chunk turned away by a full queue, or that unloads before its count lands, is simply
 * counted again the next time it loads.
 */
class ChunkOreSurvey(
    private val plugin: JavaPlugin,
    private val counter: ExecutorService = boundedWorker(),
) : DepositMap {
    private val surveyKey = NamespacedKey(plugin, "ore_survey")
    private val oreKeys = Ore.entries.associateWith { NamespacedKey(plugin, it.name.lowercase()) }
    private val inFlight = mutableSetOf<ChunkId>()

    private data class ChunkId(val world: UUID, val x: Int, val z: Int)

    /** The chunk's recorded ore, or null until every [Ore] in it has been counted. */
    fun oreIn(chunk: Chunk): Map<Ore, Int>? = recordedOre(chunk).takeIf { it.size == Ore.entries.size }

    /** How rich the chunk's deposit of [ore] is, or null if that ore has not been counted there yet. */
    fun purityIn(
        chunk: Chunk,
        ore: Ore,
    ): Purity? =
        chunk.persistentDataContainer
            .get(surveyKey, PersistentDataType.TAG_CONTAINER)
            ?.get(oreKeys.getValue(ore), PersistentDataType.INTEGER)
            ?.let(ore::purityOf)

    private fun recordedOre(chunk: Chunk): Map<Ore, Int> {
        val record = chunk.persistentDataContainer.get(surveyKey, PersistentDataType.TAG_CONTAINER) ?: return emptyMap()
        return Ore.entries
            .mapNotNull { ore -> record.get(oreKeys.getValue(ore), PersistentDataType.INTEGER)?.let { ore to it } }
            .toMap()
    }

    override fun purityAt(
        location: Location,
        ore: Ore,
    ): Purity? = location.world?.let { purityIn(location.chunk, ore) }

    /** Counts whichever ores the chunk has no record of yet, unless a count is already under way. Main thread only. */
    fun survey(chunk: Chunk) {
        val unrecorded = Ore.entries - recordedOre(chunk).keys
        if (unrecorded.isEmpty()) return
        val world = chunk.world
        val (generated, neverGenerated) = unrecorded.partition { it.dimension == world.environment }
        val none = neverGenerated.associateWith { 0 }
        if (generated.isEmpty()) {
            write(chunk, none)
            return
        }

        val id = ChunkId(world.uid, chunk.x, chunk.z)
        if (!inFlight.add(id)) return
        val snapshot = chunk.getChunkSnapshot(false, false, false, false)
        val minY = world.minHeight
        val maxY = world.maxHeight
        try {
            counter.execute {
                val ore = countOre(snapshot, minY, maxY).filterKeys { it in generated } + none
                if (plugin.isEnabled) plugin.server.scheduler.runTask(plugin, Runnable { record(world, id, ore) })
            }
        } catch (_: RejectedExecutionException) {
            inFlight.remove(id)
        }
    }

    /** Abandons any counts still queued; their chunks are counted again the next time they load. */
    fun stop() {
        counter.shutdownNow()
    }

    private fun record(
        world: World,
        id: ChunkId,
        ore: Map<Ore, Int>,
    ) {
        inFlight.remove(id)
        if (world.isChunkLoaded(id.x, id.z)) write(world.getChunkAt(id.x, id.z), ore)
    }

    private fun write(
        chunk: Chunk,
        ore: Map<Ore, Int>,
    ) {
        val chunkData = chunk.persistentDataContainer
        val record =
            chunkData.get(surveyKey, PersistentDataType.TAG_CONTAINER)
                ?: chunkData.adapterContext.newPersistentDataContainer()
        ore.forEach { (kind, count) -> record.set(oreKeys.getValue(kind), PersistentDataType.INTEGER, count) }
        chunkData.set(surveyKey, PersistentDataType.TAG_CONTAINER, record)
    }
}

/** A single daemon worker whose queue holds at most [SURVEY_QUEUE] chunks, refusing any beyond that. */
private fun boundedWorker(): ExecutorService =
    ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue(SURVEY_QUEUE)) {
        Thread(it, "Atlas ore survey").apply { isDaemon = true }
    }

private const val SURVEY_QUEUE = 256

/** Every [Ore] block in [snapshot] between [minY] (inclusive) and [maxY] (exclusive), by ore. */
fun countOre(
    snapshot: ChunkSnapshot,
    minY: Int,
    maxY: Int,
): Map<Ore, Int> {
    val counts = IntArray(Ore.entries.size)
    for (section in 0 until (maxY - minY) / 16) {
        if (snapshot.isSectionEmpty(section)) continue
        val sectionBottom = minY + section * 16
        for (y in sectionBottom until sectionBottom + 16) {
            for (x in 0 until 16) {
                for (z in 0 until 16) {
                    val ore = Ore.of(snapshot.getBlockType(x, y, z)) ?: continue
                    counts[ore.ordinal]++
                }
            }
        }
    }
    return Ore.entries.associateWith { counts[it.ordinal] }
}
