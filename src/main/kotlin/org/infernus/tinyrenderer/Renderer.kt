package org.infernus.tinyrenderer

import org.infernus.tinyrenderer.Colour.Companion.BLACK
import org.infernus.tinyrenderer.Origin.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


class Renderer(private val width: Int,
               private val height: Int,
               private val initialColour: Colour = BLACK,
               private val origin: Origin = TOP_LEFT) {

    private val pixels = IntArray(width * height) { initialColour.rawValue }

    fun drawModel(model: WavefrontObject) {
        model.faces.forEach { face ->
            drawFilledTriangle(face.vertex1.toScreen(width, height),
                    face.vertex2.toScreen(width, height),
                    face.vertex3.toScreen(width, height),
                    Colour.random())
        }
    }

    fun drawTriangle(point1: Point2, point2: Point2, point3: Point2, colour: Colour) {
        drawLine(point1, point2, colour)
        drawLine(point2, point3, colour)
        drawLine(point3, point1, colour)
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
        val u = cross(Point3(point3.x - point1.x, point2.x - point1.x, point1.x - testPoint.x),
                Point3(point3.y - point1.y, point2.y - point1.y, point1.y - testPoint.y))
        return if (abs(u.z) < 1) {
            Point3(-1.0, 1.0, 1.0)
        } else {
            Point3(1.0 - (u.x + u.y) / u.z, u.y / u.z, u.x / u.z)
        }
    }

    private fun cross(v1: Point3, v2: Point3) = Point3(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x)

    fun drawLine(start: Point2, end: Point2, colour: Colour) {
        val steep = abs(start.x - end.x) < abs(start.y - end.y)

        var x0 = (if (steep) start.y else start.x).toInt()
        var y0 = (if (steep) start.x else start.y).toInt()
        var x1 = (if (steep) end.y else end.x).toInt()
        var y1 = (if (steep) end.x else end.y).toInt()

        val leftToRight = x0 > x1
        if (leftToRight) {
            var tempCopy = x0
            x0 = x1
            x1 = tempCopy
            tempCopy = y0
            y0 = y1
            y1 = tempCopy
        }

        val deltaX = x1 - x0
        val deltaY = y1 - y0
        val deltaError = abs(deltaY) * 2
        var error = 0
        var y = y0
        for (x in x0..x1) {
            if (steep) {
                setPixel(y, x, colour)
            } else {
                setPixel(x, y, colour)
            }
            error += deltaError
            if (error > deltaX) {
                y += if (y1 > y0) 1 else -1
                error -= deltaX * 2
            }
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
    companion object {
        val WHITE = Colour(0xFFFFFF)
        val RED = Colour(0xFF0000)
        val GREEN = Colour(0x00FF00)
        val BLACK = Colour(0x000000)

        fun random() = Colour(Random.nextInt())
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
    fun toScreen(width: Int, height: Int) = Point2((x + 1.0) * width / 2, (y + 1.0) * height / 2)
}
