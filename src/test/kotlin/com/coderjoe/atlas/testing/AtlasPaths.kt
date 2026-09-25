package com.coderjoe.atlas.testing

import java.io.File

object AtlasPaths {
    val RESOURCES = File("src/main/resources/atlas")
    private val CONFIG_DIR = File(RESOURCES, "configuration")
    val BLOCK_MODEL_DIR = File(RESOURCES, "resourcepack/assets/minecraft/models/block/custom")
    val ITEM_MODEL_DIR = File(RESOURCES, "resourcepack/assets/minecraft/models/item/custom")
    val BLOCK_TEXTURE_DIR = File(RESOURCES, "resourcepack/assets/minecraft/textures/block/custom")
    val ITEM_TEXTURE_DIR = File(RESOURCES, "resourcepack/assets/minecraft/textures/item/custom")
    val ITEM_DEFINITION_DIR = File(RESOURCES, "resourcepack/assets/atlas/items")

    fun configFiles(): List<File> {
        val files = CONFIG_DIR.walkTopDown().filter { it.isFile && it.extension == "yml" }.toList()
        check(files.isNotEmpty()) { "no .yml configs found under $CONFIG_DIR" }
        return files
    }

    fun config(name: String): File {
        return configFiles().single { it.name == name }
    }
}
