package org.infernus.tinyrenderer

import org.infernus.tinyrenderer.Colour.Companion.BLACK
import org.infernus.tinyrenderer.Origin.BOTTOM_LEFT
import java.awt.FlowLayout
import java.awt.image.BufferedImage
import java.net.URL
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel


class App {

    fun showExample() {
        val renderer = Renderer(800, 800, 255, BLACK, BOTTOM_LEFT)

        val model = WavefrontObjectParser().parse(pathOf("/african_head.obj"))
        val modelDiffuseTexture = ImageIO.read(urlOf("/african_head_diffuse.png"))
        renderer.drawModel(model, modelDiffuseTexture)

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

    private fun urlOf(classpathPath: String): URL = App::class.java.getResource(classpathPath)

}

fun main() {
    App().showExample()
}
