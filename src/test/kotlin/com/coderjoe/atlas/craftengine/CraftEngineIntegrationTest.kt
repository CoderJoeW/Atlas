package com.coderjoe.atlas.craftengine

import com.coderjoe.atlas.testing.AtlasPaths.RESOURCES
import com.coderjoe.atlas.testing.AtlasPaths.configFiles
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * The pruning contract: Atlas cleans up after itself and touches nothing else.
 *
 * Exercised through the manifest directly rather than a live plugin, since the copy step needs a
 * real Bukkit plugin to unpack resources from.
 */
class CraftEngineIntegrationTest {
    private companion object {
        const val MODELS = "${CraftEngineIntegration.RESOURCE_PACK_PATH}/assets/minecraft/models/block/custom"
        const val TEXTURES = "${CraftEngineIntegration.RESOURCE_PACK_PATH}/assets/minecraft/textures/block/custom"
    }

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
        val model = touch("$MODELS/power_splitter_base.json")
        val texture = touch("$TEXTURES/power_splitter_in.png")

        prune(
            previous = listOf("$MODELS/power_splitter_base.json", "$TEXTURES/power_splitter_in.png"),
            deployed = emptySet(),
        )

        assertFalse(model.exists())
        assertFalse(texture.exists())
    }

    @Test
    fun `configs in subfolders are discovered`() {
        val discovered = CraftEngineIntegration.discoverResources("atlas/configuration/", ".yml").sorted()
        val onDisk = configFiles().map { "atlas/" + it.relativeTo(RESOURCES).invariantSeparatorsPath }.sorted()

        assertEquals(onDisk, discovered)
    }

    @Test
    fun `two configs with the same file name stop the deploy`() {
        val clash = listOf("atlas/configuration/power/pump.yml", "atlas/configuration/fluid/pump.yml")

        val error = assertThrows<IllegalStateException> { CraftEngineIntegration.requireUniqueFileNames(clash) }
        assertTrue("pump.yml" in error.message!!, "the error should name the clashing file")
    }

    @Test
    fun `the configs Atlas ships have unique file names`() {
        val shipped = configFiles().map { it.invariantSeparatorsPath }

        assertDoesNotThrow { CraftEngineIntegration.requireUniqueFileNames(shipped) }
    }
}
