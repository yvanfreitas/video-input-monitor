package com.videoinputmonitor.android

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.serenegiant.usb.Size
import com.serenegiant.widget.AspectRatioSurfaceView

class MainActivity : AppCompatActivity(), CaptureManager.Listener {

    private lateinit var videoView: AspectRatioSurfaceView
    private lateinit var status: TextView
    private lateinit var controls: View
    private lateinit var deviceSpinner: Spinner
    private lateinit var sizeSpinner: Spinner
    private lateinit var audioButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private val hideControls = Runnable { controls.visibility = View.GONE }
    private var surface: Surface? = null
    private var fullscreen = false
    private var updatingSpinners = false
    private var shownDevices: List<android.hardware.usb.UsbDevice> = emptyList()
    private var shownSizes: List<Size> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoView = findViewById(R.id.videoView)
        status = findViewById(R.id.status)
        controls = findViewById(R.id.controls)
        deviceSpinner = findViewById(R.id.deviceSpinner)
        sizeSpinner = findViewById(R.id.sizeSpinner)
        audioButton = findViewById(R.id.audioButton)

        videoView.setAspectRatio(16, 9)
        videoView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surface = holder.surface.also { CaptureManager.addSurface(it) }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surface?.let { CaptureManager.removeSurface(it) }
                surface = null
            }
        })

        findViewById<View>(R.id.root).setOnClickListener { showControls() }
        videoView.setOnClickListener { showControls() }
        findViewById<Button>(R.id.pipButton).setOnClickListener { enterPip() }
        findViewById<Button>(R.id.fullscreenButton).setOnClickListener { setFullscreen(!fullscreen) }
        audioButton.setOnClickListener {
            if (!AudioController.hasPermission()) requestPermissions() else AudioController.setEnabled(!AudioController.enabled)
            updateAudioButton()
        }

        deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (updatingSpinners) return
                shownDevices.getOrNull(position)?.let { CaptureManager.selectDevice(it) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        sizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (updatingSpinners) return
                val size = shownSizes.getOrNull(position) ?: return
                val current = CaptureManager.previewSize
                if (current == null || CaptureManager.sizeKey(current) != CaptureManager.sizeKey(size)) {
                    CaptureManager.setPreviewSize(size)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        requestPermissions()
        handleUsbIntent(intent)
        setFullscreen(true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUsbIntent(intent)
    }

    private fun handleUsbIntent(intent: Intent?) {
        if (intent?.action == android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            handler.postDelayed({ CaptureManager.selectFirstIfIdle() }, 500)
        }
    }

    override fun onStart() {
        super.onStart()
        CaptureManager.addListener(this)
        CaptureManager.selectFirstIfIdle()
        onCaptureStateChanged()
        if (AudioController.hasPermission()) CaptureService.start(this)
    }

    override fun onStop() {
        super.onStop()
        CaptureManager.removeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) CaptureService.stop(this)
    }

    private fun requestPermissions() {
        val wanted = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= 33) wanted += Manifest.permission.POST_NOTIFICATIONS
        val missing = wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (AudioController.hasPermission()) {
            CaptureService.start(this)
            AudioController.refresh()
        }
        updateAudioButton()
    }

    override fun onCaptureStateChanged() {
        val size = CaptureManager.previewSize
        if (size != null && size.width > 0 && size.height > 0) {
            videoView.setAspectRatio(size.width, size.height)
            updatePipParams()
        }
        status.text = CaptureManager.statusText
        status.visibility = if (CaptureManager.isOpen) View.GONE else View.VISIBLE
        updateSpinners()
        updateAudioButton()
    }

    private fun updateSpinners() {
        updatingSpinners = true
        shownDevices = CaptureManager.devices()
        val deviceLabels = shownDevices.map { CaptureManager.deviceLabel(it) }
            .ifEmpty { listOf(getString(R.string.no_device)) }
        deviceSpinner.adapter = adapter(deviceLabels)
        val devIndex = shownDevices.indexOf(CaptureManager.currentDevice)
        if (devIndex >= 0) deviceSpinner.setSelection(devIndex, false)

        shownSizes = CaptureManager.supportedSizes()
        sizeSpinner.visibility = if (shownSizes.isEmpty()) View.GONE else View.VISIBLE
        sizeSpinner.adapter = adapter(shownSizes.map { CaptureManager.sizeLabel(it) })
        val current = CaptureManager.previewSize
        if (current != null) {
            val idx = shownSizes.indexOfFirst {
                CaptureManager.sizeKey(it) == CaptureManager.sizeKey(current)
            }
            if (idx >= 0) sizeSpinner.setSelection(idx, false)
        }
        // Os callbacks de seleção do Spinner chegam depois do layout.
        handler.post { updatingSpinners = false }
    }

    private fun adapter(items: List<String>) =
        ArrayAdapter(this, R.layout.spinner_item, items).apply {
            setDropDownViewResource(R.layout.spinner_item)
        }

    private fun updateAudioButton() {
        audioButton.setText(
            if (AudioController.enabled && AudioController.hasPermission()) R.string.audio_on else R.string.audio_off
        )
    }

    private fun showControls() {
        if (isInPip()) return
        controls.visibility = View.VISIBLE
        handler.removeCallbacks(hideControls)
        handler.postDelayed(hideControls, 5000)
    }

    private fun setFullscreen(enabled: Boolean) {
        fullscreen = enabled
        WindowCompat.setDecorFitsSystemWindows(window, !enabled)
        val insets = WindowInsetsControllerCompat(window, window.decorView)
        if (enabled) {
            insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insets.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insets.show(WindowInsetsCompat.Type.systemBars())
        }
        showControls()
    }

    private fun pipParams(): PictureInPictureParams {
        val size = CaptureManager.previewSize
        var w = size?.width ?: 16
        var h = size?.height ?: 9
        if (w <= 0 || h <= 0) { w = 16; h = 9 }
        // O Android aceita proporções entre 1:2.39 e 2.39:1 no PiP.
        val ratio = (w.toDouble() / h).coerceIn(1 / 2.39, 2.39)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(CaptureManager.isOpen)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun updatePipParams() {
        try {
            setPictureInPictureParams(pipParams())
        } catch (_: Exception) {
        }
    }

    private fun enterPip() {
        try {
            enterPictureInPictureMode(pipParams())
        } catch (_: Exception) {
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Em Android 12+ a entrada automática é feita pelo sistema.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && CaptureManager.isOpen) enterPip()
    }

    private fun isInPip() = isInPictureInPictureMode

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            handler.removeCallbacks(hideControls)
            controls.visibility = View.GONE
        } else {
            showControls()
        }
    }
}
