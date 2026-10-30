package com.example.rooftophunt.engine3d

import android.opengl.Matrix
import java.util.ArrayDeque

class MatrixStack {
    private val stack = ArrayDeque<FloatArray>()
    var current = FloatArray(16)
        private set

    init {
        Matrix.setIdentityM(current, 0)
    }

    fun push() {
        val copy = FloatArray(16)
        System.arraycopy(current, 0, copy, 0, 16)
        stack.push(copy)
    }

    fun pop() {
        if (stack.isNotEmpty()) {
            current = stack.pop()
        }
    }

    fun identity() {
        Matrix.setIdentityM(current, 0)
    }

    fun translate(x: Float, y: Float, z: Float) {
        Matrix.translateM(current, 0, x, y, z)
    }

    fun rotate(angleDegrees: Float, x: Float, y: Float, z: Float) {
        Matrix.rotateM(current, 0, angleDegrees, x, y, z)
    }

    fun scale(sx: Float, sy: Float, sz: Float) {
        Matrix.scaleM(current, 0, sx, sy, sz)
    }

    fun multiply(other: FloatArray) {
        val result = FloatArray(16)
        Matrix.multiplyMM(result, 0, current, 0, other, 0)
        current = result
    }
}
