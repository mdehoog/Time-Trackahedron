package com.example.android.bluetoothlegatt

data class Quaternion(val w: Double, val x: Double, val y: Double, val z: Double) {

    companion object {
        fun fromYawPitchRoll(v: Vector): Quaternion {
            return fromYawPitchRoll(v.y, v.z, v.x)
        }

        fun fromYawPitchRoll(pitch: Double, roll: Double, yaw: Double): Quaternion {
            val cy = Math.cos(yaw * 0.5)
            val sy = Math.sin(yaw * 0.5)
            val cr = Math.cos(roll * 0.5)
            val sr = Math.sin(roll * 0.5)
            val cp = Math.cos(pitch * 0.5)
            val sp = Math.sin(pitch * 0.5)

            val w = cy * cr * cp + sy * sr * sp
            val x = cy * sr * cp - sy * cr * sp
            val y = cy * cr * sp + sy * sr * cp
            val z = sy * cr * cp - cy * sr * sp
            return Quaternion(w, x, y, z)
        }
    }

    fun innerProduct(q: Quaternion): Double {
        return w * q.w + x * q.x + y * q.y + z * q.z
    }

    fun distance(q: Quaternion): Double {
        val p = innerProduct(q)
        return 1 - p * p
    }

    fun angle(q: Quaternion): Double {
        val p = innerProduct(q)
        return Math.acos(2 * p * p - 1)
    }
}