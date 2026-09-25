package com.coderjoe.atlas.pack

import com.coderjoe.atlas.testing.AtlasPaths.ITEM_MODEL_DIR
import com.coderjoe.atlas.testing.AtlasPaths.ITEM_TEXTURE_DIR
import com.coderjoe.atlas.testing.AtlasPaths.RESOURCES
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.io.File
import javax.imageio.ImageIO

/**
 * The goggles are a plain Paper item, not a CraftEngine one, so no config checks their art: the
 * item definition, models and textures are hand-placed in the pack. A broken link anywhere in that
 * chain shows the missing-model cube in game with nothing logged, so the chain is walked here.
 */
class GogglesAssetsTest {
    private companion object {
        val itemDefinition = File(RESOURCES, "resourcepack/assets/atlas/items/atlas_goggles.json")
        const val ICON = "atlas_goggles"
        const val WORN = "atlas_goggles_worn"
    }

    /** JSON is a subset of YAML, which saves pulling in a JSON parser for a few small files. */
    @Suppress("UNCHECKED_CAST")
    private fun read(file: File): Map<String, Any?> = Yaml().load(file.readText()) as Map<String, Any?>

    /** Every `model` reference in a definition, through any `select` cases and fallback. */
    private fun modelReferences(node: Any?): List<String> =
        when (node) {
            is Map<*, *> -> {
                val self = (node["model"] as? String)?.let { listOf(it) }.orEmpty()
                self + node.values.flatMap(::modelReferences)
            }
            is List<*> -> node.flatMap(::modelReferences)
            else -> emptyList()
        }

    private fun models(): List<String> = modelReferences(read(itemDefinition)).map { it.substringAfterLast("/") }

    @Suppress("UNCHECKED_CAST")
    private fun textures(model: String): List<File> {
        val refs = read(File(ITEM_MODEL_DIR, "$model.json"))["textures"] as Map<String, String>
        return refs.values.filterNot { it.startsWith("#") }.distinct().map { File(ITEM_TEXTURE_DIR, "${it.substringAfterLast("/")}.png") }
    }

    @Test
    fun `the item definition draws the icon in the gui and the 3D model everywhere else`() {
        assertEquals(listOf(ICON, WORN), models().sorted())
    }

    @Test
    fun `every model the item definition names exists`() {
        for (model in models()) {
            val file = File(ITEM_MODEL_DIR, "$model.json")
            assertTrue(file.isFile, "missing $file")
        }
    }

    @Test
    fun `every texture the models use exists and is a 512 pixel square`() {
        for (texture in models().flatMap(::textures)) {
            assertTrue(texture.isFile, "missing $texture")
            val image = ImageIO.read(texture)
            assertEquals(512 to 512, image.width to image.height, "$texture")
        }
    }

    /** The icon's backdrop and the lens bezel's cut corners are both meant to be see-through. */
    @Test
    fun `the icon and the lens carry transparency`() {
        for (name in listOf("atlas_goggles", "atlas_goggles_lens")) {
            val image = ImageIO.read(File(ITEM_TEXTURE_DIR, "$name.png"))
            assertTrue(image.colorModel.hasAlpha(), "$name has no alpha channel")
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `the worn model stays inside the bounds Minecraft accepts`() {
        val elements = read(File(ITEM_MODEL_DIR, "$WORN.json"))["elements"] as List<Map<String, List<Number>>>

        for (element in elements) {
            for (v in element.getValue("from") + element.getValue("to")) {
                assertTrue(v.toDouble() in -16.0..32.0, "element $element leaves -16..32")
            }
        }
    }
}
