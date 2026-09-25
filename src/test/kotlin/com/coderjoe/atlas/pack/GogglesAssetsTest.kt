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
 * item definition, model and texture are hand-placed in the pack. A broken link anywhere in that
 * chain shows the missing-model cube in game with nothing logged, so the chain is walked here.
 */
class GogglesAssetsTest {
    private val itemDefinition = File(RESOURCES, "resourcepack/assets/atlas/items/atlas_goggles.json")

    /** JSON is a subset of YAML, which saves pulling in a JSON parser for three small files. */
    @Suppress("UNCHECKED_CAST")
    private fun read(file: File): Map<String, Any?> = Yaml().load(file.readText()) as Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun modelName(): String {
        val model = read(itemDefinition)["model"] as Map<String, Any?>
        return (model["model"] as String).substringAfterLast("/")
    }

    @Test
    fun `the item definition exists`() {
        assertTrue(itemDefinition.isFile, "missing $itemDefinition")
    }

    @Test
    fun `the item definition points at a model that exists`() {
        val model = File(ITEM_MODEL_DIR, "${modelName()}.json")

        assertTrue(model.isFile, "missing $model")
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `the model's texture exists and is a 512 pixel square with transparency`() {
        val model = read(File(ITEM_MODEL_DIR, "${modelName()}.json"))
        val layer = (model["textures"] as Map<String, Any?>)["layer0"] as String
        val texture = File(ITEM_TEXTURE_DIR, "${layer.substringAfterLast("/")}.png")

        assertTrue(texture.isFile, "missing $texture")
        val image = ImageIO.read(texture)
        assertEquals(512, image.width)
        assertEquals(512, image.height)
        assertTrue(image.colorModel.hasAlpha(), "$texture has no alpha channel, so its backdrop would show")
    }
}
