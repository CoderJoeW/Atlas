package com.coderjoe.atlas.pack

import com.coderjoe.atlas.testing.AtlasPaths.configFiles
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

/**
 * An entity-rendered block is a forced barrier on the client, and a barrier has no model for the
 * vanilla break crack to draw on. Without `destroy-stages` the block gives no sign it is being mined.
 */
class BlockBreakFeedbackTest {
    @Suppress("UNCHECKED_CAST")
    private fun blocks(): List<Pair<String, Map<String, Any?>>> =
        configFiles()
            .map { Yaml().load<Map<String, Any?>>(it.readText()) }
            .flatMap { doc ->
                doc.entries
                    .filter { it.key.startsWith("items") }
                    .flatMap { (_, items) -> (items as Map<String, Any?>).entries }
            }.mapNotNull { (id, cfg) ->
                val behavior = (cfg as Map<String, Any?>)["behavior"] as? Map<String, Any?>
                val block = behavior?.get("block") as? Map<String, Any?> ?: return@mapNotNull null
                id to block
            }

    @Suppress("UNCHECKED_CAST")
    private fun isBarrierRendered(block: Map<String, Any?>): Boolean {
        val single = block["state"] as? Map<String, Any?>
        val appearances = ((block["states"] as? Map<String, Any?>)?.get("appearances") as? Map<String, Any?>)?.values.orEmpty()
        return (listOfNotNull(single) + appearances.filterIsInstance<Map<String, Any?>>()).any { it["state"] == "barrier" }
    }

    @Test
    fun `every barrier-rendered block shows destroy stages while being mined`() {
        val barrierBlocks = blocks().filter { (_, block) -> isBarrierRendered(block) }
        val missing =
            barrierBlocks.filter { (_, block) ->
                @Suppress("UNCHECKED_CAST")
                val settings = block["settings"] as? Map<String, Any?>
                val stages = settings?.get("destroy-stages") as? Map<*, *>
                stages?.get("template") != "internal:destroy_stages"
            }.map { it.first }

        assertTrue(barrierBlocks.isNotEmpty(), "no barrier-rendered blocks found")
        assertTrue(missing.isEmpty(), "barrier-rendered blocks with no destroy stage display: $missing")
    }

    @Test
    fun `every block drops something when broken`() {
        val missing = blocks().filter { (_, block) -> block["loot"] == null }.map { it.first }

        assertTrue(missing.isEmpty(), "blocks with no loot table: $missing")
    }
}
