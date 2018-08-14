package com.example.android.bluetoothlegatt

data class Vector(val x: Double, val y: Double, val z: Double) {
    constructor(x: Int, y: Int, z: Int) : this(x.toDouble(), y.toDouble(), z.toDouble())

    fun scale(min: Vector, max: Vector): Vector {
        return Vector(
                Math.max(-1.0, Math.min(1.0, (x - min.x) / (max.x - min.x) * 2.0 - 1.0)),
                Math.max(-1.0, Math.min(1.0, (y - min.y) / (max.y - min.y) * 2.0 - 1.0)),
                Math.max(-1.0, Math.min(1.0, (z - min.z) / (max.z - min.z) * 2.0 - 1.0))
        )
    }

    fun scale(s: Double): Vector {
        return Vector(x * s, y * s, z * s)
    }

    fun add(v: Vector): Vector {
        return Vector(x + v.x, y + v.y, z + v.z)
    }

    fun subtract(v: Vector): Vector {
        return Vector(x - v.x, y - v.y, z - v.z)
    }

    fun multiply(v: Vector): Vector {
        return Vector(x * v.x, y * v.y, z * v.z)
    }
}