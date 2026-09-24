package com.coderjoe.atlas.block

import com.coderjoe.atlas.data.BlockPersister
import com.coderjoe.atlas.util.atlasInfo
import org.bukkit.plugin.java.JavaPlugin

class AtlasSubsystem(
    private val name: String,
    val registry: BlockRegistry,
    private val factory: BlockFactory,
    val descriptors: Map<String, BlockDescriptor>,
    private val persistence: BlockPersister,
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
