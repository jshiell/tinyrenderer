package org.infernus.tinyrenderer

import org.infernus.tinyrenderer.Colour.BLACK
import org.infernus.tinyrenderer.Colour.WHITE
import org.infernus.tinyrenderer.Origin.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.abs

class Renderer(private val width: Int,
               private val height: Int,
               private val initialColour: Colour = BLACK,
               private val origin: Origin = TOP_LEFT) {

    private val pixels = IntArray(width * height) { initialColour.rawValue }

    fun drawModel(model: WavefrontObject) {
        model.faces.forEach { face ->
            face.lines().forEach { (start, end) ->
                val startX = (start.x + 1) * width / 2
                val startY = (start.y + 1) * height / 2
                val endX = (end.x + 1) * width / 2
                val endY = (end.y + 1) * height / 2
                drawLine(Point2(startX, startY), Point2(endX, endY), WHITE)
            }
        }
    }

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
        var x = x0
        var y = y0
        while (x <= x1) {
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
            x += 1
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

enum class Colour(val rawValue: Int) {
    WHITE(0xFFFFFF),
    RED(0xFF0000),
    BLACK(0x000000)
}

data class Point2(val x: Double, val y: Double)

data class Point3(val x: Double, val y: Double, val z: Double)
