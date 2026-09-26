package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.deposit.Ore
import com.coderjoe.atlas.block.deposit.Purity
import com.coderjoe.atlas.testing.MockServer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Chunk
import org.bukkit.ChunkSnapshot
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException

class ChunkOreSurveyTest {
    private lateinit var survey: ChunkOreSurvey
    private lateinit var chunkData: PersistentDataContainer
    private lateinit var written: PersistentDataContainer
    private lateinit var chunk: Chunk
    private val deferredAsync = mutableListOf<Runnable>()
    private lateinit var counter: ExecutorService

    private val surveyKey = NamespacedKey("atlas", "ore_survey")

    private fun oreKey(ore: Ore) = NamespacedKey("atlas", ore.name.lowercase())

    /** A snapshot of a 32-block-tall world, `minY` 0, whose only non-air blocks are [blocks]. */
    private fun snapshot(
        blocks: Map<Triple<Int, Int, Int>, Material>,
        emptySections: Set<Int> = emptySet(),
    ): ChunkSnapshot {
        val snapshot = mockk<ChunkSnapshot>()
        every { snapshot.isSectionEmpty(any()) } answers { firstArg<Int>() in emptySections }
        every { snapshot.getBlockType(any(), any(), any()) } answers {
            blocks[Triple(firstArg(), secondArg(), thirdArg())] ?: Material.AIR
        }
        return snapshot
    }

    /** Gives the chunk a stored record holding exactly [ore], and returns it. */
    private fun storedRecord(ore: Map<Ore, Int>): PersistentDataContainer {
        val record = mockk<PersistentDataContainer>(relaxed = true)
        every { record.get(any<NamespacedKey>(), PersistentDataType.INTEGER) } returns null
        ore.forEach { (kind, count) -> every { record.get(oreKey(kind), PersistentDataType.INTEGER) } returns count }
        every { chunkData.get(surveyKey, PersistentDataType.TAG_CONTAINER) } returns record
        return record
    }

    private val everyOre = Ore.entries.associateWith { 7 }

    @BeforeEach
    fun setup() {
        MockServer.setup()
        every { MockServer.plugin.namespace() } returns "atlas"
        every { MockServer.plugin.isEnabled } returns true
        every { MockServer.scheduler.runTask(any<JavaPlugin>(), any<Runnable>()) } answers {
            secondArg<Runnable>().run()
            mockk(relaxed = true)
        }

        written = mockk(relaxed = true)
        val context = mockk<PersistentDataAdapterContext>()
        every { context.newPersistentDataContainer() } returns written
        chunkData = mockk(relaxed = true)
        every { chunkData.adapterContext } returns context
        every { chunkData.get(surveyKey, PersistentDataType.TAG_CONTAINER) } returns null

        chunk = mockk(relaxed = true)
        every { chunk.world } returns MockServer.world
        every { chunk.x } returns 3
        every { chunk.z } returns -2
        every { chunk.persistentDataContainer } returns chunkData
        every { chunk.getChunkSnapshot(false, false, false, false) } returns
            snapshot(mapOf(Triple(0, 5, 0) to Material.DIAMOND_ORE, Triple(1, 5, 0) to Material.DEEPSLATE_DIAMOND_ORE))
        every { MockServer.world.uid } returns UUID(0, 1)
        every { MockServer.world.environment } returns World.Environment.NORMAL
        every { MockServer.world.minHeight } returns 0
        every { MockServer.world.maxHeight } returns 32
        every { MockServer.world.isChunkLoaded(3, -2) } returns true
        every { MockServer.world.getChunkAt(3, -2) } returns chunk

        counter = mockk(relaxed = true)
        every { counter.execute(any()) } answers { deferredAsync.add(firstArg()) }
        survey = ChunkOreSurvey(MockServer.plugin, counter)
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    private fun finishCounting() {
        deferredAsync.toList().forEach { it.run() }
        deferredAsync.clear()
    }

    @Test
    fun `every block an ore generates as counts toward that ore`() {
        val blocks =
            Ore.entries.flatMap { it.blocks }
                .mapIndexed { i, block -> Triple(i % 16, i / 16, 0) to block }
                .toMap()

        val counts = countOre(snapshot(blocks), 0, 32)

        Ore.entries.forEach { assertEquals(it.blocks.size, counts[it], "$it") }
    }

    @Test
    fun `nether gold ore is not a gold deposit`() {
        val counts = countOre(snapshot(mapOf(Triple(0, 0, 0) to Material.NETHER_GOLD_ORE)), 0, 32)

        assertEquals(0, counts[Ore.GOLD])
    }

    @Test
    fun `counting reads heights relative to the world floor`() {
        val blocks = mapOf(Triple(0, -64, 0) to Material.COAL_ORE, Triple(15, -33, 15) to Material.COAL_ORE)

        assertEquals(2, countOre(snapshot(blocks), -64, -32)[Ore.COAL])
    }

    @Test
    fun `empty sections are skipped`() {
        val blocks = mapOf(Triple(0, 3, 0) to Material.COAL_ORE, Triple(0, 20, 0) to Material.COAL_ORE)

        assertEquals(1, countOre(snapshot(blocks, emptySections = setOf(1)), 0, 32)[Ore.COAL])
    }

    @Test
    fun `an unsurveyed chunk is recorded with every ore`() {
        survey.survey(chunk)
        finishCounting()

        verify { written.set(oreKey(Ore.DIAMOND), PersistentDataType.INTEGER, 2) }
        verify { written.set(oreKey(Ore.COAL), PersistentDataType.INTEGER, 0) }
        verify { chunkData.set(surveyKey, PersistentDataType.TAG_CONTAINER, written) }
    }

    @Test
    fun `a chunk with every ore recorded is never counted again`() {
        storedRecord(everyOre)

        survey.survey(chunk)

        verify(exactly = 0) { chunk.getChunkSnapshot(any(), any(), any(), any()) }
    }

    @Test
    fun `a chunk loaded twice while being counted is counted once`() {
        survey.survey(chunk)
        survey.survey(chunk)

        assertEquals(1, deferredAsync.size)
    }

    @Test
    fun `a chunk that unloads mid-count is left for its next load`() {
        every { MockServer.world.isChunkLoaded(3, -2) } returns false

        survey.survey(chunk)
        finishCounting()
        survey.survey(chunk)

        verify(exactly = 0) { chunkData.set(surveyKey, PersistentDataType.TAG_CONTAINER, any()) }
        assertEquals(1, deferredAsync.size)
    }

    @Test
    fun `an ore added after a chunk was surveyed is counted alone, keeping the original counts`() {
        val record = storedRecord(everyOre - Ore.DIAMOND)

        survey.survey(chunk)
        finishCounting()

        verify { record.set(oreKey(Ore.DIAMOND), PersistentDataType.INTEGER, 2) }
        verify(exactly = 0) { record.set(oreKey(Ore.COAL), any<PersistentDataType<Int, Int>>(), any()) }
        verify { chunkData.set(surveyKey, PersistentDataType.TAG_CONTAINER, record) }
    }

    @Test
    fun `a partly surveyed chunk has no full record but grades the ores it has`() {
        storedRecord(everyOre - Ore.DIAMOND)

        assertNull(survey.oreIn(chunk))
        assertNull(survey.purityIn(chunk, Ore.DIAMOND))
        assertEquals(Ore.COAL.purityOf(7), survey.purityIn(chunk, Ore.COAL))
    }

    @Test
    fun `a recorded chunk reads back its counts`() {
        storedRecord(everyOre + (Ore.IRON to 41) + (Ore.DIAMOND to 0))

        val ore = survey.oreIn(chunk)!!

        assertEquals(41, ore[Ore.IRON])
        assertEquals(0, ore[Ore.DIAMOND])
    }

    @Test
    fun `a recorded chunk is graded against the ore's own thresholds`() {
        storedRecord(everyOre + (Ore.DIAMOND to 40) + (Ore.COAL to 40) + (Ore.EMERALD to 0))

        assertEquals(Purity.PURE, survey.purityIn(chunk, Ore.DIAMOND))
        assertEquals(Purity.POOR, survey.purityIn(chunk, Ore.COAL))
        assertEquals(Purity.BARREN, survey.purityIn(chunk, Ore.EMERALD))
    }

    @Test
    fun `an unsurveyed chunk has no purity`() {
        assertNull(survey.purityIn(chunk, Ore.COAL))
    }

    @Test
    fun `a chunk turned away by a full queue is counted on its next load`() {
        every { counter.execute(any()) } throws RejectedExecutionException()
        survey.survey(chunk)

        every { counter.execute(any()) } answers { deferredAsync.add(firstArg()) }
        survey.survey(chunk)

        assertEquals(1, deferredAsync.size)
    }

    @Test
    fun `an End chunk is recorded as holding no ore without being scanned`() {
        every { MockServer.world.environment } returns World.Environment.THE_END

        survey.survey(chunk)

        verify(exactly = 0) { chunk.getChunkSnapshot(any(), any(), any(), any()) }
        Ore.entries.forEach { verify { written.set(oreKey(it), PersistentDataType.INTEGER, 0) } }
    }

    @Test
    fun `a Nether chunk records the Overworld's ores as none, whatever blocks it holds`() {
        every { MockServer.world.environment } returns World.Environment.NETHER

        survey.survey(chunk)
        finishCounting()

        verify { written.set(oreKey(Ore.DIAMOND), PersistentDataType.INTEGER, 0) }
        verify { written.set(oreKey(Ore.ANCIENT_DEBRIS), PersistentDataType.INTEGER, 0) }
    }
}
