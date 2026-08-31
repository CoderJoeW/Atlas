package com.coderjoe.atlas.core

abstract class InstanceHolder<T> {
    var instance: T? = null
        protected set
}
