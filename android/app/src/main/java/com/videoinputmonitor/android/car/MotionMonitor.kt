package com.videoinputmonitor.android.car

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.Speed
import androidx.core.content.ContextCompat
import kotlin.math.abs

/**
 * Decide se o carro está parado. Usa a velocidade informada pelo carro e,
 * se ela não estiver disponível, a velocidade do GPS do celular. Na dúvida
 * (sem nenhum dado), considera que o carro está em movimento.
 */
class MotionMonitor(
    private val carContext: CarContext,
    private val onChange: (MotionMonitor) -> Unit,
) {
    var parked = false
        private set
    var reason = "Verificando se o carro está parado…"
        private set

    private val main = Handler(Looper.getMainLooper())
    private var carSpeed: Float? = null
    private var gpsSpeed: Float? = null
    private var gpsTime = 0L
    private var lastMovingAt = SystemClock.elapsedRealtime()
    private var carListenerRegistered = false
    private var gpsRegistered = false

    private val speedListener = OnCarDataAvailableListener<Speed> { speed ->
        val value = speed.rawSpeedMetersPerSecond
        if (value.status == CarValue.STATUS_SUCCESS) {
            value.value?.let { carSpeed = abs(it) }
        }
        evaluate()
    }

    private val locationListener = LocationListener { location ->
        if (location.hasSpeed()) {
            gpsSpeed = location.speed
            gpsTime = SystemClock.elapsedRealtime()
        }
        evaluate()
    }

    private val tick = object : Runnable {
        override fun run() {
            evaluate()
            main.postDelayed(this, 1000)
        }
    }

    fun start() {
        try {
            carContext.getCarService(CarHardwareManager::class.java).carInfo
                .addSpeedListener(ContextCompat.getMainExecutor(carContext), speedListener)
            carListenerRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "velocidade do carro indisponível", e)
        }
        if (ContextCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val lm = carContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener, Looper.getMainLooper())
                gpsRegistered = true
            } catch (e: Exception) {
                Log.w(TAG, "GPS indisponível", e)
            }
        }
        main.post(tick)
    }

    fun stop() {
        main.removeCallbacks(tick)
        if (carListenerRegistered) {
            try {
                carContext.getCarService(CarHardwareManager::class.java).carInfo.removeSpeedListener(speedListener)
            } catch (_: Exception) {
            }
            carListenerRegistered = false
        }
        if (gpsRegistered) {
            try {
                (carContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                    .removeUpdates(locationListener)
            } catch (_: Exception) {
            }
            gpsRegistered = false
        }
    }

    private fun evaluate() {
        val now = SystemClock.elapsedRealtime()
        val speed = carSpeed ?: gpsSpeed?.takeIf { now - gpsTime < GPS_MAX_AGE_MS }
        val newParked: Boolean
        val newReason: String
        when {
            speed == null -> {
                newParked = false
                newReason = "Sem dados de velocidade para confirmar que o carro está parado. " +
                    "Permita a localização para o app no celular."
            }
            speed > MOVING_SPEED -> {
                lastMovingAt = now
                newParked = false
                newReason = MOVING_TEXT
            }
            now - lastMovingAt < PARKED_DELAY_MS -> {
                newParked = false
                newReason = MOVING_TEXT
            }
            else -> {
                newParked = true
                newReason = ""
            }
        }
        if (newParked != parked || newReason != reason) {
            parked = newParked
            reason = newReason
            onChange(this)
        }
    }

    companion object {
        private const val TAG = "MotionMonitor"
        private const val MOVING_SPEED = 1.0f // m/s (~3,6 km/h)
        private const val PARKED_DELAY_MS = 3000L
        private const val GPS_MAX_AGE_MS = 5000L
        private const val MOVING_TEXT =
            "Vídeo disponível só com o carro parado. Os passageiros podem continuar na tela do celular."
    }
}
