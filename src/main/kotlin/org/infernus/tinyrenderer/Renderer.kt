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

    fun drawModel(model: WavefrontObject) {
        val lightDirection = Point3(0, 0, -1)
        model.faces.forEach { face ->
            val world1 = face.vertex1.toPoint()
            val world2 = face.vertex2.toPoint()
            val world3 = face.vertex3.toPoint()
            val normal = (world3 - world1).cross(world2 - world1).normalise()
            val intensity = normal.dot(lightDirection)
            if (intensity > 0) {
                drawFilledTriangle(face.vertex1.toScreen(width, height),
                        face.vertex2.toScreen(width, height),
                        face.vertex3.toScreen(width, height),
                        Colour((255 * intensity).toInt(), (255 * intensity).toInt(), (255 * intensity).toInt()))
            }
        }
    }

    fun drawFilledTriangle(point1: Point2, point2: Point2, point3: Point2, colour: Colour) {
        val bounds = boundingBox(listOf(point1, point2, point3))

        for (x in (bounds.fromX..bounds.toX)) {
            for (y in (bounds.fromY..bounds.toY)) {
                val screen = barycentric(point1, point2, point3, Point2(x, y))
                if (screen.x >= 0 && screen.y >= 0 && screen.z >= 0) {
                    setPixel(x, y, colour)
                }
            }
        }
    }

    private fun boundingBox(points: List<Point2>): Rectangle {
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

    private fun barycentric(point1: Point2, point2: Point2, point3: Point2, testPoint: Point2): Point3 {
        val u = Point3(point3.x - point1.x, point2.x - point1.x, point1.x - testPoint.x)
                .cross(Point3(point3.y - point1.y, point2.y - point1.y, point1.y - testPoint.y))
        return if (abs(u.z) < 1) {
            Point3(-1.0, 1.0, 1.0)
        } else {
            Point3(1.0 - (u.x + u.y) / u.z, u.y / u.z, u.x / u.z)
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

data class Rectangle(val fromX: Int, val fromY: Int, val toX: Int, val toY: Int)

data class Point2(val x: Double, val y: Double) {
    constructor(x: Number, y: Number) : this(x.toDouble(), y.toDouble())

    operator fun plus(v: Point2) = Point2(x + v.x, y + v.y)

    operator fun minus(v: Point2) = Point2(x - v.x, y - v.y)

    operator fun times(v: Double) = Point2(x * v, y * v)
}

data class Point3(val x: Double, val y: Double, val z: Double) {
    constructor(x: Number, y: Number, z: Number) : this(x.toDouble(), y.toDouble(), z.toDouble())

    fun toScreen(width: Int, height: Int) = Point2((x + 1.0) * width / 2, (y + 1.0) * height / 2)

    fun cross(v: Point3) = Point3(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x)

    fun dot(v: Point3) = x * v.x + y * v.y + z * v.z

    operator fun minus(v: Point3) = Point3(x - v.x, y - v.y, z - v.z)

    operator fun times(v: Point3) = Point3(x * v.x, y * v.y, z * v.z)

    fun magnitude() = sqrt(x * x + y * y + z * z)

    fun normalise(): Point3 {
        val l = 1.0 / magnitude()
        return Point3(x * l, y * l, z * l)
    }
}
