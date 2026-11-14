package com.videoinputmonitor.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Repassa o áudio da placa de captura (entrada USB Audio) para a saída de
 * mídia do celular. Com o Android Auto conectado, a saída de mídia vai para
 * os alto-falantes do carro.
 */
object AudioController {

    private const val TAG = "AudioController"
    private const val PREFS = "audio"
    private const val PREF_ENABLED = "enabled"
    private const val SAMPLE_RATE = 48000

    private lateinit var appContext: Context
    private lateinit var audioManager: AudioManager
    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var running = false
    private var worker: Thread? = null
    private var activeDeviceId = -1

    var enabled = true
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        enabled = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(PREF_ENABLED, true)
        audioManager.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
        }, main)
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(PREF_ENABLED, value).apply()
        refresh()
    }

    fun hasPermission(): Boolean = ::appContext.isInitialized &&
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    /** Liga ou desliga o repasse de acordo com o estado atual. */
    fun refresh() {
        if (!::appContext.isInitialized) return
        val input = if (enabled && CaptureManager.isOpen && hasPermission()) findUsbInput() else null
        if (input == null) {
            stop()
            return
        }
        if (running && input.id == activeDeviceId) return
        stop()
        start(input)
    }

    private fun findUsbInput(): AudioDeviceInfo? =
        audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }

    private fun start(input: AudioDeviceInfo) {
        running = true
        activeDeviceId = input.id
        worker = Thread({ loop(input) }, "audio-passthrough").also { it.start() }
    }

    private fun stop() {
        running = false
        activeDeviceId = -1
        worker?.let {
            it.interrupt()
            try {
                it.join(500)
            } catch (_: InterruptedException) {
            }
        }
        worker = null
    }

    @Suppress("MissingPermission")
    private fun buildRecord(input: AudioDeviceInfo, channelMask: Int): AudioRecord? {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return null
        return try {
            val record = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(channelMask)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .build()
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                null
            } else {
                record.preferredDevice = input
                record
            }
        } catch (e: Exception) {
            Log.w(TAG, "AudioRecord falhou (mask=$channelMask)", e)
            null
        }
    }

    private fun loop(input: AudioDeviceInfo) {
        var stereo = true
        val record = buildRecord(input, AudioFormat.CHANNEL_IN_STEREO)
            ?: buildRecord(input, AudioFormat.CHANNEL_IN_MONO).also { stereo = false }
            ?: run {
                running = false
                return
            }
        val outMask = if (stereo) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val outMin = AudioTrack.getMinBufferSize(SAMPLE_RATE, outMask, AudioFormat.ENCODING_PCM_16BIT)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(outMask)
                        .build()
                )
                .setBufferSizeInBytes(outMin * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "AudioTrack falhou", e)
            record.release()
            running = false
            return
        }

        val buffer = ShortArray(maxOf(outMin / 2, 960))
        try {
            record.startRecording()
            track.play()
            while (running && !Thread.currentThread().isInterrupted) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) track.write(buffer, 0, read)
                else if (read < 0) break
            }
        } catch (e: Exception) {
            Log.w(TAG, "repasse de áudio interrompido", e)
        } finally {
            try { record.stop() } catch (_: Exception) {}
            try { track.stop() } catch (_: Exception) {}
            record.release()
            track.release()
        }
    }
}
