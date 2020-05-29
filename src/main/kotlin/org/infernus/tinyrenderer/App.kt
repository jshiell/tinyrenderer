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
import kotlin.math.max
import kotlin.math.min

class App {

    fun renderModelFile() {
        val model = WavefrontObjectParser().parse(pathOf("/african_head/african_head.obj"))
        val modelDiffuseTexture = ImageIO.read(urlOf("/african_head/african_head_diffuse.png"))
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
        val tinyGL = TinyGL(width, height, BLACK, BOTTOM_LEFT)
                .lookAt(eye, centre, up)
                .projection(-1.0 / (eye - centre).magnitude())
                .viewport(width / 8, height / 8, width * 3 / 4, height * 3 / 4)

        model.faces.forEach { face ->
            val shader = FaceShader(face, lightDirection, diffuseTexture)
            tinyGL.renderTriangle(face.toTriangle(), shader)
        }

        return tinyGL.asBufferedImage()
    }

    private fun Face.toTriangle() =
            Triangle(first.vertex.toVector4(), second.vertex.toVector4(), third.vertex.toVector4())

    private fun Vertex.toVector4() = Vector4(x, y, z, 1.0)

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

class FaceShader(private val face: Face,
                 private val lightDirection: Vector3,
                 private val diffuseTexture: BufferedImage) : Shader {

    private var intensity: Vector3? = null

    override fun vertex(triangle: Triangle<Vector4>, viewport: Matrix, projection: Matrix, modelView: Matrix): Triangle<Vector4> {
        intensity = if (face.first.normal != null && face.second.normal != null && face.third.normal != null) {
            Vector3(face.first.normal.toVector3().normalise().dot(lightDirection),
                    face.second.normal.toVector3().normalise().dot(lightDirection),
                    face.third.normal.toVector3().normalise().dot(lightDirection))
        } else {
            Vector3(0.0, 0.0, 0.0)
        }
        return Triangle(
                (viewport * projection * modelView * triangle.vertex1).toVector4(),
                (viewport * projection * modelView * triangle.vertex2).toVector4(),
                (viewport * projection * modelView * triangle.vertex3).toVector4()
        )
    }

    override fun fragment(vertex: Vector3): FragmentResult {
        val currentIntensity = intensity
        if (currentIntensity != null) {
            val lightIntensity = max(0.0, min(1.0, vertex.dot(currentIntensity)))

            val textureCoordinates = face.textureCoordinates()
            val colour = if (textureCoordinates != null) {
                val textureVector = (textureCoordinates.vertex1 * vertex.x) +
                        (textureCoordinates.vertex2 * vertex.y) +
                        (textureCoordinates.vertex3 * vertex.z)
                diffuseTexture.colourAt(textureVector) * lightIntensity
            } else {
                Colour.WHITE * lightIntensity
            }
            return FragmentResult.SetColour(colour)
        } else {
            return FragmentResult.Discard
        }
    }

    private fun Face.textureCoordinates() = if (first.textureCoordinate != null && second.textureCoordinate != null && third.textureCoordinate != null) {
        Triangle(first.textureCoordinate.toVector3(), second.textureCoordinate.toVector3(), third.textureCoordinate.toVector3())
    } else {
        null
    }

    private fun TextureCoordinate.toVector3() = Vector3(x, y, z)

    private fun Normal.toVector3() = Vector3(x, y, z)

    private fun BufferedImage.colourAt(coordinates: Vector3) = Colour(getRGB((coordinates.x * width).toInt(), height - (coordinates.y * height).toInt()))

}

fun main() {
    App().renderModelFile()
}
