package com.example.ui.components

import android.content.Context
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.GlRectDrawer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink

class WebRtcTextureView(
    context: Context,
    private val cornerRadiusDp: Float = 20f
) : TextureView(context), TextureView.SurfaceTextureListener, VideoSink {

    private val eglRenderer = EglRenderer("WebRtcTextureView")
    private var isInitialized = false

    private var lastFrameWidth = 0
    private var lastFrameHeight = 0
    private var lastFrameRotation = 0

    init {
        surfaceTextureListener = this
        val radiusPx = cornerRadiusDp * context.resources.displayMetrics.density
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
            }
        }
        clipToOutline = true
    }

    fun init(sharedContext: EglBase.Context?) {
        if (isInitialized) return
        eglRenderer.init(sharedContext, EglBase.CONFIG_PLAIN, GlRectDrawer())
        isInitialized = true
    }

    fun setMirror(mirror: Boolean) {
        eglRenderer.setMirror(mirror)
    }

    override fun onFrame(frame: VideoFrame) {
        val width = if (frame.rotation % 180 == 0) frame.buffer.width else frame.buffer.height
        val height = if (frame.rotation % 180 == 0) frame.buffer.height else frame.buffer.width

        if (width != lastFrameWidth || height != lastFrameHeight || frame.rotation != lastFrameRotation) {
            lastFrameWidth = width
            lastFrameHeight = height
            lastFrameRotation = frame.rotation
            post {
                updateTextureTransformMatrix(width, height)
            }
        }

        eglRenderer.onFrame(frame)
    }

    private fun updateTextureTransformMatrix(frameWidth: Int, frameHeight: Int) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if (viewWidth <= 0f || viewHeight <= 0f || frameWidth <= 0 || frameHeight <= 0) return

        val videoAspect = frameWidth.toFloat() / frameHeight.toFloat()
        val viewAspect = viewWidth / viewHeight

        val scaleX: Float
        val scaleY: Float

        if (videoAspect > viewAspect) {
            // Video is wider than view -> scale X to fill/crop horizontally
            scaleX = videoAspect / viewAspect
            scaleY = 1.0f
        } else {
            // Video is taller than view -> scale Y to fill/crop vertically
            scaleX = 1.0f
            scaleY = viewAspect / videoAspect
        }

        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
        setTransform(matrix)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        eglRenderer.createEglSurface(Surface(surface))
        if (lastFrameWidth > 0 && lastFrameHeight > 0) {
            updateTextureTransformMatrix(lastFrameWidth, lastFrameHeight)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        if (lastFrameWidth > 0 && lastFrameHeight > 0) {
            updateTextureTransformMatrix(lastFrameWidth, lastFrameHeight)
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        eglRenderer.releaseEglSurface { }
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    fun release() {
        if (isInitialized) {
            eglRenderer.release()
            isInitialized = false
        }
    }
}
