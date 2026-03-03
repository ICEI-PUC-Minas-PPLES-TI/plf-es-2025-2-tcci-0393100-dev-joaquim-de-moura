package br.com.seunome.mobulite.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

suspend fun getAddressFromLatLng(
    apiKey: String,
    lat: Double,
    lng: Double
): String = withContext(Dispatchers.IO) {

    val url =
        "https://maps.googleapis.com/maps/api/geocode/json" +
                "?latlng=$lat,$lng" +
                "&key=$apiKey"

    val response = URL(url).readText()
    val json = JSONObject(response)

    val results = json.getJSONArray("results")

    if (results.length() == 0)
        return@withContext "Endereço não encontrado"

    results
        .getJSONObject(0)
        .getString("formatted_address")
}