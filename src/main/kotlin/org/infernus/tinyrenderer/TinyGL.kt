package org.infernus.tinyrenderer

import org.infernus.tinyrenderer.Colour.Companion.BLACK
import org.infernus.tinyrenderer.Origin.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class TinyGL(private val width: Int,
             private val height: Int,
             private val depth: Int,
             private val initialColour: Colour = BLACK,
             private val origin: Origin = TOP_LEFT) {

    private val pixels = IntArray(width * height) { initialColour.rawValue }
    private val zBuffer = IntArray(width * height) { Integer.MIN_VALUE }

    private var modelView: Matrix? = null
    private var projection: Matrix? = null
    private var viewport: Matrix? = null

    fun projection(coefficient: Double): TinyGL {
        projection = Matrix.identity(4).also {
            it[3, 2] = coefficient
        }
        return this
    }

    fun lookAt(eye: Vector3, centre: Vector3, up: Vector3): TinyGL {
        val z = (eye - centre).normalise()
        val x = up.cross(z).normalise()
        val y = z.cross(x).normalise()
        val result = Matrix.identity(4)
        result[0, 0] = x.x
        result[0, 1] = x.y
        result[0, 2] = x.z
        result[1, 0] = y.x
        result[1, 1] = y.y
        result[1, 2] = y.z
        result[2, 0] = z.x
        result[2, 1] = z.y
        result[2, 2] = z.z
        result[3, 0] = centre.x
        result[3, 1] = centre.y
        result[3, 2] = centre.z
        modelView = result
        return this
    }

    fun viewport(x: Int, y: Int, width: Int, height: Int): TinyGL {
        viewport = Matrix.identity(4).also {
            it[0, 3] = x + width / 2.0
            it[1, 3] = y + height / 2.0
            it[2, 3] = depth / 2.0

            it[0, 0] = width / 2.0
            it[1, 1] = height / 2.0
            it[2, 2] = depth / 2.0
        }
        return this
    }

    fun renderTriangle(worldTriangle: Triangle,
                       textureCoordinates: Triangle?,
                       diffuseTexture: BufferedImage,
                       lightIntensities: Intensities) {
        val currentProjection = projection
        val currentModelView = modelView
        val currentViewport = viewport
        if (currentProjection == null || currentModelView == null || currentViewport == null) {
            throw IllegalStateException("Projection, modelView, and viewport must be configured first")
        }

        val screenTriangle = Triangle(
                currentViewport * currentProjection * currentModelView * worldTriangle.vertex1,
                currentViewport * currentProjection * currentModelView * worldTriangle.vertex2,
                currentViewport * currentProjection * currentModelView * worldTriangle.vertex3)

        val bounds = boundingBox(screenTriangle.pointsAsList())

        for (x in (bounds.fromX..bounds.toX)) {
            for (y in (bounds.fromY..bounds.toY)) {
                val screen = barycentric(screenTriangle, Vector3(x, y, 0))
                if (screen.x >= 0 && screen.y >= 0 && screen.z >= 0) {
                    val lightIntensity = lightIntensity(lightIntensities, screen)
                    val z = (screenTriangle.vertex1.z * screen.x + screenTriangle.vertex2.z * screen.y + screenTriangle.vertex3.z * screen.z).toInt()
                    if (zBuffer[x + y * width] < z) {
                        zBuffer[x + y * width] = z
                        setPixel(x, y, drawColour(textureCoordinates, screen, diffuseTexture, lightIntensity))
                    }
                }
            }
        }
    }

    private fun drawColour(textureCoordinates: Triangle?, screen: Vector3, diffuseTexture: BufferedImage, lightIntensity: Double) =
            if (textureCoordinates != null) {
                val textureVector = (textureCoordinates.vertex1 * screen.x) +
                        (textureCoordinates.vertex2 * screen.y) +
                        (textureCoordinates.vertex3 * screen.z)
                diffuseTexture.colourAt(textureVector) * lightIntensity
            } else {
                Colour.WHITE * lightIntensity
            }

    private fun lightIntensity(lightIntensities: Intensities, screen: Vector3): Double {
        val lightIntensity = lightIntensities.vertex1 * screen.x +
                lightIntensities.vertex2 * screen.y +
                lightIntensities.vertex3 * screen.z
        return when {
            lightIntensity < 0 -> 0.0
            lightIntensity > 1 -> 1.0
            else -> lightIntensity
        }
    }

    private fun BufferedImage.colourAt(coordinates: Vector3) = Colour(getRGB((coordinates.x * width).toInt(), height - (coordinates.y * height).toInt()))

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
        val u = Vector3(triangle.vertex3.x - triangle.vertex1.x,
                triangle.vertex2.x - triangle.vertex1.x,
                triangle.vertex1.x - testPoint.x)
                .cross(Vector3(triangle.vertex3.y - triangle.vertex1.y,
                        triangle.vertex2.y - triangle.vertex1.y,
                        triangle.vertex1.y - testPoint.y))
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

    operator fun times(intensity: Double) = Colour(
            (rawValue.and(0xFF0000).shr(16) * intensity).toInt(),
            (rawValue.and(0xFF00).shr(8) * intensity).toInt(),
            (rawValue.and(0xFF) * intensity).toInt())

    companion object {
        val BLACK = Colour(0x000000)
        val WHITE = Colour(0xFFFFFF)
    }
}

data class Intensities(val vertex1: Double, val vertex2: Double, val vertex3: Double)

data class Triangle(val vertex1: Vector3, val vertex2: Vector3, val vertex3: Vector3) {

    constructor(vertex1: Matrix, vertex2: Matrix, vertex3: Matrix) : this(vertex1.toVector3(), vertex2.toVector3(), vertex3.toVector3())

    fun pointsAsList() = listOf(vertex1, vertex2, vertex3)
}

data class Rectangle(val fromX: Int, val fromY: Int, val toX: Int, val toY: Int)
