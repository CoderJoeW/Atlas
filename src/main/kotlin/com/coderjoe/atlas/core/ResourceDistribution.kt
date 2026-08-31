package com.coderjoe.atlas.core

import org.bukkit.block.BlockFace

fun <T : AtlasBlock> pullFromOpposite(
    facing: BlockFace,
    getAdjacent: (BlockFace) -> T?,
    canPullSelf: () -> Boolean,
    canProvide: (source: T) -> Boolean,
    transfer: (source: T) -> Unit,
) {
    if (!canPullSelf()) return
    val source = getAdjacent(facing.oppositeFace) ?: return
    if (!canProvide(source)) return
    transfer(source)
}

fun <T : AtlasBlock> pushRoundRobin(
    excludeFace: BlockFace,
    startIndex: Int,
    getAdjacent: (BlockFace) -> T?,
    hasResource: () -> Boolean,
    isCandidate: (target: T) -> Boolean,
    tryPush: (target: T, face: BlockFace) -> Boolean,
    stopAfterFirstCandidate: Boolean = false,
): Int {
    val outputFaces = AtlasBlock.ADJACENT_FACES.filter { it != excludeFace }
    val faceCount = outputFaces.size
    var lastPushOffset = -1

    for (i in outputFaces.indices) {
        if (!hasResource()) break
        val face = outputFaces[(startIndex + i) % faceCount]
        val target = getAdjacent(face) ?: continue
        if (!isCandidate(target)) continue
        if (tryPush(target, face)) {
            lastPushOffset = i
        }
        if (stopAfterFirstCandidate) break
    }

    return if (lastPushOffset >= 0) (startIndex + lastPushOffset + 1) % faceCount else startIndex
}

fun <T : AtlasBlock> pullFromAll(
    excludeFace: BlockFace,
    getAdjacent: (BlockFace) -> T?,
    isDone: () -> Boolean,
    isCandidate: (source: T, face: BlockFace) -> Boolean,
    tryPull: (source: T, face: BlockFace) -> Boolean,
    stopAfterFirstCandidate: Boolean = false,
) {
    val inputFaces = AtlasBlock.ADJACENT_FACES.filter { it != excludeFace }
    for (face in inputFaces) {
        if (isDone()) break
        val source = getAdjacent(face) ?: continue
        if (!isCandidate(source, face)) continue
        tryPull(source, face)
        if (stopAfterFirstCandidate) break
    }
}
