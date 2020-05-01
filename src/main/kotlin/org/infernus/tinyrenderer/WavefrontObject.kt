package org.infernus.tinyrenderer

import java.nio.file.Files
import java.nio.file.Path

data class WavefrontObject(val vertices: List<Vertex>,
                           val faces: List<Face>)

class WavefrontObjectParser {

    private val vertices = mutableListOf<Vertex>()
    private val faceIndices = mutableListOf<FaceIndices>()

    private val parseFunctions = mapOf<Regex, (MatchResult) -> Unit>(
            VERTEX_PATTERN to { match ->
                val (vx, vy, vz, vw) = match.destructured
                vertices.add(Vertex(vx.toDouble(),
                        vy.toDouble(),
                        vz.toDouble(),
                        if (vw.isBlank()) 1.0 else vw.toDouble()))
            },
            FACE_PATTERN to { match ->
                val (face1, face2, face3) = match.destructured
                faceIndices.add(FaceIndices(parseFaceElement(face1).vertexIndex,
                        parseFaceElement(face2).vertexIndex,
                        parseFaceElement(face3).vertexIndex))
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

        return WavefrontObject(vertices, faceIndices.map { it.resolve(vertices) })
    }

    data class FaceIndices(val vertex1Index: Int, val vertex2Index: Int, val vertex3Index: Int) {
        fun resolve(vertices: List<Vertex>): Face =
                Face(vertices[vertex1Index - 1], vertices[vertex2Index - 1], vertices[vertex3Index - 1])
    }

    data class FaceElement(val vertexIndex: Int, val vertexTextureCoOrdIndex: Int?, val vertexNormalIndex: Int?)

    companion object {
        private val VERTEX_PATTERN = Regex("^v\\s+([eE\\d.-]+)\\s+([eE\\d.-]+)\\s+([eE\\d.-]+)\\s*([eE\\d.-]+)?\\s*$")
        private val FACE_PATTERN = Regex("^f\\s+([\\d/]+)\\s+([\\d/]+)\\s+([\\d/]+)\\s*$")
        private val FACE_ELEMENT_PATTERN = Regex("^(\\d+)/?(\\d+)?/?(\\d+)?$")
    }
}

data class Vertex(val x: Double, val y: Double, val z: Double, val w: Double = 1.0) {
    fun toPoint() = Point3(x, y, z)

    fun toScreen(width: Int, height: Int) = toPoint().toScreen(width, height)
}

data class Face(val vertex1: Vertex, val vertex2: Vertex, val vertex3: Vertex) {
    fun lines(): List<Pair<Point3, Point3>> = listOf(
            vertex1.toPoint() to vertex2.toPoint(),
            vertex2.toPoint() to vertex3.toPoint(),
            vertex3.toPoint() to vertex1.toPoint())
}