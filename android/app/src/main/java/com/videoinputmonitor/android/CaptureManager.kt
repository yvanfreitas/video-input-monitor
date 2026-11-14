package com.videoinputmonitor.android

import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.herohan.uvcapp.CameraException
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import com.serenegiant.usb.Size
import com.serenegiant.usb.UVCCamera

/**
 * Ponto único de acesso à placa de captura (UVC).
 *
 * A tela do celular e a tela do Android Auto registram suas superfícies aqui;
 * o vídeo é desenhado em todas ao mesmo tempo. Todos os métodos devem ser
 * chamados na thread principal.
 */
object CaptureManager {

    fun interface Listener {
        fun onCaptureStateChanged()
    }

    private const val TAG = "CaptureManager"
    private const val PREFS = "capture"
    private const val PREF_SIZE = "preferred_size"

    private val main = Handler(Looper.getMainLooper())
    private lateinit var appContext: Context
    private var helper: ICameraHelper? = null
    private val surfaces = LinkedHashSet<Any>()
    private val listeners = LinkedHashSet<Listener>()

    var currentDevice: UsbDevice? = null
        private set
    var isOpen = false
        private set
    var previewSize: Size? = null
        private set
    var statusText: String = "Conecte a placa de captura (USB-OTG)."
        private set

    fun start(context: Context) {
        if (helper != null) return
        appContext = context.applicationContext
        helper = CameraHelper().apply { setStateCallback(stateCallback) }
        // Placas já conectadas antes do app abrir.
        main.postDelayed({ selectFirstIfIdle() }, 500)
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun addSurface(surface: Any) {
        if (!surfaces.add(surface)) return
        if (isOpen) safely { helper?.addSurface(surface, false) }
    }

    fun removeSurface(surface: Any) {
        if (!surfaces.remove(surface)) return
        if (isOpen) safely { helper?.removeSurface(surface) }
    }

    fun devices(): List<UsbDevice> = safely { helper?.deviceList } ?: emptyList()

    fun supportedSizes(): List<Size> {
        if (!isOpen) return emptyList()
        return safely { helper?.supportedSizeList } ?: emptyList()
    }

    fun selectDevice(device: UsbDevice) {
        if (device == currentDevice && isOpen) return
        val h = helper ?: return
        if (isOpen) closeCurrent()
        currentDevice = device
        statusText = "Aguardando permissão USB…"
        notifyChanged()
        safely { h.selectDevice(device) }
    }

    fun setPreviewSize(size: Size) {
        val h = helper ?: return
        if (!isOpen) return
        prefs().edit().putString(PREF_SIZE, sizeKey(size)).apply()
        safely {
            h.stopPreview()
            h.setPreviewSize(size)
            h.startPreview()
        }
        previewSize = safely { h.previewSize } ?: size
        notifyChanged()
    }

    /** Re-tenta abrir a primeira placa encontrada, se nenhuma estiver em uso. */
    fun selectFirstIfIdle() {
        if (currentDevice != null) return
        devices().firstOrNull()?.let { selectDevice(it) }
    }

    private fun closeCurrent() {
        val h = helper ?: return
        surfaces.forEach { s -> safely { h.removeSurface(s) } }
        safely { h.closeCamera() }
        isOpen = false
        previewSize = null
    }

    private val stateCallback = object : ICameraHelper.StateCallback {
        override fun onAttach(device: UsbDevice) {
            Log.i(TAG, "attach ${device.deviceName}")
            if (currentDevice == null) selectDevice(device) else notifyChanged()
        }

        override fun onDeviceOpen(device: UsbDevice, isFirstOpen: Boolean) {
            statusText = "Abrindo a placa…"
            notifyChanged()
            safely { helper?.openCamera() }
        }

        override fun onCameraOpen(device: UsbDevice) {
            val h = helper ?: return
            isOpen = true
            statusText = ""
            safely { h.startPreview() }
            applyPreferredSize(h)
            previewSize = safely { h.previewSize }
            surfaces.forEach { s -> safely { h.addSurface(s, false) } }
            notifyChanged()
        }

        override fun onCameraClose(device: UsbDevice) {
            val h = helper
            if (h != null) surfaces.forEach { s -> safely { h.removeSurface(s) } }
            isOpen = false
            previewSize = null
            notifyChanged()
        }

        override fun onDeviceClose(device: UsbDevice) {}

        override fun onDetach(device: UsbDevice) {
            Log.i(TAG, "detach ${device.deviceName}")
            if (device == currentDevice) {
                isOpen = false
                previewSize = null
                currentDevice = null
                statusText = "Placa desconectada. Conecte a placa de captura (USB-OTG)."
                main.postDelayed({ selectFirstIfIdle() }, 300)
            }
            notifyChanged()
        }

        override fun onCancel(device: UsbDevice) {
            if (device == currentDevice) currentDevice = null
            statusText = "Permissão USB negada. Reconecte a placa e toque em \"Permitir\"."
            notifyChanged()
        }

        override fun onError(device: UsbDevice, e: CameraException) {
            Log.e(TAG, "erro na câmera", e)
            statusText = "Erro ao abrir a placa: $e"
            notifyChanged()
        }
    }

    /**
     * Placas HDMI costumam abrir em 640x480. Usa a resolução escolhida antes
     * pelo usuário ou, na falta dela, a melhor opção MJPEG (YUY2 em 1080p
     * normalmente não cabe na banda do USB 2.0).
     */
    private fun applyPreferredSize(h: ICameraHelper) {
        val sizes = safely { h.supportedSizeList } ?: return
        if (sizes.isEmpty()) return
        val saved = prefs().getString(PREF_SIZE, null)
        val target = sizes.firstOrNull { sizeKey(it) == saved } ?: pickDefaultSize(sizes) ?: return
        val current = safely { h.previewSize }
        if (current != null && sizeKey(current) == sizeKey(target)) return
        safely {
            h.stopPreview()
            h.setPreviewSize(target)
            h.startPreview()
        }
    }

    private fun pickDefaultSize(sizes: List<Size>): Size? {
        val mjpeg = sizes.filter { it.type == UVCCamera.UVC_VS_FRAME_MJPEG }
        val pool = mjpeg.ifEmpty { sizes }
        return pool.firstOrNull { it.width == 1920 && it.height == 1080 }
            ?: pool.firstOrNull { it.width == 1280 && it.height == 720 }
            ?: pool.maxByOrNull { it.width * it.height }
    }

    fun sizeLabel(size: Size): String {
        val fmt = if (size.type == UVCCamera.UVC_VS_FRAME_MJPEG) "MJPEG" else "YUY2"
        val fps = if (size.fps > 0) " ${size.fps}fps" else ""
        return "${size.width}x${size.height} $fmt$fps"
    }

    fun sizeKey(size: Size) = "${size.type}:${size.width}x${size.height}"

    fun deviceLabel(device: UsbDevice): String =
        device.productName?.takeIf { it.isNotBlank() } ?: device.deviceName

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun notifyChanged() {
        AudioController.refresh()
        listeners.toList().forEach { it.onCaptureStateChanged() }
    }

    private inline fun <T> safely(block: () -> T): T? = try {
        block()
    } catch (e: Exception) {
        Log.w(TAG, "falha na chamada UVC", e)
        null
    }
}
