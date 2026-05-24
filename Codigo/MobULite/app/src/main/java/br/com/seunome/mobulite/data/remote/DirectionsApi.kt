package br.com.seunome.mobulite.data.remote

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class DirectionsStep(
    val instruction: String,
    val maneuver: String,
    val distanceMeters: Int,
    val endLocation: LatLng
)

data class RouteResult(
    val steps: List<DirectionsStep>,
    val polyline: List<LatLng>,
    val totalDurationSeconds: Int = 0
)

// Single API call returning both the visual polyline and turn-by-turn steps
suspend fun fetchRoute(
    apiKey: String,
    origin: LatLng,
    destination: LatLng
): RouteResult = withContext(Dispatchers.IO) {
    val originStr = "${origin.latitude},${origin.longitude}"
    val destStr   = "${destination.latitude},${destination.longitude}"

    val url = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=${URLEncoder.encode(originStr, "UTF-8")}" +
            "&destination=${URLEncoder.encode(destStr, "UTF-8")}" +
            "&mode=driving" +
            "&language=pt-BR" +
            "&key=${URLEncoder.encode(apiKey, "UTF-8")}"

    val json   = JSONObject(URL(url).readText())
    val routes = json.optJSONArray("routes")
        ?: return@withContext RouteResult(emptyList(), emptyList())
    if (routes.length() == 0) return@withContext RouteResult(emptyList(), emptyList())

    val route   = routes.getJSONObject(0)
    val polyline = PolyUtil.decode(route.getJSONObject("overview_polyline").getString("points"))

    val legs    = route.optJSONArray("legs")
        ?: return@withContext RouteResult(emptyList(), polyline)
    if (legs.length() == 0) return@withContext RouteResult(emptyList(), polyline)

    val leg = legs.getJSONObject(0)
    val totalDurationSeconds = leg.optJSONObject("duration")?.optInt("value", 0) ?: 0
    val stepsJson = leg.optJSONArray("steps")
        ?: return@withContext RouteResult(emptyList(), polyline)

    val steps = (0 until stepsJson.length()).map { i ->
        val step   = stepsJson.getJSONObject(i)
        val endLoc = step.getJSONObject("end_location")
        DirectionsStep(
            instruction    = step.optString("html_instructions")
                .replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim(),
            maneuver       = step.optString("maneuver", "straight"),
            distanceMeters = step.getJSONObject("distance").optInt("value", 0),
            endLocation    = LatLng(endLoc.getDouble("lat"), endLoc.getDouble("lng"))
        )
    }
    RouteResult(steps, polyline, totalDurationSeconds)
}

suspend fun fetchRouteSteps(
    apiKey: String,
    origin: LatLng,
    destination: LatLng
): List<DirectionsStep> = withContext(Dispatchers.IO) {
    val originStr = "${origin.latitude},${origin.longitude}"
    val destStr = "${destination.latitude},${destination.longitude}"

    val url = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=${URLEncoder.encode(originStr, "UTF-8")}" +
            "&destination=${URLEncoder.encode(destStr, "UTF-8")}" +
            "&mode=driving" +
            "&language=pt-BR" +
            "&key=${URLEncoder.encode(apiKey, "UTF-8")}"

    val json = JSONObject(URL(url).readText())
    val routes = json.optJSONArray("routes") ?: return@withContext emptyList()
    if (routes.length() == 0) return@withContext emptyList()

    val legs = routes.getJSONObject(0).optJSONArray("legs") ?: return@withContext emptyList()
    if (legs.length() == 0) return@withContext emptyList()

    val steps = legs.getJSONObject(0).optJSONArray("steps") ?: return@withContext emptyList()

    (0 until steps.length()).map { i ->
        val step = steps.getJSONObject(i)
        val endLoc = step.getJSONObject("end_location")
        DirectionsStep(
            instruction = step.optString("html_instructions")
                .replace(Regex("<[^>]*>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim(),
            maneuver = step.optString("maneuver", "straight"),
            distanceMeters = step.getJSONObject("distance").optInt("value", 0),
            endLocation = LatLng(endLoc.getDouble("lat"), endLoc.getDouble("lng"))
        )
    }
}

suspend fun fetchRoutePoints(
    apiKey: String,
    origin: LatLng,
    destination: LatLng
): List<LatLng> = withContext(Dispatchers.IO) {

    val originStr = "${origin.latitude},${origin.longitude}"
    val destStr = "${destination.latitude},${destination.longitude}"

    val url = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=${URLEncoder.encode(originStr, "UTF-8")}" +
            "&destination=${URLEncoder.encode(destStr, "UTF-8")}" +
            "&mode=driving" +
            "&key=${URLEncoder.encode(apiKey, "UTF-8")}"

    val jsonText = URL(url).readText()
    val json = JSONObject(jsonText)

    val routes = json.optJSONArray("routes") ?: return@withContext emptyList()
    if (routes.length() == 0) return@withContext emptyList()

    val overviewPolyline =
        routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points")

    PolyUtil.decode(overviewPolyline)
}