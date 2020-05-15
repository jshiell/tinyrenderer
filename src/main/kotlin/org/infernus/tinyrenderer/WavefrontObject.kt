package org.infernus.tinyrenderer

import java.nio.file.Files
import java.nio.file.Path

data class WavefrontObject(val vertices: List<Vertex>,
                           val textureCoordinates: List<TextureCoordinate>,
                           val normals: List<Normal>,
                           val faces: List<Face>)

class WavefrontObjectParser {

    private val vertices = mutableListOf<Vertex>()
    private val textureCoordinates = mutableListOf<TextureCoordinate>()
    private val normals = mutableListOf<Normal>()
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
            NORMAL_PATTERN to { match ->
                val (x, y, z) = match.destructured
                normals.add(Normal(x.toDouble(), y.toDouble(), z.toDouble()))
            },
            FACE_PATTERN to { match ->
                val (face1, face2, face3) = match.destructured
                faceIndices.add(FaceIndices(
                        VertexIndices.parse(face1),
                        VertexIndices.parse(face2),
                        VertexIndices.parse(face3)))
            }
    )

    fun parse(sourceFile: Path): WavefrontObject {
        Files.lines(sourceFile).forEach { line ->
            parseFunctions.entries.forEach { (regex, action) ->
                regex.find(line)?.let { action(it) }
            }
        }

        return WavefrontObject(vertices,
                textureCoordinates,
                normals,
                faceIndices.map { it.resolve(vertices, textureCoordinates, normals) })
    }

    data class VertexIndices(val vertexIndex: Int, val textureCoordinatesIndex: Int?, val normalIndex: Int?) {
        fun resolve(vertices: List<Vertex>,
                    textureCoordinates: List<TextureCoordinate>,
                    normals: List<Normal>) = VertexInformation(
                vertices[vertexIndex - 1],
                textureCoordinatesIndex?.let { textureCoordinates[textureCoordinatesIndex - 1] },
                normalIndex?.let { normals[normalIndex - 1] })

        companion object {
            internal fun parse(faceElement: String): VertexIndices {
                val (vertexIndex, vertexTextureCoOrdIndex, vertexNormalIndex) = FACE_ELEMENT_PATTERN.find(faceElement)
                        ?.destructured
                        ?: error("Could not parse face element '${faceElement}'")

                return VertexIndices(vertexIndex.toInt(), vertexTextureCoOrdIndex.toIntOrNull(), vertexNormalIndex.toIntOrNull())
            }
        }
    }

    data class FaceIndices(val vertex1: VertexIndices,
                           val vertex2: VertexIndices,
                           val vertex3: VertexIndices) {
        fun resolve(vertices: List<Vertex>,
                    textureCoordinates: List<TextureCoordinate>,
                    normals: List<Normal>): Face = Face(
                vertex1.resolve(vertices, textureCoordinates, normals),
                vertex2.resolve(vertices, textureCoordinates, normals),
                vertex3.resolve(vertices, textureCoordinates, normals))
    }

    companion object {
        private val VERTEX_PATTERN = Regex("^v\\s+([eE\\d.-]+)\\s+([eE\\d.-]+)\\s+([eE\\d.-]+)\\s*([eE\\d.-]+)?\\s*$")
        private val TEXTURE_COORD_PATTERN = Regex("^vt\\s+([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)\\s*$")
        private val FACE_PATTERN = Regex("^f\\s+([\\d/]+)\\s+([\\d/]+)\\s+([\\d/]+)\\s*$")
        private val FACE_ELEMENT_PATTERN = Regex("^(\\d+)/?(\\d+)?/?(\\d+)?$")
        private val NORMAL_PATTERN = Regex("^vn\\s+([\\d.-]+)\\s+([\\d.-]+)\\s+([\\d.-]+)\\s*$")
    }
}

data class Vertex(val x: Double, val y: Double, val z: Double, val w: Double = 1.0)

data class TextureCoordinate(val x: Double, val y: Double, val z: Double)

data class Normal(val x: Double, val y: Double, val z: Double)

data class VertexInformation(val vertex: Vertex, val textureCoordinate: TextureCoordinate?, val normal: Normal?)

data class Face(val first: VertexInformation, val second: VertexInformation, val third: VertexInformation)