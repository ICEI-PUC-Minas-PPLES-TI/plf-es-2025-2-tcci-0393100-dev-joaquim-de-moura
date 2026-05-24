package br.com.seunome.mobulite.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.seunome.mobulite.data.local.SessionStore
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.data.remote.UpdateStatusRequest
import kotlinx.coroutines.flow.first

/**
 * Runs once at app startup to clean up rides that became orphaned during a crash.
 *
 * Only cancels rides in PENDING_DRIVER (waiting for a driver — no one accepted yet).
 * Active rides (ACCEPTED, DRIVER_ARRIVING, etc.) are left intact so the app can
 * restore the live session on the map screen. FINISHED/CANCELED rides just have
 * their stored ID cleared.
 */
class CancelPendingRideWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val session = SessionStore(applicationContext)
        val token = session.tokenFlow.first()
        val pendingRideId = session.pendingRideIdFlow.first()

        if (token.isNullOrBlank() || pendingRideId.isNullOrBlank()) return Result.success()

        RetrofitClient.init(applicationContext)
        RetrofitClient.setToken(token)

        return try {
            val ride = RetrofitClient.api.getRideById(pendingRideId)
            when (ride.status) {
                "PENDING_DRIVER" -> {
                    // No driver accepted yet — safe to cancel the orphaned request
                    RetrofitClient.api.updateRideStatus(
                        rideId = pendingRideId,
                        body = UpdateStatusRequest(status = "CANCELED"),
                    )
                    session.savePendingRideId(null)
                }
                "FINISHED", "CANCELED" -> {
                    // Ride is already done — just clear the stored ID
                    session.savePendingRideId(null)
                }
                // ACCEPTED, DRIVER_ARRIVING, DRIVER_ARRIVED, IN_PROGRESS:
                // Leave the ID — PassengerScreen will restore the active session
            }
            Result.success()
        } catch (_: Exception) {
            // Can't reach the server — leave the ID so the app can try to restore it
            Result.success()
        }
    }
}
