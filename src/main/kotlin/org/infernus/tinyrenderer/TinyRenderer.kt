package org.infernus.tinyrenderer

import org.infernus.tinyrenderer.Colour.*
import org.infernus.tinyrenderer.Origin.*
import java.awt.FlowLayout
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import kotlin.math.abs


class TinyRenderer(private val width: Int,
                   private val height: Int,
                   private val initialColour: Colour = BLACK,
                   private val origin: Origin = TOP_LEFT) {

    private val pixels = IntArray(width * height) { initialColour.rawValue }

    fun drawLine(fromX: Int, fromY: Int, toX: Int, toY: Int, colour: Colour) {
        val steep = abs(fromX - toX) < abs(fromY - toY)

        var x0 = if (steep) fromY else fromX
        var y0 = if (steep) fromX else fromY
        var x1 = if (steep) toY else toX
        var y1 = if (steep) toX else toY

        val leftToRight = x0 > x1
        if (leftToRight) {
            var tempCopy = x0
            x0 = x1
            x1 = tempCopy
            tempCopy = y0
            y0 = y1
            y1 = tempCopy
        }

        var x = x0
        while (x <= x1) {
            val t = (x - x0).toFloat() / (x1 - x0).toFloat()
            val y = (y0 * (1f - t) + y1 * t).toInt()
            if (steep) {
                pixels[x + y * width] = colour.rawValue
            } else {
                pixels[y + x * width] = colour.rawValue
            }
            x += 1
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

fun main() {
    val renderer = TinyRenderer(100, 100, BLACK, BOTTOM_LEFT)

    renderer.drawLine(13, 20, 80, 40, WHITE)
    renderer.drawLine(20, 13, 40, 80, RED)
    renderer.drawLine(80, 40, 13, 20, RED)

    showImageInFrame(renderer.asBufferedImage())
}

private fun showImageInFrame(image: BufferedImage) {
    val frame = JFrame()
    frame.contentPane.layout = FlowLayout()
    frame.contentPane.add(JLabel(ImageIcon(image)))
    frame.pack()
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.isVisible = true
}
