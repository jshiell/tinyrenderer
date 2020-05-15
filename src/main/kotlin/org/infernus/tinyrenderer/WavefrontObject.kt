package org.infernus.tinyrenderer

import java.nio.file.Files
import java.nio.file.Path

data class WavefrontObject(val vertices: List<Vertex>,
                           val textureCoordinates: List<TextureCoordinate>,
                           val faces: List<Face>)

class WavefrontObjectParser {

    private val vertices = mutableListOf<Vertex>()
    private val textureCoordinates = mutableListOf<TextureCoordinate>()
    private val faceIndices = mutableListOf<FaceIndices>()

    private val parseFunctions = mapOf<Regex, (MatchResult) -> Unit>(
            VERTEX_PATTERN to { match ->
                val (vx, vy, vz, vw) = match.destructured
                vertices.add(Vertex(vx.toDouble(),
                        vy.toDouble(),
                        vz.toDouble(),
                        if (vw.isBlank()) 1.0 else vw.toDouble()))
            },
            TEXTURE_COORD_PATTERN to { match ->
                val (x, y, z) = match.destructured
                textureCoordinates.add(TextureCoordinate(x.toDouble(), y.toDouble(), z.toDouble()))
            },
            FACE_PATTERN to { match ->
                val (face1, face2, face3) = match.destructured
                val element1 = parseFaceElement(face1)
                val element2 = parseFaceElement(face2)
                val element3 = parseFaceElement(face3)
                faceIndices.add(FaceIndices(
                        element1.vertexIndex,
                        element1.textureCoordinateIndex,
                        element2.vertexIndex,
                        element2.textureCoordinateIndex,
                        element3.vertexIndex,
                element3.textureCoordinateIndex))
            }
    )

    private fun parseFaceElement(faceElement: String): FaceElement {
        val (vertexIndex, vertexTextureCoOrdIndex, vertexNormalIndex) = FACE_ELEMENT_PATTERN.find(faceElement)
                ?.destructured
                ?: error("Could not parse face element '${faceElement}'")

        return FaceElement(vertexIndex.toInt(), vertexTextureCoOrdIndex.toIntOrNull(), vertexNormalIndex.toIntOrNull())
    }

    private fun String.toIntOrNull() = try {
        this.toInt()
    } catch (_: NumberFormatException) {
        null
    }

    fun parse(sourceFile: Path): WavefrontObject {
        Files.lines(sourceFile).forEach { line ->
            parseFunctions.entries.forEach { (regex, action) ->
                regex.find(line)?.let { action(it) }
            }
        }

        return WavefrontObject(vertices,
                textureCoordinates,
                faceIndices.map { it.resolve(vertices, textureCoordinates) })
    }

    data class FaceIndices(val vertex1Index: Int,
                           val textureCoordinate1Index: Int?,
                           val vertex2Index: Int,
                           val textureCoordinate2Index: Int?,
                           val vertex3Index: Int,
                           val textureCoordinate3Index: Int?) {
        fun resolve(vertices: List<Vertex>, textureCoordinates: List<TextureCoordinate>): Face = Face(
                vertices[vertex1Index - 1],
                textureCoordinate1Index?.let { textureCoordinates[textureCoordinate1Index - 1] },
                vertices[vertex2Index - 1],
                textureCoordinate2Index?.let { textureCoordinates[textureCoordinate2Index - 1] },
                vertices[vertex3Index - 1],
                textureCoordinate3Index?.let { textureCoordinates[textureCoordinate3Index - 1] })
    }

    data class FaceElement(val vertexIndex: Int, val textureCoordinateIndex: Int?, val vertexNormalIndex: Int?)

    companion object {
        private val VERTEX_PATTERN = Regex("^v\\s+([eE\\d.-]+)\\s+([eE\\d.-]+)\\s+([eE\\d.-]+)\\s*([eE\\d.-]+)?\\s*$")
        private val TEXTURE_COORD_PATTERN = Regex("^vt\\s+([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)\\s*$")
        private val FACE_PATTERN = Regex("^f\\s+([\\d/]+)\\s+([\\d/]+)\\s+([\\d/]+)\\s*$")
        private val FACE_ELEMENT_PATTERN = Regex("^(\\d+)/?(\\d+)?/?(\\d+)?$")
    }
}

data class Vertex(val x: Double, val y: Double, val z: Double, val w: Double = 1.0)

data class TextureCoordinate(val x: Double, val y: Double, val z: Double)

data class Face(val vertex1: Vertex,
                val textureCoordinate1: TextureCoordinate?,
                val vertex2: Vertex,
                val textureCoordinate2: TextureCoordinate?,
                val vertex3: Vertex,
                val textureCoordinate3: TextureCoordinate?)