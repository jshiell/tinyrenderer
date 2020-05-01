package org.infernus.tinyrenderer

import java.awt.FlowLayout
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel


class TinyRenderer(private val width: Int,
                   private val height: Int) {

    private val pixels = IntArray(width * height)

    fun drawLine(x0: Int, y0: Int, x1: Int, y1: Int, colour: Colour) {
        var t = 0.0f
        while (t < 1.0) {
            val x = (x0 + (x1 - x0) * t).toInt()
            val y = (y0 + (y1 - y0) * t).toInt()
            pixels[x + y * width] = colour.rawValue
            t += .01f
        }
    }

    fun asBufferedImage(): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val imagePixels = (image.raster.dataBuffer as DataBufferInt).data
        IntRange(0, pixels.size - 1).forEach { index ->
            imagePixels[index] = pixels[index]
        }
        return image
    }
}

enum class Colour(val rawValue: Int) {
    WHITE(0xFFFFFF)
}

fun main() {
    val renderer = TinyRenderer(100, 100)
    renderer.drawLine(20, 20, 80, 70, Colour.WHITE)

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
