package br.com.seunome.mobulite.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class FcmEvent(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap()
)

object FcmEventBus {
    private val _events = MutableSharedFlow<FcmEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun post(event: FcmEvent) {
        _events.tryEmit(event)
    }
}
