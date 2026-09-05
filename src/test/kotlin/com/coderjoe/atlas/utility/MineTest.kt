package com.coderjoe.atlas.utility

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlockFactory
import com.coderjoe.atlas.utility.block.CoalMine
import com.coderjoe.atlas.utility.block.DiamondMine
import com.coderjoe.atlas.utility.block.EmeraldMine
import com.coderjoe.atlas.utility.block.GoldMine
import com.coderjoe.atlas.utility.block.IronMine
import com.coderjoe.atlas.utility.block.Mine
import com.coderjoe.atlas.utility.block.NetheriteMine
import com.coderjoe.atlas.utility.block.RedstoneMine
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.io.File

class MineTest {
    private val configDir = File("src/main/resources/atlas/configuration")
    private val gantryModel =
        File("src/main/resources/atlas/resourcepack/assets/minecraft/models/block/custom/mine_gantry.json")

    @Suppress("UNCHECKED_CAST")
    private fun appearances(fileName: String): Map<String, Map<String, Any?>> {
        val doc = Yaml().load<Map<String, Any?>>(File(configDir, fileName).readText())
        val item = (doc["items"] as Map<String, Any?>).values.first() as Map<String, Any?>
        val block = (item["behavior"] as Map<String, Any?>)["block"] as Map<String, Any?>
        return (block["states"] as Map<String, Any?>)["appearances"] as Map<String, Map<String, Any?>>
    }

    @Suppress("UNCHECKED_CAST")
    private fun gantryElements(): List<Map<String, Any?>> =
        (Yaml().load<Map<String, Any?>>(gantryModel.readText())["elements"] as List<Map<String, Any?>>)

    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    /**
     * Runs [body], tolerating only the failure a mine legitimately hits in a unit test: building
     * an [org.bukkit.inventory.ItemStack] needs Bukkit's registry, which no test server provides.
     * Anything else is a real bug and is rethrown.
     */
    private fun expectingRegistryFailure(body: () -> Unit) {
        try {
            body()
        } catch (e: Throwable) {
            val cause = generateSequence(e) { it.cause }.last()
            val registryFailure =
                cause is NoClassDefFoundError || cause is ExceptionInInitializerError ||
                    cause.message?.contains("Registry") == true
            if (!registryFailure) throw e
        }
    }

    /** Every mine, paired with the ore it digs and what a haul costs. */
    private fun allMines(location: Location): List<Triple<Mine, Material, Int>> =
        listOf(
            Triple(CoalMine(location), Material.COAL, CoalMine.POWER_PER_HAUL),
            Triple(IronMine(location), Material.RAW_IRON, IronMine.POWER_PER_HAUL),
            Triple(RedstoneMine(location), Material.REDSTONE, RedstoneMine.POWER_PER_HAUL),
            Triple(GoldMine(location), Material.RAW_GOLD, GoldMine.POWER_PER_HAUL),
            Triple(EmeraldMine(location), Material.EMERALD, EmeraldMine.POWER_PER_HAUL),
            Triple(DiamondMine(location), Material.DIAMOND, DiamondMine.POWER_PER_HAUL),
            Triple(NetheriteMine(location), Material.ANCIENT_DEBRIS, NetheriteMine.POWER_PER_HAUL),
        )

    @Test
    fun `each mine digs its own ore for its own price`() {
        val location = TestHelper.createLocation()
        for ((mine, ore, cost) in allMines(location)) {
            assertEquals(ore, mine.output, "${mine::class.simpleName} output")
            assertEquals(cost, mine.powerPerHaul, "${mine::class.simpleName} power per haul")
        }
    }

    @Test
    fun `a haul spends the power`() {
        val location = TestHelper.createLocation()
        val mine = CoalMine(location)
        mine.currentPower = CoalMine.POWER_PER_HAUL

        expectingRegistryFailure { mine.callPowerUpdate() }

        assertEquals(0, mine.currentPower)
    }

    @Test
    fun `a mine short of power digs nothing and keeps what it has`() {
        val location = TestHelper.createLocation()
        val mine = DiamondMine(location)
        mine.currentPower = DiamondMine.POWER_PER_HAUL - 1

        // No try/catch: a mine that cannot afford a haul never reaches the drop, so nothing
        // here touches the item registry.
        mine.callPowerUpdate()

        assertEquals(DiamondMine.POWER_PER_HAUL - 1, mine.currentPower)
    }

    @Test
    fun `a haul takes only its own cost, leaving the rest banked`() {
        val location = TestHelper.createLocation()
        val mine = GoldMine(location)
        mine.currentPower = GoldMine.POWER_PER_HAUL * 2

        expectingRegistryFailure { mine.callPowerUpdate() }

        assertEquals(GoldMine.POWER_PER_HAUL, mine.currentPower)
    }

    @Test
    fun `the ore lands above the deck so it falls clear of the rig`() {
        val mine = IronMine(TestHelper.createLocation(x = 10.0, y = 64.0, z = -3.0))
        val drop = mine.dropLocation()

        assertEquals(10.5, drop.x)
        assertEquals(65.5, drop.y)
        assertEquals(-2.5, drop.z)
    }

    @Test
    fun `a mine keeps one visual state - digging is a property, not a second block`() {
        val mine = EmeraldMine(TestHelper.createLocation())
        mine.currentPower = 0
        assertEquals(EmeraldMine.BLOCK_ID, mine.getVisualStateBlockId())
        mine.currentPower = EmeraldMine.POWER_PER_HAUL
        assertEquals(EmeraldMine.BLOCK_ID, mine.getVisualStateBlockId())
    }

    @Test
    fun `rarer ore costs more power and takes longer to bore`() {
        val location = TestHelper.createLocation()
        val ordered =
            listOf(
                CoalMine(location),
                IronMine(location),
                GoldMine(location),
                EmeraldMine(location),
                DiamondMine(location),
                NetheriteMine(location),
            )
        for ((cheaper, dearer) in ordered.zipWithNext()) {
            assertTrue(
                dearer.powerPerHaul > cheaper.powerPerHaul,
                "${dearer::class.simpleName} should cost more than ${cheaper::class.simpleName}",
            )
        }
    }

    @Test
    fun `every mine descriptor faces the player and registers its own ID`() {
        TestHelper.initPowerFactory()
        val descriptors =
            listOf(
                CoalMine.descriptor,
                IronMine.descriptor,
                RedstoneMine.descriptor,
                GoldMine.descriptor,
                EmeraldMine.descriptor,
                DiamondMine.descriptor,
                NetheriteMine.descriptor,
            )
        assertEquals(7, descriptors.map { it.baseBlockId }.toSet().size)
        for (descriptor in descriptors) {
            // The shaft mouth is turned back toward whoever placed it.
            assertEquals(PlacementType.DIRECTIONAL_OPPOSITE, descriptor.placementType, descriptor.baseBlockId)
            assertTrue(descriptor.displayName.endsWith("Mine"), descriptor.displayName)
            assertTrue(PowerBlockFactory.isRegistered(descriptor.baseBlockId), descriptor.baseBlockId)
        }
    }

    /**
     * A mine shows the digging state only when it can actually afford a haul.
     *
     * The inherited powered flag answers "holds any charge", which for a mine is a lie whenever it
     * is fed too slowly: a netherite mine costs 30, so a trickle would leave it sitting at 1-29
     * looking like it is cutting, with the ore lit, while producing nothing.
     */
    @Test
    fun `a mine part way to a haul does not claim to be cutting`() {
        val location = TestHelper.createLocation()
        val mine = NetheriteMine(location)

        mine.currentPower = NetheriteMine.POWER_PER_HAUL - 1
        mine.callPowerUpdate()
        assertFalse(mine.isCutting, "cannot afford a haul, so it is not cutting")

        mine.currentPower = NetheriteMine.POWER_PER_HAUL
        expectingRegistryFailure { mine.callPowerUpdate() }
        assertTrue(mine.isCutting, "can afford a haul, so it is cutting")
    }

    @Test
    fun `a mine placed on the ground still faces a horizontal direction`() {
        val location = TestHelper.createLocation()
        // getPlayerFacing answers UP for anything set on the ground, and a block restored with no
        // stored facing replays SELF. Neither may leave the shaft mouth pointing at the sky.
        for (face in listOf(BlockFace.UP, BlockFace.DOWN, BlockFace.SELF)) {
            val mine = CoalMine(location, face)
            assertTrue(mine.facing in Mine.HORIZONTAL_FACES, "placed against $face, faced ${mine.facing}")
        }
        assertEquals(BlockFace.EAST, CoalMine(location, BlockFace.EAST).facing)
    }

    /**
     * The renderer is the machine plus the ore in its jaws, and nothing else. The yield used to
     * hang over the chute as well, which was redundant once the ore block was there.
     */
    @Test
    @Suppress("UNCHECKED_CAST")
    fun `a mine renders its machine and the ore it holds, and nothing else`() {
        for (file in configDir.listFiles { f -> f.name.endsWith("_mine.yml") }!!) {
            for ((name, appearance) in appearances(file.name)) {
                val elements = appearance["entity_renderer"] as List<Map<String, Any?>>
                assertEquals(2, elements.size, "${file.name}/$name renders machine + held ore")
                assertTrue(elements[0].containsKey("item"), "${file.name}/$name first is the machine")
                assertTrue(elements[1].containsKey("block"), "${file.name}/$name second is the ore")
            }
        }
    }

    /**
     * Four facings, each turning the machine a different way. Nothing in the renderer is off
     * centre any more, so yaw alone carries the rotation - but the appearances still have to exist
     * and differ, or three of the four facings would render identically.
     */
    @Test
    @Suppress("UNCHECKED_CAST")
    fun `every facing turns the machine a different way`() {
        val expected = mapOf("south" to null, "north" to 180, "east" to -90, "west" to 90)

        for (file in configDir.listFiles { f -> f.name.endsWith("_mine.yml") }!!) {
            val found = appearances(file.name)
            assertEquals(8, found.size, "${file.name} has an appearance per facing per state")

            for ((name, appearance) in found) {
                val machine = (appearance["entity_renderer"] as List<Map<String, Any?>>)[0]
                assertEquals(
                    expected.getValue(name.substringAfterLast('_')),
                    (machine["yaw"] as Number?)?.toInt(),
                    "${file.name}/$name yaw",
                )
            }
        }
    }

    /**
     * Minecraft silently refuses to render anything outside -16..32, and rejects a rotation angle
     * that is not one of -45, -22.5, 0, 22.5 or 45.
     */
    @Test
    @Suppress("UNCHECKED_CAST")
    fun `every gantry element obeys Minecraft's model limits`() {
        val legalAngles = setOf(-45.0, -22.5, 0.0, 22.5, 45.0)
        val elements = gantryElements()

        assertTrue(elements.isNotEmpty(), "the gantry model has elements")
        for (element in elements) {
            val name = element["name"] as String
            for (corner in listOf("from", "to")) {
                for (value in (element[corner] as List<Number>).map { it.toDouble() }) {
                    assertTrue(value >= -16.0 && value <= 32.0, "$name $corner $value is outside -16..32")
                }
            }
            val rotation = element["rotation"] as? Map<String, Any?> ?: continue
            val angle = (rotation["angle"] as Number).toDouble()
            assertTrue(angle in legalAngles, "$name rotates by $angle, which Minecraft rejects")
        }
    }

    /**
     * The power cable's hub and arms occupy y 4-12 of its cell, so a mine has to present solid
     * geometry across that band or the cable joins onto thin air. Three sides must be solid; the
     * fourth is the mouth, which is open on purpose - you do not wire into the opening.
     */
    @Test
    @Suppress("UNCHECKED_CAST")
    fun `the pad is solid where the power cable connects`() {
        val elements = gantryElements()

        fun coversCableBand(
            side: String,
            test: (List<Double>, List<Double>) -> Boolean,
        ) {
            val covered =
                elements.any { element ->
                    val from = (element["from"] as List<Number>).map { it.toDouble() }
                    val to = (element["to"] as List<Number>).map { it.toDouble() }
                    from[1] <= 4.0 && to[1] >= 12.0 && test(from, to)
                }
            assertTrue(covered, "nothing covers the cable band (y 4-12) on the $side face")
        }

        coversCableBand("west") { from, _ -> from[0] <= 0.0 }
        coversCableBand("east") { _, to -> to[0] >= 16.0 }
        coversCableBand("back") { from, _ -> from[2] <= 0.0 }
    }

    /**
     * Each tier holds, in its jaws, the ore block that yields the material it drops. That block is
     * the whole answer to "what is this mine mining" - the hardware is identical across all seven.
     *
     * The expectation is derived from each block class's own [Mine.output], so changing a mine's
     * output material without changing its config fails here. A literal map of block id to ore
     * would pass no matter what the Kotlin said.
     */
    @Test
    @Suppress("UNCHECKED_CAST")
    fun `each mine holds the ore block that yields what it drops, lit while digging`() {
        val oreBlockFor =
            mapOf(
                Material.COAL to "minecraft:coal_ore",
                Material.RAW_IRON to "minecraft:iron_ore",
                Material.REDSTONE to "minecraft:redstone_ore",
                Material.RAW_GOLD to "minecraft:gold_ore",
                Material.EMERALD to "minecraft:emerald_ore",
                Material.DIAMOND to "minecraft:diamond_ore",
                Material.ANCIENT_DEBRIS to "minecraft:ancient_debris",
            )

        val mines = allMines(TestHelper.createLocation())
        assertEquals(7, mines.size)

        for ((mine, _, _) in mines) {
            val blockId = mine.baseBlockId.removePrefix("atlas:")
            val expected =
                oreBlockFor[mine.output]
                    ?: error("$blockId drops ${mine.output}, which no ore block in this test yields")

            for ((name, appearance) in appearances("$blockId.yml")) {
                val held = (appearance["entity_renderer"] as List<Map<String, Any?>>)[1]
                assertEquals(expected, held["block"], "$blockId/$name holds the wrong ore")

                // Centred in the cut, which is why it needs no per-facing position.
                val position = (held["position"] as List<Number>).map { it.toDouble() }
                val scale = (held["scale"] as List<Number>).first().toDouble()
                assertEquals(position[0], position[2], "$blockId/$name ore block is off centre")
                assertEquals(0.5, position[0] + scale / 2, "$blockId/$name ore block is off centre")

                val brightness = held["brightness"]
                if (name.startsWith("digging")) {
                    assertTrue(brightness != null, "$blockId/$name should light the ore it is cutting")
                } else {
                    assertTrue(brightness == null, "$blockId/$name is idle and should not light the ore")
                }
            }
        }
    }

    /**
     * Two boxes that share a face plane in the SAME direction z-fight: both quads draw at the same
     * depth and the surface flickers. This shipped once - rails and legs that both spanned
     * x 1-3.5 with their tops at y 25 - and showed up in game as mesh glitching on top of the mine.
     *
     * Opposite-facing coincident faces are fine, because backface culling drops one of them, so
     * this only flags a shared `from` with a `from`, or a `to` with a `to`, where the two boxes
     * also overlap across the other two axes.
     */
    @Test
    @Suppress("UNCHECKED_CAST")
    fun `no two parts of the machine share a face plane`() {
        val boxes =
            gantryElements().map { element ->
                Triple(
                    element["name"] as String,
                    (element["from"] as List<Number>).map { it.toDouble() },
                    (element["to"] as List<Number>).map { it.toDouble() },
                )
            }

        val clashes = mutableListOf<String>()
        for (i in boxes.indices) {
            for (j in i + 1 until boxes.size) {
                val (nameA, fromA, toA) = boxes[i]
                val (nameB, fromB, toB) = boxes[j]
                for (axis in 0..2) {
                    val overlaps =
                        (0..2).filter { it != axis }
                            .all { o -> minOf(toA[o], toB[o]) - maxOf(fromA[o], fromB[o]) > 1e-9 }
                    if (!overlaps) continue
                    if (fromA[axis] == fromB[axis]) clashes += "$nameA/$nameB from.${"xyz"[axis]}"
                    if (toA[axis] == toB[axis]) clashes += "$nameA/$nameB to.${"xyz"[axis]}"
                }
            }
        }
        assertTrue(clashes.isEmpty(), "coincident faces will z-fight in game: $clashes")
    }
}
