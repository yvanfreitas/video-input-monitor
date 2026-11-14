package com.videoinputmonitor.android.car

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import com.videoinputmonitor.android.CaptureManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Recebe o vídeo da placa numa SurfaceTexture própria e o desenha na
 * superfície do Android Auto mantendo a proporção (com tarjas pretas),
 * dentro da área visível que o carro informa.
 */
class CarSurfaceRenderer(
    private val output: Surface,
    private val width: Int,
    private val height: Int,
) : SurfaceTexture.OnFrameAvailableListener {

    private val thread = HandlerThread("car-video").apply { start() }
    private val gl = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())

    @Volatile
    var blocked = true
        set(value) {
            field = value
            gl.post { draw(false) }
        }

    @Volatile
    private var visible = Rect(0, 0, width, height)

    @Volatile
    private var videoActive = false

    @Volatile
    private var videoWidth = 16

    @Volatile
    private var videoHeight = 9

    private var released = false // thread principal
    private var registeredInput: Surface? = null // thread principal

    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var texture = 0
    private var program = 0
    private var aPosition = 0
    private var aTexCoord = 0
    private var uTexMatrix = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var input: Surface? = null
    private var hasFrame = false
    private val texMatrix = FloatArray(16)

    private val quad: FloatBuffer = floatBuffer(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
    private val texCoords: FloatBuffer = floatBuffer(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)

    fun start() {
        gl.post {
            if (!initGl()) return@post
            val newInput = input ?: return@post
            draw(false)
            main.post {
                if (released) return@post
                registeredInput = newInput
                CaptureManager.addSurface(newInput)
            }
        }
    }

    fun setVisibleArea(rect: Rect) {
        visible = Rect(rect)
        gl.post { draw(false) }
    }

    fun setVideo(active: Boolean, w: Int, h: Int) {
        videoActive = active
        if (w > 0 && h > 0) {
            videoWidth = w
            videoHeight = h
        }
        gl.post {
            if (!active) hasFrame = false
            if (w > 0 && h > 0) surfaceTexture?.setDefaultBufferSize(w, h)
            draw(false)
        }
    }

    /** Deve ser chamado na thread principal. */
    fun release() {
        if (released) return
        released = true
        registeredInput?.let { CaptureManager.removeSurface(it) }
        registeredInput = null
        gl.post {
            teardownGl()
            thread.quitSafely()
        }
    }

    override fun onFrameAvailable(st: SurfaceTexture) {
        draw(true)
    }

    private fun initGl(): Boolean {
        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize" }
            val attribs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            check(EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, count, 0) && count[0] > 0) {
                "eglChooseConfig"
            }
            val config = configs[0]!!
            context = EGL14.eglCreateContext(
                display, config, EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
            )
            check(context != EGL14.EGL_NO_CONTEXT) { "eglCreateContext" }
            eglSurface = EGL14.eglCreateWindowSurface(display, config, output, intArrayOf(EGL14.EGL_NONE), 0)
            check(eglSurface != EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface" }
            check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) { "eglMakeCurrent" }

            program = buildProgram()
            aPosition = GLES20.glGetAttribLocation(program, "aPosition")
            aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
            uTexMatrix = GLES20.glGetUniformLocation(program, "uTexMatrix")

            val tex = IntArray(1)
            GLES20.glGenTextures(1, tex, 0)
            texture = tex[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            val st = SurfaceTexture(texture)
            st.setDefaultBufferSize(videoWidth, videoHeight)
            st.setOnFrameAvailableListener(this, gl)
            surfaceTexture = st
            input = Surface(st)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "falha ao iniciar OpenGL na tela do carro", e)
            teardownGl()
            return false
        }
    }

    private fun draw(newFrame: Boolean) {
        if (eglSurface == EGL14.EGL_NO_SURFACE) return
        if (!EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) return
        val st = surfaceTexture
        if (newFrame && st != null) {
            try {
                st.updateTexImage()
                st.getTransformMatrix(texMatrix)
                hasFrame = true
            } catch (e: Exception) {
                Log.w(TAG, "updateTexImage", e)
            }
        }

        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (!blocked && videoActive && hasFrame) {
            val area = visible.takeIf { it.width() > 0 && it.height() > 0 } ?: Rect(0, 0, width, height)
            val videoRatio = videoWidth.toFloat() / videoHeight
            var w = area.width()
            var h = (w / videoRatio).toInt()
            if (h > area.height()) {
                h = area.height()
                w = (h * videoRatio).toInt()
            }
            val x = area.left + (area.width() - w) / 2
            val top = area.top + (area.height() - h) / 2
            // O OpenGL conta o eixo Y de baixo para cima.
            GLES20.glViewport(x, height - (top + h), w, h)

            GLES20.glUseProgram(program)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
            GLES20.glUniformMatrix4fv(uTexMatrix, 1, false, texMatrix, 0)
            GLES20.glEnableVertexAttribArray(aPosition)
            GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, quad)
            GLES20.glEnableVertexAttribArray(aTexCoord)
            GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, texCoords)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(aPosition)
            GLES20.glDisableVertexAttribArray(aTexCoord)
        }
        EGL14.eglSwapBuffers(display, eglSurface)
    }

    private fun teardownGl() {
        input?.release()
        input = null
        surfaceTexture?.release()
        surfaceTexture = null
        if (display != EGL14.EGL_NO_DISPLAY) {
            if (context != EGL14.EGL_NO_CONTEXT && eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)
                if (texture != 0) GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
                if (program != 0) GLES20.glDeleteProgram(program)
            }
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
            if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
            // Não chamamos eglTerminate: o display é compartilhado com a biblioteca UVC.
            EGL14.eglReleaseThread()
        }
        texture = 0
        program = 0
        eglSurface = EGL14.EGL_NO_SURFACE
        context = EGL14.EGL_NO_CONTEXT
        display = EGL14.EGL_NO_DISPLAY
    }

    private fun buildProgram(): Int {
        val vs = compile(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)
        val status = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) { "link: " + GLES20.glGetProgramInfoLog(p) }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return p
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) { "shader: " + GLES20.glGetShaderInfoLog(shader) }
        return shader
    }

    companion object {
        private const val TAG = "CarSurfaceRenderer"

        private val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            uniform mat4 uTexMatrix;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = (uTexMatrix * aTexCoord).xy;
            }
        """.trimIndent()

        private val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, vTexCoord);
            }
        """.trimIndent()

        private fun floatBuffer(vararg values: Float): FloatBuffer =
            ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(values)
                position(0)
            }
    }
}
