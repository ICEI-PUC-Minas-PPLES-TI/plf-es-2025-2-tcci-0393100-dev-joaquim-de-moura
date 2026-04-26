package br.com.seunome.mobulite.ui

enum class PassengerRideUiState {
    IDLE,
    REQUESTING,
    SEARCHING_DRIVER,
    DRIVER_ACCEPTED,
    IN_PROGRESS,
    FINISHED,
    CANCELED,
    ERROR
}