package br.com.seunome.mobulite.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

fun openGoogleMapsNavigation(
    context: Context,
    lat: Double,
    lng: Double
) {
    val uri = Uri.parse("google.navigation:q=$lat,$lng")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    context.startActivity(intent)
}