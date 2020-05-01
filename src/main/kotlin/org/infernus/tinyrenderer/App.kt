package org.infernus.tinyrenderer

import org.infernus.tinyrenderer.Colour.BLACK
import org.infernus.tinyrenderer.Origin.BOTTOM_LEFT
import java.awt.FlowLayout
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel


class App {

    fun showExample() {
        val renderer = Renderer(200, 200, BLACK, BOTTOM_LEFT)

//        val model = WavefrontObjectParser().parse(pathOf("/african_head.obj"))
//        renderer.drawModel(model)

        renderer.drawTriangle(Point2(10, 70), Point2(50, 160), Point2(70, 80), Colour.RED)
        renderer.drawTriangle(Point2(180, 50), Point2(150, 1), Point2(70, 180), Colour.WHITE)
        renderer.drawTriangle(Point2(180, 150), Point2(120, 160), Point2(130, 180), Colour.GREEN)

        showImageInFrame(renderer.asBufferedImage())
    }

    private fun showImageInFrame(image: BufferedImage) {
        val frame = JFrame()
        frame.title = "TinyRenderer"
        frame.contentPane.layout = FlowLayout()
        frame.contentPane.add(JLabel(ImageIcon(image)))
        frame.pack()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isVisible = true
    }

    private fun pathOf(classpathPath: String): Path = Path.of(App::class.java.getResource(classpathPath).toURI())

}

fun main() {
    App().showExample()
}
