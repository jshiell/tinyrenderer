package org.infernus.tinyrenderer

class Matrix(val rows: Int, val columns: Int) {
    private val values = DoubleArray(columns * rows) { 0.0 }

    operator fun get(row: Int, column: Int) = values[column + (row * columns)]

    operator fun set(row: Int, column: Int, value: Double) {
        values[column + (row * columns)] = value
    }

    operator fun times(multiplicand: Matrix): Matrix {
        if (columns != multiplicand.rows) {
            throw IllegalArgumentException("Multiplier columns ($columns) does not match multiplicand rows (${multiplicand.rows})")
        }

        val result = Matrix(rows, multiplicand.columns)
        for (row in 0 until rows) {
            for (column in 0 until multiplicand.columns) {
                result[row, column] = 0.0
                for (k in 0 until columns) {
                    result[row, column] += this[row, k] * multiplicand[k, column]
                }
            }
        }
        return result
    }

    operator fun times(vector3: Vector3) = this * vector3.toMatrix()

    operator fun times(vector4: Vector4) = this * vector4.toMatrix()

    fun toVector4() = Vector4(this[0, 0]/ this[3, 0], this[1, 0] / this[3, 0], this[2, 0] / this[3, 0], this[3, 0])

    fun toVector3() = Vector3(this[0, 0] / this[3, 0], this[1, 0] / this[3, 0], this[2, 0] / this[3, 0])

    companion object {
        fun identity(dimensions: Int) = Matrix(dimensions, dimensions).also {
            for (row in 0 until dimensions) {
                for (column in 0 until dimensions) {
                    it[row, column] = if (row == column) 1.0 else 0.0
                }
            }
        }
    }
}
