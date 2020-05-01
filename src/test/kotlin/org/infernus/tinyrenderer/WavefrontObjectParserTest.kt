package org.infernus.tinyrenderer

import org.junit.Test
import java.nio.file.Path
import kotlin.test.assertEquals

class WavefrontObjectParserTest {

    @Test
    fun `it parses the correct numbers of vertices`() {
        val model = WavefrontObjectParser().parse(TEST_FILE)

        assertEquals(1258, model.vertices.size)
    }

    @Test
    fun `it parses a sample vertex correctly`() {
        val model = WavefrontObjectParser().parse(TEST_FILE)

        assertEquals(Vertex(0.610273, -0.786562, 0.0197893), model.vertices[16])
    }

    @Test
    fun `it parses the correct numbers of texture coordinates`() {
        val model = WavefrontObjectParser().parse(TEST_FILE)

        assertEquals(1339, model.textureCoordinates.size)
    }

    @Test
    fun `it parses a sample texture coordinate correctly`() {
        val model = WavefrontObjectParser().parse(TEST_FILE)

        assertEquals(TextureCoordinate(0.442, 0.967, 0.000), model.textureCoordinates[1336])
    }

    @Test
    fun `it parses the correct numbers of faces`() {
        val model = WavefrontObjectParser().parse(TEST_FILE)

        assertEquals(2492, model.faces.size)
    }

    @Test
    fun `it parses a sample face correctly`() {
        val model = WavefrontObjectParser().parse(TEST_FILE)

        val expectedFace = Face(
                Vertex(-0.142316, -0.166791, 0.41184),
                TextureCoordinate(0.478, 0.944, 0.000),
                Vertex(-0.126228, -0.116368, 0.360528),
                TextureCoordinate(0.480, 0.955, 0.000),
                Vertex(-0.150805, -0.116468, 0.431278),
                TextureCoordinate(0.470, 0.952, 0.000))
        assertEquals(expectedFace, model.faces[2490])
    }

    companion object {
        private val TEST_FILE = Path.of(Renderer::class.java.getResource("/african_head.obj").toURI())
    }
}