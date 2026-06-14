package br.com.seunome.mobulite

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import br.com.seunome.mobulite.data.local.SessionStore
import kotlinx.coroutines.flow.first
import android.content.Context
import br.com.seunome.mobulite.data.remote.CouponValidationResponse
import br.com.seunome.mobulite.data.remote.CreateRideRequest
import br.com.seunome.mobulite.data.remote.DriverLocationSummary
import br.com.seunome.mobulite.data.remote.EstimateRideRequest
import br.com.seunome.mobulite.data.remote.EstimateRideResponse
import br.com.seunome.mobulite.data.remote.NearbyDriverLocation
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.data.remote.UpdateStatusRequest
import br.com.seunome.mobulite.data.remote.ValidateCouponRequest
import br.com.seunome.mobulite.data.remote.getAddressFromLatLng
import br.com.seunome.mobulite.service.PassengerRideForegroundService
import br.com.seunome.mobulite.ui.ChatFab
import br.com.seunome.mobulite.ui.PassengerRideStatusBanner
import br.com.seunome.mobulite.ui.PassengerRideUiState
import androidx.compose.ui.unit.Dp
import br.com.seunome.mobulite.ui.theme.AppAmberOrange
import br.com.seunome.mobulite.ui.theme.AppGreen
import br.com.seunome.mobulite.ui.theme.AppGreenBg
import br.com.seunome.mobulite.ui.theme.AppGreenDark
import br.com.seunome.mobulite.ui.theme.AppGreenOnline
import br.com.seunome.mobulite.ui.theme.AppPurple
import br.com.seunome.mobulite.ui.theme.AppRed
import br.com.seunome.mobulite.ui.theme.Slate500
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private val MAPS_API_KEY get() = br.com.seunome.mobulite.BuildConfig.MAPS_API_KEY

private val RouteBlue = Color(0xFF1A73E8)
private val OriginGreen = AppGreenOnline
private val MapAttributionSafeInset = 48.dp

@SuppressLint("MissingPermission")
@Composable
fun MapRideScreen(
    modifier: Modifier = Modifier,
    mapBottomPadding: Dp = 0.dp,
    encodedPolyline: String? = null,
    initialDestinationQuery: String? = null,
    initialRideUiState: PassengerRideUiState = PassengerRideUiState.IDLE,
    initialRideId: String? = null,
    onPointsChanged: (origin: LatLng?, dest: LatLng?, originText: String, destText: String) -> Unit,
    onRideStateChanged: (
        uiState: PassengerRideUiState,
        rideId: String?,
        driverName: String?,
        errorMessage: String?
    ) -> Unit,
    onDriverUpdate: (driverInfo: DriverLocationSummary?, etaMinutes: Int?, paymentMethod: String) -> Unit = { _, _, _ -> },
    onOpenChat: (rideId: String, partnerName: String) -> Unit = { _, _ -> },
    chatIsOpen: Boolean = false,
    onCouponChanged: (CouponValidationResponse?) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val placesClient: PlacesClient = remember { Places.createClient(context) }

    var originMarker by remember { mutableStateOf<com.google.android.gms.maps.model.BitmapDescriptor?>(null) }
    var destinationMarker by remember { mutableStateOf<com.google.android.gms.maps.model.BitmapDescriptor?>(null) }
    var driverMarker by remember { mutableStateOf<com.google.android.gms.maps.model.BitmapDescriptor?>(null) }

    var isMapPickerMode by remember { mutableStateOf(false) }
    var pickerAddress by remember { mutableStateOf("") }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // gpsPosition: raw GPS updated every 2 s — used only for the live origin marker
    // origin: stable for route fetch and geocoding, set once on first GPS fix then only on user action
    var gpsPosition by remember { mutableStateOf<LatLng?>(null) }
    var origin by remember { mutableStateOf<LatLng?>(null) }
    var didSetInitialOrigin by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf<LatLng?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var routeError by remember { mutableStateOf<String?>(null) }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var placesError by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var originAddress by remember { mutableStateOf("") }
    var destinationAddress by remember { mutableStateOf("") }
    var editingOrigin by remember { mutableStateOf(false) }
    var editingDestination by remember { mutableStateOf(true) }
    var originQuery by remember { mutableStateOf("") }
    var destinationQuery by remember { mutableStateOf("") }
    var rideUiState by remember { mutableStateOf(initialRideUiState) }
    var currentRideId by remember { mutableStateOf(initialRideId) }
    var rideError by remember { mutableStateOf<String?>(null) }
    var acceptedDriverName by remember { mutableStateOf<String?>(null) }
    var driverMarkerPos by remember { mutableStateOf<LatLng?>(null) }
    var driverInfo by remember { mutableStateOf<DriverLocationSummary?>(null) }
    var driverEtaMinutes by remember { mutableStateOf<Int?>(null) }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var showSosSheet by remember { mutableStateOf(false) }
    var showCancelReasonDialog by remember { mutableStateOf(false) }
    var passengerChatUnread by remember { mutableStateOf(0) }
    var passengerChatPrevCount by remember { mutableStateOf(0) }
    var selectedCancelReason by remember { mutableStateOf("Mudei de ideia") }
    var nearbyDrivers by remember { mutableStateOf<List<NearbyDriverLocation>>(emptyList()) }
    var searchCardHeightPx by remember { mutableStateOf(0) }
    var estimateResult by remember { mutableStateOf<EstimateRideResponse?>(null) }
    var estimateLoading by remember { mutableStateOf(false) }
    var couponCode     by remember { mutableStateOf("") }
    var couponApplied  by remember { mutableStateOf<CouponValidationResponse?>(null) }
    var couponLoading  by remember { mutableStateOf(false) }
    var couponError    by remember { mutableStateOf<String?>(null) }
    var savedHome by remember { mutableStateOf<String?>(null) }
    var savedWork by remember { mutableStateOf<String?>(null) }
    var searchHistory by remember { mutableStateOf<List<String>>(emptyList()) }

    val searchPrefs = remember { context.getSharedPreferences("passenger_search_hist", Context.MODE_PRIVATE) }
    val placesPrefs = remember { context.getSharedPreferences("passenger_saved_places", Context.MODE_PRIVATE) }

    val density = LocalDensity.current
    val sessionStore = remember { SessionStore(context) }
    val cameraPositionState = rememberCameraPositionState()
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Load saved places and search history from SharedPreferences
    LaunchedEffect(Unit) {
        savedHome = placesPrefs.getString("home", null)
        savedWork = placesPrefs.getString("work", null)
        val raw = searchPrefs.getString("history", null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                val arr = org.json.JSONArray(raw)
                searchHistory = (0 until arr.length()).map { arr.getString(it) }
            }
        }
    }

    // Restaura corrida ativa ao abrir o app após crash/fechamento
    LaunchedEffect(Unit) {
        if (currentRideId != null) {
            try {
                val ride = RetrofitClient.api.getRideById(currentRideId!!)
                ride.originAddress?.let { originAddress = it; originQuery = it }
                ride.destinationAddress?.let { destinationAddress = it; destinationQuery = it }
                ride.driver?.name?.let { acceptedDriverName = it }
                ride.driver?.let { d ->
                    val lat = d.currentLat
                    val lng = d.currentLng
                    if (lat != null && lng != null) driverMarkerPos = LatLng(lat, lng)
                    driverInfo = d
                }
                onRideStateChanged(rideUiState, currentRideId, acceptedDriverName, null)
            } catch (_: Exception) {}
            return@LaunchedEffect
        }

        val savedRideId = sessionStore.pendingRideIdFlow.first() ?: return@LaunchedEffect
        try {
            val ride = RetrofitClient.api.getRideById(savedRideId)
            val restoredState = when (ride.status) {
                "PENDING_DRIVER" -> PassengerRideUiState.SEARCHING_DRIVER
                "ACCEPTED"       -> PassengerRideUiState.DRIVER_ACCEPTED
                "DRIVER_ARRIVING"-> PassengerRideUiState.DRIVER_ARRIVING
                "DRIVER_ARRIVED" -> PassengerRideUiState.DRIVER_ARRIVED
                "IN_PROGRESS"    -> PassengerRideUiState.IN_PROGRESS
                "FINISHED"       -> { sessionStore.savePendingRideId(null); PassengerRideUiState.FINISHED }
                "CANCELED"       -> { sessionStore.savePendingRideId(null); PassengerRideUiState.CANCELED }
                else             -> null
            } ?: return@LaunchedEffect

            currentRideId = savedRideId
            rideUiState = restoredState
            ride.originAddress?.let { originAddress = it; originQuery = it }
            ride.destinationAddress?.let { destinationAddress = it; destinationQuery = it }
            ride.driver?.name?.let { acceptedDriverName = it }
            ride.driver?.let { d ->
                val lat = d.currentLat
                val lng = d.currentLng
                if (lat != null && lng != null) driverMarkerPos = LatLng(lat, lng)
                driverInfo = d
            }
            onRideStateChanged(rideUiState, currentRideId, acceptedDriverName, null)
        } catch (_: Exception) {
            sessionStore.savePendingRideId(null)
        }
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                gpsPosition = LatLng(loc.latitude, loc.longitude)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            onDispose { }
        } else {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .build()
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            onDispose { fusedClient.removeLocationUpdates(locationCallback) }
        }
    }

    fun searchPlaces(text: String) {
        if (text.length < 3) {
            predictions = emptyList()
            return
        }
        searching = true
        placesError = null
        val req = FindAutocompletePredictionsRequest.builder()
            .setQuery(text)
            .setCountries("BR")
            .build()
        placesClient.findAutocompletePredictions(req)
            .addOnSuccessListener { res ->
                predictions = res.autocompletePredictions
                searching = false
            }
            .addOnFailureListener { e ->
                placesError = e.message
                predictions = emptyList()
                searching = false
            }
    }

    fun choosePrediction(prediction: AutocompletePrediction) {
        placesError = null
        predictions = emptyList()
        val placeId = prediction.placeId
        val fields = listOf(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS)
        val fetchReq = FetchPlaceRequest.builder(placeId, fields).build()
        placesClient.fetchPlace(fetchReq)
            .addOnSuccessListener { res ->
                val latLng = res.place.latLng
                if (latLng != null) {
                    val selectedText = prediction.getFullText(null).toString()
                    if (editingOrigin) {
                        origin = latLng
                        originQuery = selectedText
                        originAddress = selectedText
                        editingOrigin = false
                    } else {
                        destination = latLng
                        destinationQuery = selectedText
                        destinationAddress = selectedText
                        editingDestination = false
                        // Save to search history
                        val newHistory = (listOf(selectedText) + searchHistory).distinct().take(5)
                        searchHistory = newHistory
                        searchPrefs.edit().putString("history", org.json.JSONArray(newHistory).toString()).apply()
                    }
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f), 600)
                    }
                }
            }
            .addOnFailureListener { e -> placesError = e.message }
    }

    // Set stable origin on first GPS fix (not on every 2-second GPS tick)
    LaunchedEffect(gpsPosition) {
        val gps = gpsPosition ?: return@LaunchedEffect
        if (!didSetInitialOrigin) {
            didSetInitialOrigin = true
            origin = gps
        }
    }

    // Decode the encoded polyline returned by the backend estimate endpoint.
    // Keying on encodedPolyline (not origin/destination) avoids a redundant
    // direct call to the Directions API which fails when the key has Android
    // app restrictions.
    LaunchedEffect(encodedPolyline) {
        routeError = null
        routePoints = if (!encodedPolyline.isNullOrBlank()) {
            try {
                PolyUtil.decode(encodedPolyline)
            } catch (e: Exception) {
                routeError = e.message
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    var didCenterOnce by remember { mutableStateOf(false) }

    // Center camera on first GPS fix
    LaunchedEffect(gpsPosition) {
        val gps = gpsPosition ?: return@LaunchedEffect
        if (!didCenterOnce) {
            didCenterOnce = true
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(gps, 16f), 700)
        }
    }

    LaunchedEffect(routePoints) {
        if (routePoints.size < 2) return@LaunchedEffect
        val boundsBuilder = LatLngBounds.Builder()
        routePoints.forEach { boundsBuilder.include(it) }
        cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 140), 800)
    }

    LaunchedEffect(originAddress) {
        if (!editingOrigin && originAddress.isNotBlank()) originQuery = originAddress
    }

    LaunchedEffect(destinationAddress) {
        if (!editingDestination && destinationAddress.isNotBlank()) destinationQuery = destinationAddress
    }

    // Map-picker mode: geocode center when mode is toggled or camera stops
    LaunchedEffect(isMapPickerMode) {
        if (isMapPickerMode) {
            val center = cameraPositionState.position.target
            pickerAddress = "..."
            try {
                pickerAddress = getAddressFromLatLng(
                    apiKey = MAPS_API_KEY,
                    lat = center.latitude,
                    lng = center.longitude
                ).ifBlank { "${center.latitude}, ${center.longitude}" }
            } catch (_: Exception) {
                pickerAddress = "${center.latitude}, ${center.longitude}"
            }
        } else {
            pickerAddress = ""
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!isMapPickerMode) return@LaunchedEffect
        if (cameraPositionState.isMoving) {
            pickerAddress = "..."
        } else {
            val center = cameraPositionState.position.target
            try {
                pickerAddress = getAddressFromLatLng(
                    apiKey = MAPS_API_KEY,
                    lat = center.latitude,
                    lng = center.longitude
                ).ifBlank { "${center.latitude}, ${center.longitude}" }
            } catch (_: Exception) {
                pickerAddress = "${center.latitude}, ${center.longitude}"
            }
        }
    }

    // Pre-fill destination when navigating from a recent ride on the home screen.
    // Keyed on initialDestinationQuery so it re-fires each time a new address is passed in.
    LaunchedEffect(initialDestinationQuery) {
        if (initialDestinationQuery.isNullOrBlank()) {
            // Null means the user returned to home without selecting a ride — clear the field.
            destination = null
            destinationQuery = ""
            destinationAddress = ""
            predictions = emptyList()
            return@LaunchedEffect
        }
        val query = initialDestinationQuery

        destinationQuery = query
        editingDestination = false
        editingOrigin = false

        try {
            val prediction = suspendCancellableCoroutine<AutocompletePrediction?> { cont ->
                val req = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .setCountries("BR")
                    .build()
                placesClient.findAutocompletePredictions(req)
                    .addOnSuccessListener { res -> cont.resume(res.autocompletePredictions.firstOrNull(), null) }
                    .addOnFailureListener { cont.resume(null, null) }
            } ?: run {
                // No prediction found — show autocomplete for manual selection
                editingDestination = true
                searchPlaces(query)
                return@LaunchedEffect
            }

            val latLng = suspendCancellableCoroutine<LatLng?> { cont ->
                val fields = listOf(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS)
                val fetchReq = FetchPlaceRequest.builder(prediction.placeId, fields).build()
                placesClient.fetchPlace(fetchReq)
                    .addOnSuccessListener { res -> cont.resume(res.place.latLng, null) }
                    .addOnFailureListener { cont.resume(null, null) }
            } ?: run {
                editingDestination = true
                searchPlaces(query)
                return@LaunchedEffect
            }

            val selectedText = prediction.getFullText(null).toString()
            destination = latLng
            destinationQuery = selectedText
            destinationAddress = selectedText
            editingDestination = false
            predictions = emptyList()

            scope.launch {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f), 600)
            }
        } catch (_: Exception) {
            // Fallback: show autocomplete list for manual selection
            editingDestination = true
            searchPlaces(query)
        }
    }

    // Geocode only when stable origin changes (not every GPS tick)
    LaunchedEffect(origin) {
        val o = origin ?: return@LaunchedEffect
        originAddress = try {
            getAddressFromLatLng(apiKey = MAPS_API_KEY, lat = o.latitude, lng = o.longitude)
        } catch (_: Exception) { originAddress }
    }

    // Notify parent only when destination or address labels change (not every GPS tick)
    LaunchedEffect(origin, destination, originAddress, destinationAddress) {
        onPointsChanged(gpsPosition ?: origin, destination, originAddress, destinationAddress)
    }

    // Auto-reset after CANCELED so passenger can request a new ride
    LaunchedEffect(rideUiState) {
        if (rideUiState != PassengerRideUiState.CANCELED) return@LaunchedEffect
        delay(4_000)
        rideUiState = PassengerRideUiState.IDLE
        currentRideId = null
        acceptedDriverName = null
        rideError = null
        driverMarkerPos = null
        driverInfo = null
        driverEtaMinutes = null
        onRideStateChanged(PassengerRideUiState.IDLE, null, null, null)
    }

    // ── Real-time status via Foreground Service WebSocket ─────────────────────
    val rideEvent by PassengerRideForegroundService.rideEventFlow.collectAsState()

    // Start / stop the foreground service when a ride is created or cleared
    LaunchedEffect(currentRideId) {
        val rideId = currentRideId
        if (rideId != null) {
            PassengerRideForegroundService.start(context, rideId)
        } else {
            PassengerRideForegroundService.stop(context)
        }
    }

    // Process ride status events delivered by the foreground service
    LaunchedEffect(rideEvent) {
        val event = rideEvent ?: return@LaunchedEffect
        when (event.status) {
            "ACCEPTED" -> {
                rideUiState = PassengerRideUiState.DRIVER_ACCEPTED
                acceptedDriverName = event.driverName
                event.driverLat?.let { lat ->
                    event.driverLng?.let { lng -> driverMarkerPos = LatLng(lat, lng) }
                }
                val rId = currentRideId
                if (!rId.isNullOrBlank()) {
                    try {
                        val r = RetrofitClient.api.getRideById(rId)
                        driverInfo = r.driver
                    } catch (_: Exception) {}
                }
            }
            "DRIVER_ARRIVING" -> rideUiState = PassengerRideUiState.DRIVER_ARRIVING
            "DRIVER_ARRIVED" -> rideUiState = PassengerRideUiState.DRIVER_ARRIVED
            "IN_PROGRESS" -> rideUiState = PassengerRideUiState.IN_PROGRESS
            "FINISHED" -> {
                rideUiState = PassengerRideUiState.FINISHED
                driverMarkerPos = null
                driverInfo = null
                driverEtaMinutes = null
                scope.launch { sessionStore.savePendingRideId(null) }
            }
            "PENDING_DRIVER" -> {
                // Driver canceled — ride re-queued, keep searching for another driver
                rideUiState = PassengerRideUiState.SEARCHING_DRIVER
                acceptedDriverName = null
                driverMarkerPos = null
                driverInfo = null
                driverEtaMinutes = null
            }
            "CANCELED" -> {
                rideUiState = PassengerRideUiState.CANCELED
                driverMarkerPos = null
                driverInfo = null
                driverEtaMinutes = null
                rideError = event.reason
                scope.launch { sessionStore.savePendingRideId(null) }
            }
        }
        onRideStateChanged(rideUiState, currentRideId, acceptedDriverName, rideError)
        PassengerRideForegroundService.clearEvent()
    }

    // Lightweight location poll (every 5 s) only while driver is en route
    LaunchedEffect(currentRideId, rideUiState) {
        val rideId = currentRideId ?: return@LaunchedEffect
        val needsLocation = rideUiState == PassengerRideUiState.DRIVER_ACCEPTED ||
                rideUiState == PassengerRideUiState.DRIVER_ARRIVING ||
                rideUiState == PassengerRideUiState.DRIVER_ARRIVED ||
                rideUiState == PassengerRideUiState.IN_PROGRESS
        if (!needsLocation) return@LaunchedEffect
        while (true) {
            delay(5_000)
            try {
                val ride = RetrofitClient.api.getRideById(rideId)
                val lat = ride.driver?.currentLat
                val lng = ride.driver?.currentLng
                if (lat != null && lng != null) {
                    driverMarkerPos = LatLng(lat, lng)
                    // Compute ETA inline at 25 km/h urban speed for ACCEPTED / DRIVER_ARRIVING
                    val passengerPos = gpsPosition ?: origin
                    if ((rideUiState == PassengerRideUiState.DRIVER_ACCEPTED ||
                        rideUiState == PassengerRideUiState.DRIVER_ARRIVING) &&
                        passengerPos != null) {
                        val distKm = haversineKm(
                            lat1 = lat, lng1 = lng,
                            lat2 = passengerPos.latitude, lng2 = passengerPos.longitude
                        )
                        driverEtaMinutes = (distKm / 25.0 * 60).toInt().coerceAtLeast(1)
                    } else if (rideUiState != PassengerRideUiState.DRIVER_ACCEPTED &&
                        rideUiState != PassengerRideUiState.DRIVER_ARRIVING) {
                        driverEtaMinutes = null
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // ETA: estimate driver arrival time from haversine distance at 25 km/h (fallback when polling hasn't run yet)
    LaunchedEffect(driverMarkerPos, rideUiState) {
        val dp = driverMarkerPos ?: return@LaunchedEffect
        val pp = gpsPosition ?: origin ?: return@LaunchedEffect
        if (rideUiState == PassengerRideUiState.DRIVER_ACCEPTED ||
            rideUiState == PassengerRideUiState.DRIVER_ARRIVING) {
            val distKm = haversineKm(dp, pp)
            driverEtaMinutes = (distKm / 25.0 * 60).toInt().coerceAtLeast(1)
        } else {
            driverEtaMinutes = null
        }
    }

    // Badge de mensagens não lidas — poll em background quando chat está fechado
    LaunchedEffect(currentRideId, chatIsOpen) {
        val rideId = currentRideId
        if (rideId == null) { passengerChatUnread = 0; passengerChatPrevCount = 0; return@LaunchedEffect }
        // Inicializa baseline ao abrir/fechar chat
        runCatching {
            val msgs = RetrofitClient.api.getChatMessages(rideId)
            passengerChatPrevCount = msgs.size
        }
        passengerChatUnread = 0
        if (chatIsOpen) return@LaunchedEffect   // chat aberto: ChatScreen faz o próprio poll
        val showSosNow = rideUiState == PassengerRideUiState.DRIVER_ARRIVING ||
                rideUiState == PassengerRideUiState.DRIVER_ARRIVED ||
                rideUiState == PassengerRideUiState.IN_PROGRESS
        if (!showSosNow) return@LaunchedEffect
        while (true) {
            delay(5_000)
            runCatching {
                val msgs = RetrofitClient.api.getChatMessages(rideId)
                val n = msgs.size
                if (n > passengerChatPrevCount) {
                    passengerChatUnread += n - passengerChatPrevCount
                    passengerChatPrevCount = n
                }
            }
        }
    }

    // Nearby drivers: poll while idle so passenger sees available cars on map
    LaunchedEffect(rideUiState) {
        if (rideUiState != PassengerRideUiState.IDLE) {
            nearbyDrivers = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            try {
                nearbyDrivers = RetrofitClient.api.getAvailableDrivers().take(20)
            } catch (_: Exception) {}
            delay(10_000)
        }
    }

    // Fare + trip ETA estimate — called when origin+destination are both set
    LaunchedEffect(origin, destination) {
        val o = origin
        val d = destination
        if (o == null || d == null) { estimateResult = null; return@LaunchedEffect }
        if (rideUiState != PassengerRideUiState.IDLE) return@LaunchedEffect
        estimateLoading = true
        estimateResult = runCatching {
            RetrofitClient.api.estimateRide(
                EstimateRideRequest(originLat = o.latitude, originLng = o.longitude, destLat = d.latitude, destLng = d.longitude)
            )
        }.getOrNull()
        estimateLoading = false
    }

    LaunchedEffect(driverInfo, driverEtaMinutes, paymentMethod) {
        onDriverUpdate(driverInfo, driverEtaMinutes, paymentMethod)
    }

    val hasActiveRideRequest = rideUiState == PassengerRideUiState.REQUESTING ||
            rideUiState == PassengerRideUiState.SEARCHING_DRIVER

    val showCompactHeader = rideUiState == PassengerRideUiState.REQUESTING ||
            rideUiState == PassengerRideUiState.SEARCHING_DRIVER ||
            rideUiState == PassengerRideUiState.DRIVER_ACCEPTED ||
            rideUiState == PassengerRideUiState.DRIVER_ARRIVING ||
            rideUiState == PassengerRideUiState.DRIVER_ARRIVED ||
            rideUiState == PassengerRideUiState.IN_PROGRESS

    Box(modifier = modifier.fillMaxSize()) {
        if (!hasLocationPermission) {
            // ── Permission request screen ──────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "Localização necessária",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Precisamos da sua localização para exibir o mapa e calcular a rota.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.MyLocation, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Permitir acesso à localização", style = MaterialTheme.typography.titleSmall)
                }
            }
        } else {
            // ── Map ────────────────────────────────────────────────────────
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false
                ),
                contentPadding = PaddingValues(
                    top = with(density) { searchCardHeightPx.toDp() } + 12.dp,
                    bottom = mapBottomPadding + MapAttributionSafeInset,
                    start = 12.dp,
                    end = 12.dp,
                ),
                onMapLoaded = {
                    if (originMarker == null) {
                        originMarker = createCircleMarker(context, AppGreenOnline, 30)
                        destinationMarker = createPinMarker(context, AppPurple)
                        driverMarker = createDriverMarker(context)
                    }
                }
            ) {
                // Route: shadow + colored line with round caps
                if (routePoints.isNotEmpty()) {
                    Polyline(
                        points = routePoints,
                        color = Color(0x55000000),
                        width = 22f
                    )
                    Polyline(
                        points = routePoints,
                        color = RouteBlue,
                        width = 13f,
                        startCap = RoundCap(),
                        endCap = RoundCap()
                    )
                }

                // Origin marker (green) — follows live GPS position
                (gpsPosition ?: origin)?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Origem",
                        icon = originMarker
                    )
                }

                // Destination marker (purple pin) — hidden in picker mode
                if (!isMapPickerMode) {
                    destination?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Destino",
                            icon = destinationMarker
                        )
                    }
                }

                // Nearby driver markers (idle only)
                if (rideUiState == PassengerRideUiState.IDLE && driverMarker != null) {
                    nearbyDrivers.forEach { nd ->
                        Marker(
                            state = MarkerState(position = LatLng(nd.lat, nd.lng)),
                            title = "Motorista disponível",
                            icon = driverMarker
                        )
                    }
                }

                // Driver marker (custom car icon)
                driverMarkerPos?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = acceptedDriverName ?: "Motorista",
                        icon = driverMarker
                    )
                }
            }

            // Driver approaching pulse
            if ((rideUiState == PassengerRideUiState.DRIVER_ARRIVING ||
                         rideUiState == PassengerRideUiState.DRIVER_ACCEPTED) && driverMarkerPos != null) {
                val screenPos = cameraPositionState.projection?.toScreenLocation(driverMarkerPos!!)
                if (screenPos != null) {
                    DriverApproachingPulse(screenX = screenPos.x.toFloat(), screenY = screenPos.y.toFloat())
                }
            }

            // ── Overlay: compact destination strip OR full search card ─────────
            AnimatedContent(
                targetState = showCompactHeader,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "top_card",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                    .onGloballyPositioned { coords -> searchCardHeightPx = coords.size.height }
            ) { isCompact ->
                if (isCompact) {
                    // Compact destination strip during active ride
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Destino",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    destinationAddress.ifBlank { "—" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (hasActiveRideRequest) {
                                IconButton(
                                    onClick = { showCancelReasonDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Cancelar corrida",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                } else {
                if (isMapPickerMode) {
                    // Picker pill — shown when user drags the map to pick a destination
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords -> searchCardHeightPx = coords.size.height }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { isMapPickerMode = false }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = AppPurple
                                )
                            }
                            Column {
                                Text(
                                    "Escolher no mapa",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Arraste para posicionar o marcador",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                            }
                        }
                    }
                } else {
                // Full search card — shown when idle / after ride ends
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords -> searchCardHeightPx = coords.size.height },
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // ── Fields row ─────────────────────────────────────
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Visual indicator: green dot → swap button → red pin
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(OriginGreen, CircleShape)
                                )
                                IconButton(
                                    onClick = {
                                        // Swap origin ↔ destination
                                        val tmpLatLng = origin
                                        val tmpQuery = originQuery
                                        val tmpAddr = originAddress
                                        origin = destination
                                        originQuery = destinationQuery
                                        originAddress = destinationAddress
                                        destination = tmpLatLng
                                        destinationQuery = tmpQuery
                                        destinationAddress = tmpAddr
                                        editingOrigin = false
                                        editingDestination = false
                                        predictions = emptyList()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SwapVert,
                                        contentDescription = "Trocar origem e destino",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Text fields
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = originQuery,
                                    onValueChange = {
                                        originQuery = it
                                        editingOrigin = true
                                        editingDestination = false
                                        searchPlaces(it)
                                    },
                                    label = { Text("Origem") },
                                    placeholder = { Text("Sua localização atual") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OriginGreen,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                                OutlinedTextField(
                                    value = destinationQuery,
                                    onValueChange = {
                                        destinationQuery = it
                                        editingDestination = true
                                        editingOrigin = false
                                        searchPlaces(it)
                                    },
                                    label = { Text("Destino") },
                                    placeholder = { Text("Para onde vamos?") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.error,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    trailingIcon = {
                                        if (destinationQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    destinationQuery = ""
                                                    destinationAddress = ""
                                                    destination = null
                                                    predictions = emptyList()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Limpar destino",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // ── "Escolher no mapa" button ───────────────────────
                        TextButton(
                            onClick = {
                                isMapPickerMode = true
                                predictions = emptyList()
                                editingDestination = false
                                editingOrigin = false
                            },
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = AppPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "📍 Escolher no mapa",
                                color = AppPurple,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // ── Saved places (Casa/Trabalho) ────────────────────
                        AnimatedVisibility(
                            visible = editingDestination && predictions.isEmpty() && destinationQuery.length < 2 && (savedHome != null || savedWork != null),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text("Locais salvos", style = MaterialTheme.typography.labelSmall, color = Slate500, modifier = Modifier.padding(bottom = 6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    savedHome?.let { home ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                destinationQuery = home; editingDestination = false
                                                searchPlaces(home)
                                            },
                                            label = {
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Home, null, modifier = Modifier.size(13.dp))
                                                    Text("Casa", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        )
                                    }
                                    savedWork?.let { work ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                destinationQuery = work; editingDestination = false
                                                searchPlaces(work)
                                            },
                                            label = {
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Work, null, modifier = Modifier.size(13.dp))
                                                    Text("Trabalho", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // ── Search history ──────────────────────────────────
                        AnimatedVisibility(
                            visible = editingDestination && predictions.isEmpty() && destinationQuery.length < 2 && searchHistory.isNotEmpty(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Text("Recentes", style = MaterialTheme.typography.labelSmall, color = Slate500, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                searchHistory.forEach { address ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                destinationQuery = address
                                                editingDestination = true
                                                searchPlaces(address)
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.History, null, tint = Slate500, modifier = Modifier.size(16.dp))
                                        Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // ── Autocomplete suggestions ────────────────────────
                        AnimatedVisibility(
                            visible = predictions.isNotEmpty(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp)
                                ) {
                                    items(predictions) { p ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { choosePrediction(p) }
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    p.getPrimaryText(null).toString(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val secondary = p.getSecondaryText(null).toString()
                                                if (secondary.isNotBlank()) {
                                                    Text(
                                                        secondary,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 50.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        // ── Payment method selector ─────────────────────────
                        if ((gpsPosition ?: origin) != null && destination != null && !hasActiveRideRequest) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = paymentMethod == "CASH",
                                    onClick = { paymentMethod = "CASH" },
                                    label = { Text("Dinheiro") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = paymentMethod == "PIX",
                                    onClick = { paymentMethod = "PIX" },
                                    label = { Text("Pix") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // ── Coupon field ────────────────────────────────────
                        if ((gpsPosition ?: origin) != null && destination != null && !hasActiveRideRequest) {
                            Spacer(Modifier.height(8.dp))
                            CouponField(
                                code = couponCode,
                                onCodeChange = { couponCode = it; couponError = null; if (it.isBlank()) { couponApplied = null; onCouponChanged(null) } },
                                applied = couponApplied,
                                loading = couponLoading,
                                error = couponError,
                                onApply = {
                                    val c = couponCode.trim().uppercase()
                                    if (c.isBlank()) return@CouponField
                                    scope.launch {
                                        couponLoading = true; couponError = null
                                        runCatching {
                                            val result = RetrofitClient.api.validateCoupon(ValidateCouponRequest(c))
                                            couponApplied = result
                                            onCouponChanged(result)
                                        }.onFailure { couponError = "Cupom inválido ou expirado" }
                                        couponLoading = false
                                    }
                                },
                                onRemove = {
                                    couponApplied = null
                                    couponCode = ""
                                    couponError = null
                                    onCouponChanged(null)
                                }
                            )
                        }

                        // ── Request / cancel button ─────────────────────────
                        if ((gpsPosition ?: origin) != null && destination != null) {
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (hasActiveRideRequest) {
                                        showCancelReasonDialog = true
                                    } else {
                                        val o = gpsPosition ?: origin
                                        val d = destination
                                        if (o != null && d != null) {
                                            scope.launch {
                                                try {
                                                    rideUiState = PassengerRideUiState.REQUESTING
                                                    rideError = null
                                                    onRideStateChanged(rideUiState, currentRideId, acceptedDriverName, rideError)
                                                    val response = RetrofitClient.api.createRide(
                                                        CreateRideRequest(
                                                            originLat = o.latitude,
                                                            originLng = o.longitude,
                                                            destLat = d.latitude,
                                                            destLng = d.longitude,
                                                            originAddress = originAddress,
                                                            destinationAddress = destinationAddress,
                                                            paymentMethod = paymentMethod,
                                                            promoCode = couponApplied?.code
                                                        )
                                                    )
                                                    currentRideId = response.id
                                                    sessionStore.savePendingRideId(response.id)
                                                    rideUiState = PassengerRideUiState.SEARCHING_DRIVER
                                                    onRideStateChanged(rideUiState, currentRideId, acceptedDriverName, rideError)
                                                } catch (e: Exception) {
                                                    rideError = e.message ?: "Falha ao solicitar corrida"
                                                    rideUiState = PassengerRideUiState.ERROR
                                                    onRideStateChanged(rideUiState, currentRideId, acceptedDriverName, rideError)
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasActiveRideRequest) AppRed else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (hasActiveRideRequest) Icons.Default.Cancel else Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = when {
                                        rideUiState == PassengerRideUiState.REQUESTING -> "Solicitando..."
                                        hasActiveRideRequest -> "Cancelar corrida"
                                        else -> "Pedir corrida"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                } // else (full search card)
                } // else (not picker pill)
            } // AnimatedContent

            // ── Map picker: fixed center pin + confirm button ──────────────
            if (isMapPickerMode) {
                FixedCenterPin(address = pickerAddress, isMoving = cameraPositionState.isMoving)

                Button(
                    onClick = {
                        val center = cameraPositionState.position.target
                        destination = center
                        destinationAddress = pickerAddress
                        destinationQuery = pickerAddress
                        isMapPickerMode = false
                        editingDestination = false
                        predictions = emptyList()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = mapBottomPadding + 16.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Confirmar este local",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Map floating controls ─────────────────────────────────────
            val showSos = rideUiState == PassengerRideUiState.DRIVER_ARRIVING ||
                    rideUiState == PassengerRideUiState.DRIVER_ARRIVED ||
                    rideUiState == PassengerRideUiState.IN_PROGRESS

            if (!isMapPickerMode && ((gpsPosition ?: origin) != null || showSos)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = mapBottomPadding + 16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    (gpsPosition ?: origin)?.let { target ->
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    runCatching {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(target, 16f)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.size(52.dp),
                            containerColor = Color.White,
                            contentColor = AppPurple
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Minha localização")
                        }
                    }

                    if (showSos) {
                        ChatFab(
                            unreadCount = passengerChatUnread,
                            onClick = {
                                currentRideId?.let { rideId ->
                                    passengerChatUnread = 0
                                    onOpenChat(rideId, acceptedDriverName ?: "Motorista")
                                }
                            }
                        )
                        FloatingActionButton(
                            onClick = { showSosSheet = true },
                            modifier = Modifier.size(52.dp),
                            containerColor = AppRed,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "SOS")
                        }
                    }
                }
            }

            // ── Cancel reason dialog ───────────────────────────────────────
            if (showCancelReasonDialog) {
                val cancelReasons = listOf(
                    "Vai demorar muito",
                    "Mudei de ideia",
                    "Erro ao solicitar",
                    "Outro"
                )
                AlertDialog(
                    onDismissRequest = { showCancelReasonDialog = false },
                    title = { Text("Por que quer cancelar?", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            cancelReasons.forEach { reason ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCancelReason = reason },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedCancelReason == reason,
                                        onClick = { selectedCancelReason = reason }
                                    )
                                    Text(
                                        reason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCancelReasonDialog = false
                                scope.launch {
                                    try {
                                        val rideId = currentRideId
                                        if (!rideId.isNullOrBlank()) {
                                            RetrofitClient.api.updateRideStatus(
                                                rideId = rideId,
                                                body = UpdateStatusRequest(status = "CANCELED", cancelReason = selectedCancelReason)
                                            )
                                        }
                                        rideUiState = PassengerRideUiState.IDLE
                                        currentRideId = null
                                        acceptedDriverName = null
                                        rideError = null
                                        sessionStore.savePendingRideId(null)
                                        onRideStateChanged(rideUiState, currentRideId, acceptedDriverName, rideError)
                                    } catch (e: Exception) {
                                        rideError = e.message ?: "Erro ao cancelar"
                                        onRideStateChanged(rideUiState, currentRideId, acceptedDriverName, rideError)
                                    }
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = AppRed
                            )
                        ) {
                            Text("Cancelar corrida", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCancelReasonDialog = false }) { Text("Voltar") }
                    }
                )
            }

            // ── SOS emergency dialog ───────────────────────────────────────
            if (showSosSheet) {
                val ctx = LocalContext.current
                AlertDialog(
                    onDismissRequest = { showSosSheet = false },
                    title = {
                        Text("Precisa de ajuda?", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SosRow(label = "Polícia — 190") {
                                ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:190")))
                                showSosSheet = false
                            }
                            SosRow(label = "SAMU — 192") {
                                ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:192")))
                                showSosSheet = false
                            }
                            SosRow(label = "Bombeiros — 193") {
                                ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:193")))
                                showSosSheet = false
                            }
                            SosRow(label = "Suporte MobU", icon = Icons.Default.HeadsetMic) {
                                ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:suporte@mobulite.com.br")))
                                showSosSheet = false
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showSosSheet = false }) { Text("Fechar") }
                    }
                )
            }
        }
    }
}

@Composable
private fun DriverInfoCard(info: DriverLocationSummary, paymentMethod: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = AppPurple,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = info.name?.firstOrNull()?.uppercase() ?: "M",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = info.name ?: "Motorista",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val vehicleText = listOfNotNull(
                    info.vehicleModel?.takeIf { it.isNotBlank() },
                    info.vehicleColor?.takeIf { it.isNotBlank() },
                    info.vehiclePlate?.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (vehicleText.isNotBlank()) {
                    Text(
                        text = vehicleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (paymentMethod == "PIX") "Pagamento: Pix" else "Pagamento: Dinheiro",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AppPurple.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = AppPurple,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun SosRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Warning,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppRed.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = AppRed, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppRed
            )
        }
    }
}

private fun haversineKm(a: LatLng, b: LatLng): Double {
    val R = 6371.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val sinDLat = kotlin.math.sin(dLat / 2)
    val sinDLon = kotlin.math.sin(dLon / 2)
    val aa = sinDLat * sinDLat +
            kotlin.math.cos(Math.toRadians(a.latitude)) *
            kotlin.math.cos(Math.toRadians(b.latitude)) *
            sinDLon * sinDLon
    return 2 * R * kotlin.math.asin(kotlin.math.sqrt(aa))
}

private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lng2 - lng1)
    val sinDLat = kotlin.math.sin(dLat / 2)
    val sinDLon = kotlin.math.sin(dLon / 2)
    val aa = sinDLat * sinDLat +
            kotlin.math.cos(Math.toRadians(lat1)) *
            kotlin.math.cos(Math.toRadians(lat2)) *
            sinDLon * sinDLon
    return 2 * R * kotlin.math.asin(kotlin.math.sqrt(aa))
}

// ── Fixed-center map-picker pin ──────────────────────────────────────────────

@Composable
private fun FixedCenterPin(address: String, isMoving: Boolean) {
    val pinLift by animateDpAsState(
        targetValue = if (isMoving) 14.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pinLift"
    )
    val shadowAlpha by animateFloatAsState(
        targetValue = if (isMoving) 0.07f else 0.18f,
        animationSpec = tween(200),
        label = "shadowAlpha"
    )
    val shadowScale by animateFloatAsState(
        targetValue = if (isMoving) 1.7f else 1f,
        animationSpec = tween(200),
        label = "shadowScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val offsetY = 6.dp.toPx()
            val baseRx = 18.dp.toPx()
            val baseRy = 5.dp.toPx()
            drawOval(
                color = Color.Black.copy(alpha = shadowAlpha),
                topLeft = Offset(cx - baseRx * shadowScale, cy + offsetY - baseRy * shadowScale),
                size = Size(baseRx * 2 * shadowScale, baseRy * 2 * shadowScale)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -pinLift),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.widthIn(max = 220.dp)
            ) {
                if (address == "...") {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AppPurple
                        )
                        Text(
                            "Buscando endereço...",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = address,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = AppPurple,
                modifier = Modifier.size(50.dp)
            )
        }
    }
}

// ── Driver approaching pulse ─────────────────────────────────────────────────

@Composable
private fun DriverApproachingPulse(screenX: Float, screenY: Float) {
    val infinite = rememberInfiniteTransition(label = "driver_pulse")
    val s1 by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing), RepeatMode.Restart), label = "s1")
    val a1 by infinite.animateFloat(0.65f, 0f, infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing), RepeatMode.Restart), label = "a1")
    val s2 by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, delayMillis = 530, easing = LinearOutSlowInEasing), RepeatMode.Restart), label = "s2")
    val a2 by infinite.animateFloat(0.65f, 0f, infiniteRepeatable(tween(1600, delayMillis = 530, easing = LinearOutSlowInEasing), RepeatMode.Restart), label = "a2")
    val s3 by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, delayMillis = 1060, easing = LinearOutSlowInEasing), RepeatMode.Restart), label = "s3")
    val a3 by infinite.animateFloat(0.65f, 0f, infiniteRepeatable(tween(1600, delayMillis = 1060, easing = LinearOutSlowInEasing), RepeatMode.Restart), label = "a3")
    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxR = 75.dp.toPx()
        val center = Offset(screenX, screenY)
        drawCircle(color = AppAmberOrange.copy(alpha = a1), radius = maxR * s1, center = center)
        drawCircle(color = AppAmberOrange.copy(alpha = a2), radius = maxR * s2, center = center)
        drawCircle(color = AppAmberOrange.copy(alpha = a3), radius = maxR * s3, center = center)
    }
}

// ── Custom bitmap markers ─────────────────────────────────────────────────────

private fun createCircleMarker(
    context: android.content.Context,
    fillColor: Color,
    sizeDp: Int
): com.google.android.gms.maps.model.BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val px = (sizeDp * density).toInt()
    val bmp = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.color = fillColor.toArgb()
    canvas.drawCircle(px / 2f, px / 2f, px / 2f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = px * 0.14f
    canvas.drawCircle(px / 2f, px / 2f, px / 2f * 0.68f, paint)
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawCircle(px / 2f, px / 2f, px * 0.13f, paint)
    return BitmapDescriptorFactory.fromBitmap(bmp)
}

private fun createPinMarker(
    context: android.content.Context,
    fillColor: Color
): com.google.android.gms.maps.model.BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val w = (28 * density).toInt()
    val r = w / 2f
    val tailH = (20 * density)
    val h = (w + tailH).toInt()
    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    // shadow
    paint.color = android.graphics.Color.argb(50, 0, 0, 0)
    canvas.drawCircle(w / 2f + 1f, r + 1f, r * 0.9f, paint)
    // head circle
    paint.color = fillColor.toArgb()
    canvas.drawCircle(w / 2f, r, r, paint)
    // tail triangle
    val path = android.graphics.Path()
    path.moveTo(w / 2f - r * 0.52f, r + r * 0.48f)
    path.lineTo(w / 2f + r * 0.52f, r + r * 0.48f)
    path.lineTo(w / 2f, h.toFloat() - 1f)
    path.close()
    canvas.drawPath(path, paint)
    // white center dot
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(w / 2f, r, r * 0.38f, paint)
    return BitmapDescriptorFactory.fromBitmap(bmp)
}

// ─── Coupon field composable ──────────────────────────────────────────────────

@Composable
private fun CouponField(
    code: String,
    onCodeChange: (String) -> Unit,
    applied: CouponValidationResponse?,
    loading: Boolean,
    error: String?,
    onApply: () -> Unit,
    onRemove: () -> Unit
) {
    if (applied != null) {
        // Applied state: green success row
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppGreenBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = AppGreen, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cupom aplicado: ${applied.code}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AppGreenDark)
                    val discountText = when {
                        applied.discountPercent != null -> "${applied.discountPercent}% de desconto"
                        applied.discountCents != null -> "R$ ${"%.2f".format(applied.discountCents / 100.0).replace(".", ",")} de desconto"
                        else -> "Desconto aplicado"
                    }
                    Text(discountText, style = MaterialTheme.typography.labelSmall, color = AppGreen)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, tint = AppGreenDark, modifier = Modifier.size(16.dp))
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                placeholder = { Text("Código do cupom", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) } },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppPurple,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                enabled = !loading,
                leadingIcon = { Icon(Icons.Default.LocalOffer, null, modifier = Modifier.size(18.dp)) }
            )
            Button(
                onClick = onApply,
                enabled = code.isNotBlank() && !loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPurple),
                modifier = Modifier.height(56.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Aplicar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun createDriverMarker(
    context: android.content.Context
): com.google.android.gms.maps.model.BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val px = (40 * density).toInt()
    val bmp = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.color = AppAmberOrange.toArgb()
    canvas.drawCircle(px / 2f, px / 2f, px / 2f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = px * 0.10f
    canvas.drawCircle(px / 2f, px / 2f, px / 2f * 0.80f, paint)
    paint.style = android.graphics.Paint.Style.FILL
    val bW = px * 0.46f; val bH = px * 0.26f; val cx = px / 2f; val cy = px / 2f
    val rect = android.graphics.RectF(cx - bW / 2, cy - bH / 2, cx + bW / 2, cy + bH / 2)
    canvas.drawRoundRect(rect, bH * 0.35f, bH * 0.35f, paint)
    val rW = bW * 0.58f; val rH = bH * 0.60f
    val roofRect = android.graphics.RectF(cx - rW / 2, cy - bH / 2 - rH + 2f, cx + rW / 2, cy - bH / 2 + 2f)
    canvas.drawRoundRect(roofRect, rH * 0.4f, rH * 0.4f, paint)
    paint.color = AppAmberOrange.toArgb()
    val wR = bH * 0.28f
    canvas.drawCircle(cx - bW * 0.30f, cy + bH / 2, wR, paint)
    canvas.drawCircle(cx + bW * 0.30f, cy + bH / 2, wR, paint)
    return BitmapDescriptorFactory.fromBitmap(bmp)
}
