package br.com.seunome.mobulite.data.remote

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

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