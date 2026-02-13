package com.mrpowergamerbr.butterscotch.vm

sealed class GMLValue {
    data class Real(val value: Double) : GMLValue()
    data class Str(val value: String) : GMLValue()
    data class ArrayVal(val data: MutableMap<Int, MutableMap<Int, GMLValue>> = mutableMapOf()) : GMLValue()
    data object Undefined : GMLValue()

    fun toReal(): Double = when (this) {
        is Real -> value
        is Str -> value.toDoubleOrNull() ?: 0.0
        is ArrayVal -> 0.0
        is Undefined -> 0.0
    }

    fun toInt(): Int = toReal().toInt()
    fun toLong(): Long = toReal().toLong()

    fun toStr(): String = when (this) {
        is Real -> if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
        is Str -> value
        is ArrayVal -> "<array>"
        is Undefined -> "<undefined>"
    }

    fun toBool(): Boolean = when (this) {
        is Real -> value >= 0.5
        is Str -> value.isNotEmpty()
        is ArrayVal -> true
        is Undefined -> false
    }

    companion object {
        val ZERO = Real(0.0)
        val ONE = Real(1.0)
        val EMPTY_STRING = Str("")
        val TRUE = Real(1.0)
        val FALSE = Real(0.0)

        fun of(value: Double) = Real(value)
        fun of(value: Int) = Real(value.toDouble())
        fun of(value: String) = Str(value)
        fun of(value: Boolean) = if (value) TRUE else FALSE
    }
}
