package com.coderjoe.atlas.data

import com.coderjoe.atlas.AtlasBlockTypes
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.fluid.FluidPipe
import com.coderjoe.atlas.block.fluid.FluidPump
import com.coderjoe.atlas.block.power.PowerCable
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.block.power.factory.CobblestoneFactory
import com.coderjoe.atlas.block.transport.ConveyorBelt
import com.coderjoe.atlas.testing.MockServer
import io.mockk.every
import io.mockk.mockk
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class BlockPersistenceTest {
    private lateinit var registry: BlockRegistry
    private lateinit var persistence: BlockPersistence

    @BeforeEach
    fun setup() {
        MockServer.setup()
        registry = BlockRegistry(MockServer.plugin)
        persistence = BlockPersistence(MockServer.plugin, AtlasBlockTypes.catalog)
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    private fun file(name: String) = File(MockServer.dataFolder, name)

    private fun reload(): BlockRegistry {
        val loaded = BlockRegistry(MockServer.plugin)
        persistence.load(loaded)
        return loaded
    }

    @Test
    fun `save 0 blocks creates the file with an empty list`() {
        assertTrue(persistence.save(registry))
        assertTrue(file(BlockPersistence.FILE_NAME).exists())
        assertEquals(0, reload().getAllBlocks().size)
    }

    @Test
    fun `save stamps the schema version`() {
        persistence.save(registry)

        val config = YamlConfiguration.loadConfiguration(file(BlockPersistence.FILE_NAME))
        assertEquals(BlockPersistence.VERSION, config.getInt("version"))
    }

    @Test
    fun `load from missing file does not error`() {
        val loadRegistry = BlockRegistry(MockServer.plugin)
        assertDoesNotThrow { persistence.load(loadRegistry) }
        assertEquals(0, loadRegistry.getAllBlocks().size)
    }

    @Test
    fun `one file holds every family`() {
        registry.track(SmallBattery(MockServer.createLocation()), SmallBattery.BLOCK_ID)
        registry.track(FluidPipe(MockServer.createLocation(x = 1.0)), FluidPipe.BLOCK_ID)
        registry.track(ConveyorBelt(MockServer.createLocation(x = 2.0), BlockFace.EAST), ConveyorBelt.BLOCK_ID)

        persistence.save(registry)

        val ids = reload().getAllBlocksWithIds().map { it.second }.toSet()
        assertEquals(setOf(SmallBattery.BLOCK_ID, FluidPipe.BLOCK_ID, ConveyorBelt.BLOCK_ID), ids)
    }

    @Test
    fun `solar panel round-trip preserves id and power`() {
        val panel = SmallSolarPanel(MockServer.createLocation(1.0, 64.0, 2.0))
        panel.currentPower = 1
        registry.track(panel, "atlas:small_solar_panel")

        persistence.save(registry)

        val loaded = reload().getAllBlocksWithIds()
        assertEquals(1, loaded.size)
        assertEquals("atlas:small_solar_panel", loaded[0].second)
        assertEquals(1, assertInstanceOf(SmallSolarPanel::class.java, loaded[0].first).currentPower)
    }

    @Test
    fun `cable round-trip comes back as a cable`() {
        registry.track(PowerCable(MockServer.createLocation()), "atlas:power_cable")

        persistence.save(registry)

        assertInstanceOf(PowerCable::class.java, reload().getAllBlocks().first())
    }

    @Test
    fun `a factory's banked fluids persist across a restart`() {
        // The two portholes report these, so losing them on a restart would visibly undo a
        // half-filled machine as well as eating a unit a pump already spent power to lift.
        val factory = CobblestoneFactory(MockServer.createLocation())
        factory.currentPower = 2
        factory.acceptFluid(BlockFace.WEST, FluidType.WATER)
        registry.track(factory, "atlas:cobblestone_factory")

        persistence.save(registry)

        val loaded = reload().getAllBlocks().first() as CobblestoneFactory
        assertEquals(2, loaded.currentPower)
        assertTrue(loaded.hasWater, "the banked water should come back")
        assertFalse(loaded.hasLava, "lava was never fed, so it must not come back")
    }

    @Test
    fun `battery round-trip preserves power and stores no facing`() {
        val battery = SmallBattery(MockServer.createLocation(5.0, 64.0, 3.0))
        battery.currentPower = 7
        registry.track(battery, "atlas:small_battery")

        persistence.save(registry)

        val loaded = assertInstanceOf(SmallBattery::class.java, reload().getAllBlocks().first())
        // Storage is omnidirectional, so there is no facing to persist and nothing to restore.
        assertEquals(BlockFace.SELF, loaded.facing)
        assertEquals(7, loaded.currentPower)
    }

    @Test
    fun `pump round-trip preserves each stored fluid`() {
        for ((x, fluid) in FluidType.entries.withIndex()) {
            val pump = FluidPump(MockServer.createLocation(x = x.toDouble()))
            pump.storeFluid(fluid)
            registry.track(pump, "atlas:fluid_pump")
        }

        persistence.save(registry)

        val loaded = reload().getAllBlocks().map { assertInstanceOf(FluidPump::class.java, it) }
        for (pump in loaded) {
            assertEquals(FluidType.entries[pump.location.blockX], pump.storedFluid)
        }
    }

    @Test
    fun `conveyor belt round-trip preserves its facing`() {
        registry.track(ConveyorBelt(MockServer.createLocation(), BlockFace.EAST), ConveyorBelt.BLOCK_ID)

        persistence.save(registry)

        assertEquals(BlockFace.EAST, assertInstanceOf(ConveyorBelt::class.java, reload().getAllBlocks().first()).facing)
    }

    @Test
    fun `multiple blocks save and load correctly`() {
        val panel = SmallSolarPanel(MockServer.createLocation(0.0, 64.0, 0.0))
        val cable = PowerCable(MockServer.createLocation(1.0, 64.0, 0.0))
        val battery = SmallBattery(MockServer.createLocation(2.0, 64.0, 0.0))
        val pump = FluidPump(MockServer.createLocation(3.0, 64.0, 0.0))
        val pipe = FluidPipe(MockServer.createLocation(4.0, 64.0, 0.0))
        registry.track(panel, "atlas:small_solar_panel")
        registry.track(cable, "atlas:power_cable")
        registry.track(battery, "atlas:small_battery")
        registry.track(pump, "atlas:fluid_pump")
        registry.track(pipe, "atlas:fluid_pipe")

        persistence.save(registry)

        assertEquals(5, reload().getAllBlocks().size)
    }

    @Test
    fun `a newer schema version still loads the blocks this version understands`() {
        file(BlockPersistence.FILE_NAME).writeText(
            """
            version: ${BlockPersistence.VERSION + 1}
            blocks:
            - blockId: atlas:small_battery
              world: world
              x: 0
              y: 64
              z: 0
              currentPower: 3
            """.trimIndent(),
        )

        assertEquals(3, assertInstanceOf(SmallBattery::class.java, reload().getAllBlocks().single()).currentPower)
    }

    private fun writeLegacyFiles() {
        file("power_blocks.yml").writeText(
            """
            power_blocks:
            - blockId: atlas:small_battery
              world: world
              x: 5
              y: 64
              z: 3
              currentPower: 7
            - blockId: atlas:cobblestone_factory
              world: world
              x: 6
              y: 64
              z: 3
              currentPower: 2
              hasWater: true
              hasLava: false
            """.trimIndent(),
        )
        file("fluid_blocks.yml").writeText(
            """
            fluid_blocks:
            - blockId: atlas:fluid_pump
              world: world
              x: 7
              y: 64
              z: 3
              fluidType: LAVA
            """.trimIndent(),
        )
        file("transport_blocks.yml").writeText(
            """
            transport_blocks:
            - blockId: atlas:conveyor_belt
              world: world
              x: 8
              y: 64
              z: 3
              facing: EAST
            """.trimIndent(),
        )
    }

    private fun assertLegacyBlocks(loaded: BlockRegistry) {
        val byX = loaded.getAllBlocks().associateBy { it.location.blockX }
        assertEquals(setOf(5, 6, 7, 8), byX.keys)
        assertEquals(7, assertInstanceOf(SmallBattery::class.java, byX[5]).currentPower)
        val factory = assertInstanceOf(CobblestoneFactory::class.java, byX[6])
        assertEquals(2, factory.currentPower)
        assertTrue(factory.hasWater)
        assertEquals(FluidType.LAVA, assertInstanceOf(FluidPump::class.java, byX[7]).storedFluid)
        assertEquals(BlockFace.EAST, assertInstanceOf(ConveyorBelt::class.java, byX[8]).facing)
    }

    @Test
    fun `legacy per-family files are loaded, written to the new file and renamed`() {
        writeLegacyFiles()

        persistence.load(registry)

        assertLegacyBlocks(registry)
        assertTrue(file(BlockPersistence.FILE_NAME).isFile)
        for (legacy in BlockPersistence.LEGACY_FILES.keys) {
            assertFalse(file(legacy).exists(), legacy)
            assertTrue(file(legacy + BlockPersistence.MIGRATED_SUFFIX).isFile, legacy)
        }
    }

    @Test
    fun `migrated blocks come back from the new file alone on the next start`() {
        writeLegacyFiles()
        persistence.load(registry)

        assertLegacyBlocks(reload())
    }

    @Test
    fun `migration keeps the legacy files' contents byte for byte`() {
        writeLegacyFiles()
        val before = BlockPersistence.LEGACY_FILES.keys.associateWith { file(it).readText() }

        persistence.load(registry)

        for ((legacy, text) in before) {
            assertEquals(text, file(legacy + BlockPersistence.MIGRATED_SUFFIX).readText(), legacy)
        }
    }

    @Test
    fun `only the legacy files present are migrated`() {
        file("fluid_blocks.yml").writeText(
            """
            fluid_blocks:
            - blockId: atlas:fluid_pipe
              world: world
              x: 1
              y: 64
              z: 0
              fluidType: WATER
            """.trimIndent(),
        )

        persistence.load(registry)

        assertInstanceOf(FluidPipe::class.java, reload().getAllBlocks().single())
        assertTrue(file("fluid_blocks.yml.migrated").isFile)
        assertFalse(file("power_blocks.yml.migrated").exists())
    }

    @Test
    fun `legacy files next to an existing new file are set aside, not loaded over it`() {
        registry.track(SmallBattery(MockServer.createLocation(x = 5.0, z = 3.0)).also { it.currentPower = 9 }, SmallBattery.BLOCK_ID)
        persistence.save(registry)
        writeLegacyFiles()

        val loaded = reload()

        assertEquals(9, assertInstanceOf(SmallBattery::class.java, loaded.getAllBlocks().single()).currentPower)
        for (legacy in BlockPersistence.LEGACY_FILES.keys) {
            assertFalse(file(legacy).exists(), legacy)
            assertTrue(file(legacy + BlockPersistence.MIGRATED_SUFFIX).isFile, legacy)
        }
    }

    @Test
    fun `an earlier migrated copy is never overwritten`() {
        file("power_blocks.yml.migrated").writeText("earlier")
        writeLegacyFiles()

        persistence.load(registry)

        assertEquals("earlier", file("power_blocks.yml.migrated").readText())
        val copies = MockServer.dataFolder.listFiles()!!.filter { it.name.startsWith("power_blocks.yml.migrated-") }
        assertEquals(1, copies.size)
        assertFalse(file("power_blocks.yml").exists())
    }

    private val netherBattery =
        """
        - blockId: atlas:small_battery
          world: nether
          x: 1
          y: 70
          z: 2
          currentPower: 4
        """.trimIndent()

    private val netherBelt =
        """
        - blockId: atlas:conveyor_belt
          world: nether
          x: 3
          y: 70
          z: 2
          facing: EAST
        """.trimIndent()

    private fun writeBlocks(vararg entries: String) {
        file(BlockPersistence.FILE_NAME).writeText("version: ${BlockPersistence.VERSION}\nblocks:\n" + entries.joinToString("\n"))
    }

    private fun savedEntries(): List<Map<*, *>> = YamlConfiguration.loadConfiguration(file(BlockPersistence.FILE_NAME)).getMapList("blocks")

    private fun mockWorld(name: String): World {
        val world = mockk<World>(relaxed = true)
        every { world.name } returns name
        return world
    }

    @Test
    fun `blocks in a world that is not loaded survive a save unchanged`() {
        writeBlocks(netherBattery, netherBelt)
        val before = savedEntries()

        persistence.load(registry)
        persistence.save(registry)

        assertTrue(registry.getAllBlocks().isEmpty())
        assertEquals(before, savedEntries())
    }

    @Test
    fun `loading the world later restores its held blocks with their data and facing`() {
        writeBlocks(netherBattery, netherBelt)
        persistence.load(registry)

        persistence.restoreWorld(registry, mockWorld("nether"))

        val byX = registry.getAllBlocks().associateBy { it.location.blockX }
        assertEquals(setOf(1, 3), byX.keys)
        assertEquals(4, assertInstanceOf(SmallBattery::class.java, byX[1]).currentPower)
        assertEquals(BlockFace.EAST, assertInstanceOf(ConveyorBelt::class.java, byX[3]).facing)
        assertTrue(byX.values.all { it.location.world?.name == "nether" })
    }

    @Test
    fun `restored blocks are saved once`() {
        writeBlocks(netherBattery, netherBelt)
        persistence.load(registry)
        persistence.restoreWorld(registry, mockWorld("nether"))

        persistence.save(registry)

        assertEquals(2, savedEntries().size)
    }

    @Test
    fun `loading an unrelated world leaves held blocks held`() {
        writeBlocks(netherBattery)
        persistence.load(registry)

        persistence.restoreWorld(registry, mockWorld("the_end"))
        persistence.save(registry)

        assertTrue(registry.getAllBlocks().isEmpty())
        assertEquals(1, savedEntries().size)
    }

    @Test
    fun `a live block on a held location replaces the held entry`() {
        writeBlocks(netherBattery)
        persistence.load(registry)
        val nether = mockWorld("nether")
        registry.track(SmallBattery(MockServer.createLocation(1.0, 70.0, 2.0, nether)).also { it.currentPower = 9 }, SmallBattery.BLOCK_ID)

        persistence.save(registry)

        assertEquals(9, savedEntries().single()["currentPower"])
    }

    @Test
    fun `an entry with an id the catalog does not know is kept`() {
        writeBlocks(
            """
            - blockId: atlas:removed_machine
              world: world
              x: 0
              y: 64
              z: 0
              speed: 3
            """.trimIndent(),
        )
        val before = savedEntries()

        persistence.load(registry)
        persistence.save(registry)

        assertTrue(registry.getAllBlocks().isEmpty())
        assertEquals(before, savedEntries())
    }

    @Test
    fun `migration keeps legacy blocks whose world is not loaded`() {
        file("power_blocks.yml").writeText("power_blocks:\n$netherBattery")

        persistence.load(registry)

        assertTrue(registry.getAllBlocks().isEmpty())
        assertTrue(file("power_blocks.yml.migrated").isFile)
        val entry = savedEntries().single()
        assertEquals("nether", entry["world"])
        assertEquals(4, entry["currentPower"])
    }

    @Test
    fun `legacy files stay in place when the new file cannot be written`() {
        val blocker = file(BlockPersistence.FILE_NAME)
        blocker.mkdirs()
        File(blocker, "occupied").writeText("")
        writeLegacyFiles()

        persistence.load(registry)

        assertLegacyBlocks(registry)
        for (legacy in BlockPersistence.LEGACY_FILES.keys) {
            assertTrue(file(legacy).isFile, legacy)
            assertFalse(file(legacy + BlockPersistence.MIGRATED_SUFFIX).exists(), legacy)
        }
        assertFalse(file(BlockPersistence.FILE_NAME + ".tmp").exists())
    }
}
