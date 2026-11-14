package com.videoinputmonitor.android.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Entrada do Android Auto. Como o app é instalado fora da Play Store, é
 * preciso ativar "Fontes desconhecidas" nas opções de desenvolvedor do
 * Android Auto para que ele apareça no carro.
 */
class VideoCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = VideoSession()
}

class VideoSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = VideoScreen(carContext)
}
