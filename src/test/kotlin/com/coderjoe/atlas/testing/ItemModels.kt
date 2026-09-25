package com.coderjoe.atlas.testing

/** Reads item model definitions, whether they come from a CraftEngine config or the pack's `items/` folder. */
object ItemModels {
    /** Keys of an item model definition that hold further model definitions. */
    private val NESTED_MODEL_KEYS = setOf("cases", "fallback", "model", "on_true", "on_false")

    /** The file name a namespaced model or texture reference points at. */
    fun leaf(reference: String) = reference.substringAfterLast("/")

    /**
     * Flattens a `model` into every concrete model it can resolve to.
     *
     * A plain string is one model. A model definition may instead branch - a `minecraft:select` on
     * the display context, say - so each branch has to be checked, not just the first one found.
     */
    @Suppress("UNCHECKED_CAST")
    fun resolve(node: Any?): List<Map<String, Any?>> =
        when (node) {
            is String -> listOf(mapOf("path" to node))
            is List<*> -> node.flatMap(::resolve)
            is Map<*, *> -> {
                val map = node as Map<String, Any?>
                val self = if (map["path"] is String) listOf(map) else emptyList()
                self + NESTED_MODEL_KEYS.flatMap { resolve(map[it]) }
            }
            else -> emptyList()
        }
}
