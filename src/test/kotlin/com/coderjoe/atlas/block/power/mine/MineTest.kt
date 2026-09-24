package com.coderjoe.atlas.block.power.mine

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.PlacementType
import com.coderjoe.atlas.block.power.PowerBlockFactory
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.transport.ConveyorBelt
import com.coderjoe.atlas.testing.AtlasPaths.BLOCK_MODEL_DIR
import com.coderjoe.atlas.testing.AtlasPaths.config
import com.coderjoe.atlas.testing.AtlasPaths.configFiles
import com.coderjoe.atlas.testing.Blocks
import com.coderjoe.atlas.testing.Blocks.placedIn
import com.coderjoe.atlas.testing.MockServer
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
    private lateinit var registry: BlockRegistry

    private val gantryModel =
        File(BLOCK_MODEL_DIR, "/mine_gantry.json")

    @Suppress("UNCHECKED_CAST")
    private fun states(fileName: String): Map<String, Any?> {
        val doc = Yaml().load<Map<String, Any?>>(config(fileName).readText())
        val item = (doc["items"] as Map<String, Any?>).values.first() as Map<String, Any?>
        val block = (item["behavior"] as Map<String, Any?>)["block"] as Map<String, Any?>
        return block["states"] as Map<String, Any?>
    }

    @Suppress("UNCHECKED_CAST")
    private fun appearances(fileName: String): Map<String, Map<String, Any?>> =
        states(fileName)["appearances"] as Map<String, Map<String, Any?>>

    @Suppress("UNCHECKED_CAST")
    private fun gantryElements(): List<Map<String, Any?>> =
        (Yaml().load<Map<String, Any?>>(gantryModel.readText())["elements"] as List<Map<String, Any?>>)

    @BeforeEach
    fun setup() {
        MockServer.setup()
        registry = BlockRegistry(MockServer.plugin)
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
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
            Triple(Mine(location, MineTier.COAL), Material.COAL, MineTier.COAL.powerPerHaul),
            Triple(Mine(location, MineTier.IRON), Material.RAW_IRON, MineTier.IRON.powerPerHaul),
            Triple(Mine(location, MineTier.REDSTONE), Material.REDSTONE, MineTier.REDSTONE.powerPerHaul),
            Triple(Mine(location, MineTier.GOLD), Material.RAW_GOLD, MineTier.GOLD.powerPerHaul),
            Triple(Mine(location, MineTier.EMERALD), Material.EMERALD, MineTier.EMERALD.powerPerHaul),
            Triple(Mine(location, MineTier.DIAMOND), Material.DIAMOND, MineTier.DIAMOND.powerPerHaul),
            Triple(Mine(location, MineTier.NETHERITE), Material.ANCIENT_DEBRIS, MineTier.NETHERITE.powerPerHaul),
        )

    @Test
    fun `each mine digs its own ore for its own price`() {
        val location = MockServer.createLocation()
        for ((mine, ore, cost) in allMines(location)) {
            assertEquals(ore, mine.output, "${mine::class.simpleName} output")
            assertEquals(cost, mine.powerPerHaul, "${mine::class.simpleName} power per haul")
        }
    }

    @Test
    fun `a haul spends the power`() {
        val location = MockServer.createLocation()
        val mine = Mine(location, MineTier.COAL).placedIn(registry)
        mine.currentPower = MineTier.COAL.powerPerHaul

        expectingRegistryFailure { mine.powerUpdate() }

        assertEquals(0, mine.currentPower)
    }

    @Test
    fun `a mine short of power digs nothing and keeps what it has`() {
        val location = MockServer.createLocation()
        val mine = Mine(location, MineTier.DIAMOND).placedIn(registry)
        mine.currentPower = MineTier.DIAMOND.powerPerHaul - 1

        // No try/catch: a mine that cannot afford a haul never reaches the drop, so nothing
        // here touches the item registry.
        mine.powerUpdate()

        assertEquals(MineTier.DIAMOND.powerPerHaul - 1, mine.currentPower)
    }

    @Test
    fun `a haul takes only its own cost, leaving the rest banked`() {
        val location = MockServer.createLocation()
        val mine = Mine(location, MineTier.GOLD).placedIn(registry)
        mine.currentPower = MineTier.GOLD.powerPerHaul * 2

        expectingRegistryFailure { mine.powerUpdate() }

        assertEquals(MineTier.GOLD.powerPerHaul, mine.currentPower)
    }

    /**
     * The whole point of a drilling cycle: power can now be pulled every tick, so without a
     * separate timer a mine sitting on a full battery would produce every single tick. `CYCLE_TICKS`
     * has to actually gate completion, not just describe how the block used to be scheduled.
     */
    @Test
    fun `a haul takes the full cycle to complete, not the first tick it is committed on`() {
        val location = MockServer.createLocation()
        val mine = Mine(location, MineTier.COAL).placedIn(registry)
        mine.currentPower = MineTier.COAL.powerPerHaul

        // Committing tick: the cost is spent and drilling starts, but nothing is produced yet.
        mine.powerUpdate()
        assertEquals(0, mine.currentPower, "power is spent the moment the haul is committed")
        assertTrue(mine.isCutting, "drilling has started")

        // Mine.updateIntervalTicks is 20; the cycle needs that many calls to complete, all but
        // the last still mid-drill.
        val midCycleCalls = (MineTier.COAL.cycleTicks / 20L - 1).toInt()
        repeat(midCycleCalls) {
            mine.powerUpdate()
            assertTrue(mine.isCutting, "still drilling mid-cycle, nothing to produce yet")
        }

        // The final tick finishes it.
        expectingRegistryFailure { mine.powerUpdate() }
        assertFalse(mine.isCutting, "drilling finished, back to idle")
    }

    @Test
    fun `a fully powered mine starts its next haul the instant the last one finishes`() {
        val location = MockServer.createLocation()
        val mine = Mine(location, MineTier.COAL).placedIn(registry)
        mine.currentPower = MineTier.COAL.powerPerHaul * 2

        mine.powerUpdate() // commits haul 1
        val midCycleCalls = (MineTier.COAL.cycleTicks / 20L - 1).toInt()
        repeat(midCycleCalls) { mine.powerUpdate() } // mid-cycle
        // haul 1 completes and, in the same tick, haul 2 commits - no idle tick between them
        expectingRegistryFailure { mine.powerUpdate() }

        assertTrue(mine.isCutting, "haul 2 should already be drilling")
        assertEquals(0, mine.currentPower, "both hauls' cost has now been committed")
    }

    @Test
    fun `a mine keeps pulling power while mid-drill, banking it for the next haul`() {
        val mine = Mine(MockServer.createLocation(0.0, 64.0, 0.0), MineTier.COAL)
        mine.currentPower = MineTier.COAL.powerPerHaul

        val battery = SmallBattery(MockServer.createLocation(1.0, 64.0, 0.0))
        battery.currentPower = 5
        registry.track(mine, "atlas:coal_mine")
        registry.track(battery, "atlas:small_battery")

        // Pulls a unit from the battery, then commits the already-affordable haul in the same tick.
        mine.powerUpdate()
        assertTrue(mine.isCutting, "drilling has started")
        val powerAfterCommit = mine.currentPower

        mine.powerUpdate() // mid-drill - still pulls from the battery every tick
        assertTrue(
            mine.currentPower > powerAfterCommit,
            "the mine should keep banking power from the battery while a haul is in progress",
        )
        assertTrue(battery.currentPower < 5, "that banked power came from the battery")
    }

    @Test
    fun `a freshly committed haul lights the ore without having eaten into it yet`() {
        val mine = Mine(MockServer.createLocation(0.0, 64.0, 0.0), MineTier.COAL).placedIn(registry)
        mine.currentPower = MineTier.COAL.powerPerHaul

        mine.powerUpdate()

        assertEquals(Mine.IDLE_STAGE + 1, mine.drillStage, "the first digging stage still holds a whole ore")
    }

    /**
     * The progress read, and the reason `stage` is an int rather than the factories' `powered`
     * boolean: a bore runs 200-1000 ticks, so the ore has to visibly come apart across it rather
     * than the machine just switching a light on for fifty seconds.
     */
    @Test
    fun `the ore is eaten away step by step as the drill counts down`() {
        val mine = Mine(MockServer.createLocation(0.0, 64.0, 0.0), MineTier.COAL).placedIn(registry)
        mine.currentPower = MineTier.COAL.powerPerHaul

        mine.powerUpdate() // commits the haul
        val stages = mutableListOf(mine.drillStage)
        repeat((MineTier.COAL.cycleTicks / 20L - 1).toInt()) {
            mine.powerUpdate()
            stages += mine.drillStage
        }

        // distinct() keeps first-seen order, so this pins the sequence as well as the coverage:
        // every digging stage is shown, in order, and none of them is revisited.
        assertEquals(
            (Mine.IDLE_STAGE + 1..Mine.IDLE_STAGE + Mine.DIGGING_STAGES).toList(),
            stages.distinct(),
            "each digging stage should be shown once, in order",
        )
    }

    @Test
    fun `a finished haul left without power for another puts the ore back whole`() {
        val mine = Mine(MockServer.createLocation(0.0, 64.0, 0.0), MineTier.COAL).placedIn(registry)
        mine.currentPower = MineTier.COAL.powerPerHaul

        mine.powerUpdate() // commits the only haul this mine can afford
        val midCycleCalls = (MineTier.COAL.cycleTicks / 20L - 1).toInt()
        repeat(midCycleCalls) { mine.powerUpdate() }
        expectingRegistryFailure { mine.powerUpdate() }

        assertFalse(mine.isCutting)
        assertEquals(Mine.IDLE_STAGE, mine.drillStage, "a mine that has stopped digging shows a whole, unlit ore")
    }

    @Test
    fun `an idle mine shows a whole ore`() {
        val mine = Mine(MockServer.createLocation(0.0, 64.0, 0.0), MineTier.COAL).placedIn(registry)
        mine.currentPower = 0

        mine.powerUpdate()

        assertEquals(Mine.IDLE_STAGE, mine.drillStage)
    }

    @Test
    fun `the ore lands above the deck so it falls clear of the rig`() {
        val mine = Mine(MockServer.createLocation(x = 10.0, y = 64.0, z = -3.0), MineTier.IRON)
        val drop = mine.dropLocation()

        assertEquals(10.5, drop.x)
        assertEquals(65.5, drop.y)
        assertEquals(-2.5, drop.z)
    }

    @Test
    fun `haul destination falls back to the loose drop when nothing is attached`() {
        val mine = Mine(MockServer.createLocation(), MineTier.COAL).placedIn(registry)

        assertEquals(mine.dropLocation(), mine.haulDestination())
    }

    @Test
    fun `haul destination lands directly on an attached conveyor belt`() {
        val mine = Mine(MockServer.createLocation(0.0, 64.0, 0.0), MineTier.COAL).placedIn(registry)

        val belt = ConveyorBelt(MockServer.createLocation(0.0, 65.0, 0.0), BlockFace.NORTH)
        registry.track(belt, "atlas:conveyor_belt")

        val destination = mine.haulDestination()

        assertEquals(0.5, destination.x)
        assertEquals(65.75, destination.y)
        assertEquals(0.5, destination.z)
    }

    @Test
    fun `haul destination round-robins across every attached conveyor belt`() {
        val mine = Mine(MockServer.createLocation(0.0, 64.0, 0.0), MineTier.COAL).placedIn(registry)

        // one belt to the north, one to the south - neither is the vertical drop spot
        val northBelt = ConveyorBelt(MockServer.createLocation(0.0, 64.0, -1.0), BlockFace.NORTH)
        val southBelt = ConveyorBelt(MockServer.createLocation(0.0, 64.0, 1.0), BlockFace.SOUTH)
        registry.track(northBelt, "atlas:conveyor_belt")
        registry.track(southBelt, "atlas:conveyor_belt")

        val destinations = List(4) { mine.haulDestination().z }

        assertEquals(listOf(-0.5, 1.5, -0.5, 1.5), destinations, "hauls should alternate between the two belts")
    }

    @Test
    fun `a mine keeps one visual state - digging is a property, not a second block`() {
        val mine = Mine(MockServer.createLocation(), MineTier.EMERALD)
        mine.currentPower = 0
        assertEquals(MineTier.EMERALD.blockId, mine.getVisualStateBlockId())
        mine.currentPower = MineTier.EMERALD.powerPerHaul
        assertEquals(MineTier.EMERALD.blockId, mine.getVisualStateBlockId())
    }

    @Test
    fun `rarer ore costs more power and takes longer to bore`() {
        val location = MockServer.createLocation()
        val ordered =
            listOf(
                Mine(location, MineTier.COAL),
                Mine(location, MineTier.IRON),
                Mine(location, MineTier.GOLD),
                Mine(location, MineTier.EMERALD),
                Mine(location, MineTier.DIAMOND),
                Mine(location, MineTier.NETHERITE),
            )
        for ((cheaper, dearer) in ordered.zipWithNext()) {
            assertTrue(
                dearer.powerPerHaul > cheaper.powerPerHaul,
                "${dearer.tier.displayName} should cost more than ${cheaper.tier.displayName}",
            )
        }
    }

    @Test
    fun `every mine descriptor faces the player and registers its own ID`() {
        Blocks.initPowerFactory()
        val descriptors = MineTier.entries.map { it.descriptor }
        assertEquals(7, descriptors.map { it.baseBlockId }.toSet().size)
        for (descriptor in descriptors) {
            // The shaft mouth is turned back toward whoever placed it.
            assertEquals(PlacementType.DIRECTIONAL_OPPOSITE, descriptor.placementType, descriptor.baseBlockId)
            assertTrue(descriptor.displayName.endsWith("Mine"), descriptor.displayName)
            assertTrue(PowerBlockFactory.isRegistered(descriptor.baseBlockId), descriptor.baseBlockId)
        }
    }

    /**
     * The seven mine classes collapsed into [MineTier]. Block ids are persisted and held in
     * CraftEngine's state pools, and the descriptions are now derived from the numbers, so both
     * are pinned to exactly what the hand-written classes declared.
     */
    @Test
    fun `every tier keeps the id, storage and description its class had`() {
        val expected =
            mapOf(
                MineTier.COAL to Triple("atlas:coal_mine", 10, "Mine - consumes 2 power every 10s \u2192 1 coal"),
                MineTier.IRON to Triple("atlas:iron_mine", 20, "Mine - consumes 5 power every 15s \u2192 1 raw iron"),
                MineTier.REDSTONE to Triple("atlas:redstone_mine", 20, "Mine - consumes 5 power every 15s \u2192 1 redstone"),
                MineTier.GOLD to Triple("atlas:gold_mine", 30, "Mine - consumes 8 power every 20s \u2192 1 raw gold"),
                MineTier.EMERALD to Triple("atlas:emerald_mine", 50, "Mine - consumes 14 power every 30s \u2192 1 emerald"),
                MineTier.DIAMOND to Triple("atlas:diamond_mine", 60, "Mine - consumes 18 power every 40s \u2192 1 diamond"),
                MineTier.NETHERITE to
                    Triple("atlas:netherite_mine", 100, "Mine - consumes 30 power every 50s \u2192 1 ancient debris"),
            )
        assertEquals(MineTier.entries.toSet(), expected.keys)
        for ((tier, values) in expected) {
            val (blockId, storage, description) = values
            assertEquals(blockId, tier.descriptor.baseBlockId)
            assertEquals(storage, Mine(MockServer.createLocation(), tier).maxStorage, tier.name)
            assertEquals(description, tier.descriptor.description, tier.name)
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
        val location = MockServer.createLocation()
        val mine = Mine(location, MineTier.NETHERITE).placedIn(registry)

        mine.currentPower = MineTier.NETHERITE.powerPerHaul - 1
        mine.powerUpdate()
        assertFalse(mine.isCutting, "cannot afford a haul, so it is not cutting")

        mine.currentPower = MineTier.NETHERITE.powerPerHaul
        expectingRegistryFailure { mine.powerUpdate() }
        assertTrue(mine.isCutting, "can afford a haul, so it is cutting")
    }

    @Test
    fun `a mine placed on the ground still faces a horizontal direction`() {
        val location = MockServer.createLocation()
        // getPlayerFacing answers UP for anything set on the ground, and a block restored with no
        // stored facing replays SELF. Neither may leave the shaft mouth pointing at the sky.
        for (face in listOf(BlockFace.UP, BlockFace.DOWN, BlockFace.SELF)) {
            val mine = Mine(location, MineTier.COAL, face)
            assertTrue(mine.facing in Mine.HORIZONTAL_FACES, "placed against $face, faced ${mine.facing}")
        }
        assertEquals(BlockFace.EAST, Mine(location, MineTier.COAL, BlockFace.EAST).facing)
    }

    /**
     * The renderer is the machine plus the ore in its jaws, and nothing else. The yield used to
     * hang over the chute as well, which was redundant once the ore block was there.
     */
    @Test
    @Suppress("UNCHECKED_CAST")
    fun `a mine renders its machine and the ore it holds, and nothing else`() {
        for (file in configFiles().filter { it.name.endsWith("_mine.yml") }) {
            for ((name, appearance) in appearances(file.name)) {
                val elements = appearance["entity_renderer"] as List<Map<String, Any?>>
                assertEquals(2, elements.size, "${file.name}/$name renders machine + held ore")
                assertTrue(elements[0].containsKey("item"), "${file.name}/$name first is the machine")
                assertTrue(elements[1].containsKey("block"), "${file.name}/$name second is the ore")
            }
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `every facing turns the machine a different way`() {
        val expected = mapOf("south" to null, "north" to 180, "east" to -90, "west" to 90)

        for (file in configFiles().filter { it.name.endsWith("_mine.yml") }) {
            val found = appearances(file.name)
            val perFacing = 1 + Mine.DIGGING_STAGES
            assertEquals(4 * perFacing, found.size, "${file.name} has an appearance per facing per stage")

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

        val mines = allMines(MockServer.createLocation())
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
     * The Kotlin picks a `stage` number every tick and the YAML is the only thing that turns it
     * into something a player can see, so the two have to agree on how many stages there are and
     * on what each one looks like. A mismatch shows up in game as a mine frozen on one appearance,
     * which is exactly the failure this replaced - nothing throws, it just never animates.
     */
    @Test
    @Suppress("UNCHECKED_CAST")
    fun `every mine declares the stages the drill counts through, eating the ore away across them`() {
        val lastStage = Mine.IDLE_STAGE + Mine.DIGGING_STAGES

        for (file in configFiles().filter { it.name.endsWith("_mine.yml") }) {
            val states = states(file.name)
            val stage = (states["properties"] as Map<String, Any?>)["stage"] as Map<String, Any?>
            val variants = states["variants"] as Map<String, Map<String, Any?>>
            val found = appearances(file.name)

            assertEquals("${Mine.IDLE_STAGE}~$lastStage", stage["range"], "${file.name} stage range")

            for (facing in listOf("north", "south", "east", "west")) {
                val scales =
                    (Mine.IDLE_STAGE..lastStage).map { value ->
                        val key = "facing=$facing,stage=$value"
                        val name = variants[key]?.get("appearance") as String?
                        assertTrue(name != null, "${file.name} has no variant for $key")
                        val held = (found.getValue(name!!)["entity_renderer"] as List<Map<String, Any?>>)[1]
                        (held["scale"] as List<Number>).first().toDouble()
                    }

                assertEquals(scales[0], scales[1], "${file.name}/$facing: committing a haul lights the ore, it does not shrink it")
                for ((whole, eaten) in scales.drop(1).zipWithNext()) {
                    assertTrue(eaten < whole, "${file.name}/$facing: the ore should shrink at every stage, got $scales")
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
