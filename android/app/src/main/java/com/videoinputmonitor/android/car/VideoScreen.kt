package com.videoinputmonitor.android.car

import android.graphics.Rect
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.videoinputmonitor.android.AudioController
import com.videoinputmonitor.android.CaptureManager

/**
 * Tela do Android Auto. O vídeo da placa é desenhado na superfície do modelo
 * de navegação, e só aparece com o carro parado; em movimento a tela do carro
 * fica preta com um aviso, e o vídeo continua na tela do celular para os
 * passageiros.
 */
class VideoScreen(carContext: CarContext) : Screen(carContext), CaptureManager.Listener {

    private var renderer: CarSurfaceRenderer? = null
    private var visibleArea: Rect? = null

    private val motion = MotionMonitor(carContext) {
        renderer?.blocked = !it.parked
        invalidate()
    }

    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            renderer?.release()
            val surface = surfaceContainer.surface ?: return
            renderer = CarSurfaceRenderer(surface, surfaceContainer.width, surfaceContainer.height).also { r ->
                visibleArea?.let { r.setVisibleArea(it) }
                r.blocked = !motion.parked
                updateVideo(r)
                r.start()
            }
        }

        override fun onVisibleAreaChanged(visibleArea: Rect) {
            this@VideoScreen.visibleArea = Rect(visibleArea)
            renderer?.setVisibleArea(visibleArea)
        }

        override fun onStableAreaChanged(stableArea: Rect) {}

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            renderer?.release()
            renderer = null
        }
    }

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                CaptureManager.addListener(this@VideoScreen)
                CaptureManager.selectFirstIfIdle()
                motion.start()
                carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                motion.stop()
                CaptureManager.removeListener(this@VideoScreen)
                renderer?.release()
                renderer = null
            }
        })
    }

    override fun onCaptureStateChanged() {
        renderer?.let { updateVideo(it) }
        invalidate()
    }

    private fun updateVideo(r: CarSurfaceRenderer) {
        val size = CaptureManager.previewSize
        r.setVideo(CaptureManager.isOpen, size?.width ?: 0, size?.height ?: 0)
    }

    override fun onGetTemplate(): Template {
        val audioOn = AudioController.enabled && AudioController.hasPermission()
        val audioAction = Action.Builder()
            .setTitle(if (audioOn) "Som: ligado" else "Som: desligado")
            .setOnClickListener {
                AudioController.setEnabled(!AudioController.enabled)
                invalidate()
            }
            .build()

        val builder = NavigationTemplate.Builder()
            .setActionStrip(ActionStrip.Builder().addAction(audioAction).build())

        val message = when {
            !CaptureManager.isOpen -> CaptureManager.statusText.ifBlank { "Abrindo a placa…" }
            !motion.parked -> motion.reason
            else -> null
        }
        if (message != null) {
            builder.setNavigationInfo(MessageInfo.Builder("Video Input Monitor").setText(message).build())
        }
        return builder.build()
    }
}
