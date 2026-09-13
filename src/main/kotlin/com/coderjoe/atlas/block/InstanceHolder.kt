package com.coderjoe.atlas.block

abstract class InstanceHolder<T> {
    var instance: T? = null
        protected set
}