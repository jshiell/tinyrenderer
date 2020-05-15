package org.infernus.tinyrenderer

import kotlin.math.sqrt

data class Vector3(val x: Double, val y: Double, val z: Double) {
    constructor(x: Number, y: Number, z: Number) : this(x.toDouble(), y.toDouble(), z.toDouble())

    fun cross(v: Vector3) = Vector3(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x)

    fun dot(v: Vector3) = x * v.x + y * v.y + z * v.z

    operator fun plus(v: Vector3) = Vector3(x + v.x, y + v.y, z + v.z)

    operator fun minus(v: Vector3) = Vector3(x - v.x, y - v.y, z - v.z)

    operator fun times(v: Vector3) = Vector3(x * v.x, y * v.y, z * v.z)

    operator fun times(v: Double) = Vector3(x * v, y * v, z * v)

    operator fun div(v: Double) = Vector3(x / v, y / v, z / v)

    fun magnitude() = sqrt(x * x + y * y + z * z)

    fun toMatrix(): Matrix = Matrix(4, 1).also {
        it[0, 0] = x
        it[1, 0] = y
        it[2, 0] = z
        it[3, 0] = 1.0
    }

    fun normalise(): Vector3 {
        val l = 1.0 / magnitude()
        return Vector3(x * l, y * l, z * l)
    }
}

data class Vector4(val x: Double, val y: Double, val z: Double, val w: Double) {
    fun toMatrix(): Matrix = Matrix(4, 1).also {
        it[0, 0] = x
        it[1, 0] = y
        it[2, 0] = z
        it[3, 0] = w
    }

}
