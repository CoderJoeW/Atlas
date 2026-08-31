package com.coderjoe.atlas.core

import com.coderjoe.atlas.atlasInfo
import org.bukkit.plugin.java.JavaPlugin

class AtlasSubsystem<T : AtlasBlock>(
    private val name: String,
    val registry: BlockRegistry<T>,
    private val factory: BlockFactory<T>,
    val descriptors: Map<String, BlockDescriptor>,
    private val persistence: BlockPersister<T>,
    private val plugin: JavaPlugin,
) {
    fun init() {
        factory.registerFromDescriptors(descriptors.values)
        persistence.load(registry)
        val label = name.replaceFirstChar { it.uppercase() }
        val blockTypeCount = factory.getRegisteredBlockIds().size
        plugin.logger.atlasInfo("$label system initialized with $blockTypeCount block types")
    }

    fun save() = persistence.save(registry)

    fun stop() = registry.stopAll()
}
