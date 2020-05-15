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

    fun renderModelFile() {
        val model = WavefrontObjectParser().parse(pathOf("/african_head.obj"))
        val modelDiffuseTexture = ImageIO.read(urlOf("/african_head_diffuse.png"))
        val image = renderModel(model, modelDiffuseTexture)
        showImageInFrame(image)
    }

    private fun renderModel(model: WavefrontObject, diffuseTexture: BufferedImage): BufferedImage {
        val eye = Vector3(1, 1, 3)
        val centre = Vector3(0, 0, 0)
        val up = Vector3(0, 1, 0)
        val lightDirection = Vector3(1, -1, 1).normalise()
        val width = 800
        val height = 800
        val renderer = Renderer(width, height, 255, BLACK, BOTTOM_LEFT)
                .lookAt(eye, centre, up)
                .projection(-1.0 / (eye - centre).magnitude())
                .viewport(width / 8, height / 8, width * 3 / 4, height * 3 / 4)

        model.faces.forEach { face ->
            val worldVertex1 = face.first.vertex.toVector3()
            val worldVertex2 = face.second.vertex.toVector3()
            val worldVertex3 = face.third.vertex.toVector3()

            renderer.renderTriangle(
                    Triangle(worldVertex1, worldVertex2, worldVertex3),
                    face.textureCoordinates(),
                    diffuseTexture,
                    lightIntensities(lightDirection, face, worldVertex1, worldVertex2, worldVertex3))
        }


        return renderer.asBufferedImage()
    }

    private fun lightIntensities(lightDirection: Vector3,
                                 face: Face,
                                 worldVertex1: Vector3,
                                 worldVertex2: Vector3,
                                 worldVertex3: Vector3): Intensities =
            if (face.first.normal == null || face.second.normal == null || face.third.normal == null) {
                val intensity = (worldVertex3 - worldVertex1).cross(worldVertex2 - worldVertex1).normalise().dot(lightDirection)
                Intensities(intensity, intensity, intensity)
            } else {
                Intensities(
                        face.first.normal.toVector3().normalise().dot(lightDirection),
                        face.second.normal.toVector3().normalise().dot(lightDirection),
                        face.third.normal.toVector3().normalise().dot(lightDirection))
            }


    private fun Vertex.toVector3() = Vector3(x, y, z)

    private fun Normal.toVector3() = Vector3(x, y, z)

    private fun TextureCoordinate.toVector3() = Vector3(x, y, z)

    private fun Face.textureCoordinates() = if (first.textureCoordinate != null && second.textureCoordinate != null && third.textureCoordinate != null) {
        Triangle(first.textureCoordinate.toVector3(), second.textureCoordinate.toVector3(), third.textureCoordinate.toVector3())
    } else {
        null
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
    App().renderModelFile()
}
