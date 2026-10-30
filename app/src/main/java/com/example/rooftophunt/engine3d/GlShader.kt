package com.example.rooftophunt.engine3d

import android.opengl.GLES30
import android.util.Log

class GlShader {
    var programId: Int = 0
        private set

    var aPositionLocation: Int = -1
    var aNormalLocation: Int = -1
    var uMVPMatrixLocation: Int = -1
    var uMMatrixLocation: Int = -1
    var uColorLocation: Int = -1
    var uLightDirLocation: Int = -1
    var uLightColorLocation: Int = -1
    var uCameraPosLocation: Int = -1
    var uFogColorLocation: Int = -1
    var uFogDensityLocation: Int = -1
    var uEmissiveLocation: Int = -1

    fun init() {
        val vertexShaderCode = """#version 300 es
            layout(location = 0) in vec3 a_Position;
            layout(location = 1) in vec3 a_Normal;

            uniform mat4 u_MVPMatrix;
            uniform mat4 u_MMatrix;

            out vec3 v_FragPos;
            out vec3 v_Normal;

            void main() {
                v_FragPos = vec3(u_MMatrix * vec4(a_Position, 1.0));
                // Normal matrix approximation (uniform scale)
                v_Normal = mat3(u_MMatrix) * a_Normal;
                gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
            }
        """.trimIndent()

        val fragmentShaderCode = """#version 300 es
            precision mediump float;

            in vec3 v_FragPos;
            in vec3 v_Normal;

            uniform vec4 u_Color;
            uniform vec3 u_LightDir;
            uniform vec3 u_LightColor;
            uniform vec3 u_CameraPos;
            uniform vec3 u_FogColor;
            uniform float u_FogDensity;
            uniform float u_Emissive;

            out vec4 fragColor;

            void main() {
                vec3 norm = normalize(v_Normal);
                vec3 lightDir = normalize(u_LightDir);

                // Ambient light
                float ambientStrength = 0.38;
                vec3 ambient = ambientStrength * vec3(1.0);

                // Diffuse light
                float diff = max(dot(norm, lightDir), 0.0);
                vec3 diffuse = diff * u_LightColor;

                // Specular highlight
                vec3 viewDir = normalize(u_CameraPos - v_FragPos);
                vec3 reflectDir = reflect(-lightDir, norm);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
                vec3 specular = spec * 0.25 * u_LightColor;

                // Base lit color
                vec3 litColor = (ambient + diffuse + specular) * u_Color.rgb;
                // Boost with emissive
                litColor += u_Color.rgb * u_Emissive * 0.8;

                // Fog calculation
                float dist = length(u_CameraPos - v_FragPos);
                float fogFactor = exp(-dist * u_FogDensity);
                fogFactor = clamp(fogFactor, 0.0, 1.0);

                vec3 finalColor = mix(u_FogColor, litColor, fogFactor);
                fragColor = vec4(finalColor, u_Color.a);
            }
        """.trimIndent()

        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)
        GLES30.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(programId)
            GLES30.glDeleteProgram(programId)
            throw RuntimeException("Error linking program: $log")
        }

        aPositionLocation = GLES30.glGetAttribLocation(programId, "a_Position")
        aNormalLocation = GLES30.glGetAttribLocation(programId, "a_Normal")
        uMVPMatrixLocation = GLES30.glGetUniformLocation(programId, "u_MVPMatrix")
        uMMatrixLocation = GLES30.glGetUniformLocation(programId, "u_MMatrix")
        uColorLocation = GLES30.glGetUniformLocation(programId, "u_Color")
        uLightDirLocation = GLES30.glGetUniformLocation(programId, "u_LightDir")
        uLightColorLocation = GLES30.glGetUniformLocation(programId, "u_LightColor")
        uCameraPosLocation = GLES30.glGetUniformLocation(programId, "u_CameraPos")
        uFogColorLocation = GLES30.glGetUniformLocation(programId, "u_FogColor")
        uFogDensityLocation = GLES30.glGetUniformLocation(programId, "u_FogDensity")
        uEmissiveLocation = GLES30.glGetUniformLocation(programId, "u_Emissive")
    }

    private fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Error compiling shader ($type): $log")
        }
        return shader
    }
}
