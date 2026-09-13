package com.coderjoe.atlas.testing

import java.io.File

object AtlasPaths {
    val RESOURCES = File("src/main/resources/atlas")
    val CONFIG_DIR = File(RESOURCES, "configuration")
    val BLOCK_MODEL_DIR = File(RESOURCES, "resourcepack/assets/minecraft/models/block/custom")
    val ITEM_MODEL_DIR = File(RESOURCES, "resourcepack/assets/minecraft/models/item/custom")
    val BLOCK_TEXTURE_DIR = File(RESOURCES, "resourcepack/assets/minecraft/textures/block/custom")
    val ITEM_TEXTURE_DIR = File(RESOURCES, "resourcepack/assets/minecraft/textures/item/custom")

    fun configFiles(): List<File> = CONFIG_DIR.walkTopDown().filter {
        it.extension == "yml"
    }.toList()

    fun config(name: String): File {
        return configFiles().single() { it.name == name }
    }
}