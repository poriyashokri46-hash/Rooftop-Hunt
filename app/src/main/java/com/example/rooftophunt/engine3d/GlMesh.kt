package com.example.rooftophunt.engine3d

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

class GlMesh(
    vertices: FloatArray,
    normals: FloatArray
) {
    val vertexCount = vertices.size / 3
    private val vboIds = IntArray(2)

    init {
        GLES30.glGenBuffers(2, vboIds, 0)

        // Vertex buffer
        val vbb = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder())
        val vb = vbb.asFloatBuffer().apply {
            put(vertices)
            position(0)
        }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIds[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, vb, GLES30.GL_STATIC_DRAW)

        // Normal buffer
        val nbb = ByteBuffer.allocateDirect(normals.size * 4).order(ByteOrder.nativeOrder())
        val nb = nbb.asFloatBuffer().apply {
            put(normals)
            position(0)
        }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIds[1])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, normals.size * 4, nb, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun draw(posLoc: Int, normLoc: Int) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIds[0])
        GLES30.glEnableVertexAttribArray(posLoc)
        GLES30.glVertexAttribPointer(posLoc, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIds[1])
        GLES30.glEnableVertexAttribArray(normLoc)
        GLES30.glVertexAttribPointer(normLoc, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)

        GLES30.glDisableVertexAttribArray(posLoc)
        GLES30.glDisableVertexAttribArray(normLoc)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    companion object {
        fun createBox(hx: Float = 0.5f, hy: Float = 0.5f, hz: Float = 0.5f): GlMesh {
            val v = floatArrayOf(
                // Front (+Z)
                -hx, -hy,  hz,   hx, -hy,  hz,   hx,  hy,  hz,
                -hx, -hy,  hz,   hx,  hy,  hz,  -hx,  hy,  hz,
                // Back (-Z)
                 hx, -hy, -hz,  -hx, -hy, -hz,  -hx,  hy, -hz,
                 hx, -hy, -hz,  -hx,  hy, -hz,   hx,  hy, -hz,
                // Top (+Y)
                -hx,  hy,  hz,   hx,  hy,  hz,   hx,  hy, -hz,
                -hx,  hy,  hz,   hx,  hy, -hz,  -hx,  hy, -hz,
                // Bottom (-Y)
                -hx, -hy, -hz,   hx, -hy, -hz,   hx, -hy,  hz,
                -hx, -hy, -hz,   hx, -hy,  hz,  -hx, -hy,  hz,
                // Right (+X)
                 hx, -hy,  hz,   hx, -hy, -hz,   hx,  hy, -hz,
                 hx, -hy,  hz,   hx,  hy, -hz,   hx,  hy,  hz,
                // Left (-X)
                -hx, -hy, -hz,  -hx, -hy,  hz,  -hx,  hy,  hz,
                -hx, -hy, -hz,  -hx,  hy,  hz,  -hx,  hy, -hz
            )

            val n = floatArrayOf(
                // Front
                0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,
                0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,
                // Back
                0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f,
                0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f,
                // Top
                0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,
                0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,
                // Bottom
                0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f,
                0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f,
                // Right
                1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,
                1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,
                // Left
                -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f,
                -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f
            )

            return GlMesh(v, n)
        }

        fun createCylinder(radius: Float = 0.5f, height: Float = 1.0f, segments: Int = 12): GlMesh {
            val vList = mutableListOf<Float>()
            val nList = mutableListOf<Float>()
            val halfH = height * 0.5f

            for (i in 0 until segments) {
                val a1 = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
                val a2 = ((i + 1).toFloat() / segments) * 2f * Math.PI.toFloat()

                val x1 = cos(a1) * radius
                val z1 = sin(a1) * radius
                val x2 = cos(a2) * radius
                val z2 = sin(a2) * radius

                val nx1 = cos(a1)
                val nz1 = sin(a1)
                val nx2 = cos(a2)
                val nz2 = sin(a2)

                // Side quad (2 triangles)
                // Tri 1
                vList.addAll(listOf(x1, -halfH, z1,  x2, -halfH, z2,  x2, halfH, z2))
                nList.addAll(listOf(nx1, 0f, nz1,    nx2, 0f, nz2,    nx2, 0f, nz2))
                // Tri 2
                vList.addAll(listOf(x1, -halfH, z1,  x2, halfH, z2,   x1, halfH, z1))
                nList.addAll(listOf(nx1, 0f, nz1,    nx2, 0f, nz2,    nx1, 0f, nz1))

                // Top cap
                vList.addAll(listOf(0f, halfH, 0f,   x2, halfH, z2,   x1, halfH, z1))
                nList.addAll(listOf(0f, 1f, 0f,      0f, 1f, 0f,      0f, 1f, 0f))

                // Bottom cap
                vList.addAll(listOf(0f, -halfH, 0f,  x1, -halfH, z1,  x2, -halfH, z2))
                nList.addAll(listOf(0f, -1f, 0f,     0f, -1f, 0f,     0f, -1f, 0f))
            }

            return GlMesh(vList.toFloatArray(), nList.toFloatArray())
        }

        fun createDiamond(radius: Float = 0.4f, height: Float = 0.8f): GlMesh {
            // Octahedron crystal for powerups
            val vList = mutableListOf<Float>()
            val nList = mutableListOf<Float>()
            val halfH = height * 0.5f

            val pts = arrayOf(
                floatArrayOf(radius, 0f, 0f),
                floatArrayOf(0f, 0f, radius),
                floatArrayOf(-radius, 0f, 0f),
                floatArrayOf(0f, 0f, -radius)
            )

            for (i in 0 until 4) {
                val p1 = pts[i]
                val p2 = pts[(i + 1) % 4]

                // Upper pyramid face
                vList.addAll(listOf(0f, halfH, 0f, p1[0], p1[1], p1[2], p2[0], p2[1], p2[2]))
                val nx = (p1[0] + p2[0]) * 0.5f
                val nz = (p1[2] + p2[2]) * 0.5f
                nList.addAll(listOf(nx, 0.7f, nz, nx, 0.7f, nz, nx, 0.7f, nz))

                // Lower pyramid face
                vList.addAll(listOf(0f, -halfH, 0f, p2[0], p2[1], p2[2], p1[0], p1[1], p1[2]))
                nList.addAll(listOf(nx, -0.7f, nz, nx, -0.7f, nz, nx, -0.7f, nz))
            }

            return GlMesh(vList.toFloatArray(), nList.toFloatArray())
        }

        fun createCoin(radius: Float = 0.35f, thickness: Float = 0.08f): GlMesh {
            return createCylinder(radius = radius, height = thickness, segments = 10)
        }

        fun createSphereRough(radius: Float = 0.5f): GlMesh {
            // Geodesic / faceted sphere for shield or orbs
            return createCylinder(radius = radius, height = radius * 1.5f, segments = 8)
        }
    }
}
