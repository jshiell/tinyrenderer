package org.infernus.tinyrenderer

import org.infernus.tinyrenderer.Colour.Companion.BLACK
import org.infernus.tinyrenderer.Origin.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


class Renderer(private val width: Int,
               private val height: Int,
               private val initialColour: Colour = BLACK,
               private val origin: Origin = TOP_LEFT) {

    private val pixels = IntArray(width * height) { initialColour.rawValue }
    private val zBuffer = IntArray(width * height) { Integer.MIN_VALUE }

    fun drawModel(model: WavefrontObject) {
        val lightDirection = Vector3(0, 0, -1)
        model.faces.forEach { face ->
            val world1 = face.vertex1.toVector3()
            val normal = (face.vertex3.toVector3() - world1)
                    .cross(face.vertex2.toVector3() - world1).normalise()
            val intensity = normal.dot(lightDirection)
            if (intensity > 0) {
                renderTriangle(
                        Triangle(face.vertex1.toScreen(), face.vertex2.toScreen(), face.vertex3.toScreen()),
                        Colour((255 * intensity).toInt(), (255 * intensity).toInt(), (255 * intensity).toInt()))
            }
        }
    }

    private fun Vertex.toScreen(): Vector3 = Vector3((x + 1.0) * width / 2.0, (y + 1.0) * height / 2.0, z)

    private fun renderTriangle(triangle: Triangle, colour: Colour) {
        val bounds = boundingBox(triangle.pointsAsList())

        for (x in (bounds.fromX..bounds.toX)) {
            for (y in (bounds.fromY..bounds.toY)) {
                val screen = barycentric(triangle, Vector3(x, y, 0))
                if (screen.x >= 0 && screen.y >= 0 && screen.z >= 0) {
                    val z = (triangle.firstPoint.z * screen.x + triangle.secondPoint.z * screen.y + triangle.thirdPoint.z * screen.x).toInt()
                    if (zBuffer[x + y * width] < z) {
                        zBuffer[x + y * width] = z
                        setPixel(x, y, colour)
                    }
                }
            }
        }
    }

    private fun boundingBox(points: List<Vector3>): Rectangle {
        var minX = width - 1
        var minY = height - 1
        var maxX = 0
        var maxY = 0
        points.forEach { point ->
            minX = max(0, min(minX, point.x.toInt()))
            minY = max(0, min(minY, point.y.toInt()))
            maxX = min(width - 1, max(maxX, point.x.toInt()))
            maxY = min(height - 1, max(maxY, point.y.toInt()))
        }
        return Rectangle(minX, minY, maxX, maxY)
    }

    private fun barycentric(triangle: Triangle, testPoint: Vector3): Vector3 {
        val u = Vector3(triangle.thirdPoint.x - triangle.firstPoint.x,
                triangle.secondPoint.x - triangle.firstPoint.x,
                triangle.firstPoint.x - testPoint.x)
                .cross(Vector3(triangle.thirdPoint.y - triangle.firstPoint.y,
                        triangle.secondPoint.y - triangle.firstPoint.y,
                        triangle.firstPoint.y - testPoint.y))
        return if (abs(u.z) > 0.01) {
            Vector3(1.0 - (u.x + u.y) / u.z, u.y / u.z, u.x / u.z)
        } else {
            Vector3(-1.0, 1.0, 1.0)
        }
    }

    private fun setPixel(x: Int, y: Int, colour: Colour) {
        val offset = x + y * width
        if (offset >= 0 && offset < pixels.size) {
            pixels[offset] = colour.rawValue
        }
    }

    fun asBufferedImage(): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val imagePixels = (image.raster.dataBuffer as DataBufferInt).data
        IntRange(0, pixels.size - 1).forEach { index ->
            val destinationIndex = when (origin) {
                TOP_LEFT -> index
                TOP_RIGHT -> {
                    val yPos = index / width
                    val xPos = width - (index % width) - 1
                    yPos * width + xPos
                }
                BOTTOM_LEFT -> {
                    val yPos = height - (index / width) - 1
                    val xPos = index % width
                    yPos * width + xPos
                }
                BOTTOM_RIGHT -> {
                    val yPos = height - (index / width) - 1
                    val xPos = width - (index % width) - 1
                    yPos * width + xPos
                }
            }
            imagePixels[destinationIndex] = pixels[index]
        }
        return image
    }
}

enum class Origin {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

class Colour(val rawValue: Int) {
    constructor(red: Int, green: Int, blue: Int) : this(
            red.shl(16).and(0xFF0000)
                    + green.shl(8).and(0xFF00)
                    + blue.and(0xFF))

    companion object {
        val BLACK = Colour(0x000000)
    }
}

data class Triangle(val firstPoint: Vector3, val secondPoint: Vector3, val thirdPoint: Vector3) {
    fun pointsAsList() = listOf(firstPoint, secondPoint, thirdPoint)
}

data class Rectangle(val fromX: Int, val fromY: Int, val toX: Int, val toY: Int)

data class Vector3(val x: Double, val y: Double, val z: Double) {
    constructor(x: Number, y: Number, z: Number) : this(x.toDouble(), y.toDouble(), z.toDouble())

    fun cross(v: Vector3) = Vector3(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x)

    fun dot(v: Vector3) = x * v.x + y * v.y + z * v.z

    operator fun minus(v: Vector3) = Vector3(x - v.x, y - v.y, z - v.z)

    operator fun times(v: Vector3) = Vector3(x * v.x, y * v.y, z * v.z)

    fun magnitude() = sqrt(x * x + y * y + z * z)

    fun normalise(): Vector3 {
        val l = 1.0 / magnitude()
        return Vector3(x * l, y * l, z * l)
    }
}
