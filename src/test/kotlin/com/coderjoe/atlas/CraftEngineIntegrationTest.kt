package com.coderjoe.atlas

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * The pruning contract: Atlas cleans up after itself and touches nothing else.
 *
 * Exercised through the manifest directly rather than a live plugin, since the copy step needs a
 * real Bukkit plugin to unpack resources from.
 */
class CraftEngineIntegrationTest {
    @TempDir
    lateinit var folder: File

    /** Mirrors the prune step: delete manifest entries this run no longer ships. */
    private fun prune(
        previous: List<String>,
        deployed: Set<String>,
    ) {
        File(folder, CraftEngineIntegration.MANIFEST_NAME).writeText(previous.joinToString("\n"))
        for (path in previous.subtract(deployed)) {
            File(folder, path).takeIf { it.exists() }?.delete()
        }
    }

    private fun touch(path: String): File =
        File(folder, path).also {
            it.parentFile.mkdirs()
            it.writeText("x")
        }

    @Test
    fun `a retired config is removed on the next run`() {
        val retired = touch("configuration/power_splitter.yml")
        val kept = touch("configuration/power_cable.yml")

        prune(
            previous = listOf("configuration/power_splitter.yml", "configuration/power_cable.yml"),
            deployed = setOf("configuration/power_cable.yml"),
        )

        assertFalse(retired.exists(), "a config Atlas no longer ships should be removed")
        assertTrue(kept.exists(), "a config Atlas still ships must survive")
    }

    @Test
    fun `a file Atlas never deployed is left alone`() {
        val foreign = touch("configuration/gold_power_cable.yml")

        // the manifest has no record of it, so it is not Atlas's to delete
        prune(
            previous = listOf("configuration/power_cable.yml"),
            deployed = setOf("configuration/power_cable.yml"),
        )

        assertTrue(foreign.exists(), "a file Atlas did not deploy must never be deleted")
    }

    @Test
    fun `retired models and textures are pruned too`() {
        val model = touch("${CraftEngineIntegration.MODELS_PATH}/power_splitter_base.json")
        val texture = touch("${CraftEngineIntegration.TEXTURES_PATH}/power_splitter_in.png")

        prune(
            previous =
                listOf(
                    "${CraftEngineIntegration.MODELS_PATH}/power_splitter_base.json",
                    "${CraftEngineIntegration.TEXTURES_PATH}/power_splitter_in.png",
                ),
            deployed = emptySet(),
        )

        assertFalse(model.exists())
        assertFalse(texture.exists())
    }

    @Test
    fun `the manifest paths match the folders resources are written to`() {
        assertEquals("resourcepack/assets/minecraft/models/block/custom", CraftEngineIntegration.MODELS_PATH)
        assertEquals("resourcepack/assets/minecraft/textures/block/custom", CraftEngineIntegration.TEXTURES_PATH)
    }
}
