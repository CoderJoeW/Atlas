package com.coderjoe.atlas.util

import java.util.logging.Logger

fun Logger.atlasInfo(message: String) {
    if (AtlasConfig.loggingEnabled) {
        info(message)
    }
}