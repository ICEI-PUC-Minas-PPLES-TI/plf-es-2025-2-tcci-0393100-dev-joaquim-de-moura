package br.com.seunome.mobulite

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.com.seunome.mobulite.data.remote.CreateRideRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.data.remote.fetchRoutePoints
import br.com.seunome.mobulite.data.remote.getAddressFromLatLng
import br.com.seunome.mobulite.ui.PassengerRideUiState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import br.com.seunome.mobulite.data.remote.UpdateStatusRequest
import br.com.seunome.mobulite.ui.PassengerRideStatusBanner

private const val MAPS_API_KEY = "AIzaSyDkk3z1jiuVuXYqKoJJExNl6i-7acH4ujM"

@SuppressLint("MissingPermission")
@Composable
fun MapRideScreen(
    modifier: Modifier = Modifier,
    onPointsChanged: (origin: LatLng?, dest: LatLng?, originText: String, destText: String) -> Unit,
    onRideStateChanged: (
        uiState: PassengerRideUiState,
        rideId: String?,
        driverName: String?,
        errorMessage: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val placesClient: PlacesClient = remember {
        Places.createClient(context)
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var origin by remember { mutableStateOf<LatLng?>(null) }
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

    var rideUiState by remember { mutableStateOf(PassengerRideUiState.IDLE) }
    var currentRideId by remember { mutableStateOf<String?>(null) }
    var rideError by remember { mutableStateOf<String?>(null) }
    var acceptedDriverName by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState()
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                origin = LatLng(loc.latitude, loc.longitude)
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
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000L
            )
                .setMinUpdateIntervalMillis(1000L)
                .build()

            fusedClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            onDispose {
                fusedClient.removeLocationUpdates(locationCallback)
            }
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
        val fields = listOf(
            Place.Field.LAT_LNG,
            Place.Field.NAME,
            Place.Field.ADDRESS
        )
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
                    }

                    scope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                            durationMs = 600
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                placesError = e.message
            }
    }

    LaunchedEffect(origin, destination) {
        val o = origin
        val d = destination

        if (o == null || d == null) {
            routePoints = emptyList()
            routeError = null
            return@LaunchedEffect
        }

        routeError = null
        routePoints = try {
            fetchRoutePoints(
                apiKey = MAPS_API_KEY,
                origin = o,
                destination = d
            )
        } catch (e: Exception) {
            routeError = e.message
            emptyList()
        }
    }

    var didCenterOnce by remember { mutableStateOf(false) }

    LaunchedEffect(origin) {
        val o = origin ?: return@LaunchedEffect

        if (!didCenterOnce) {
            didCenterOnce = true
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(o, 16f),
                durationMs = 700
            )
        }
    }

    LaunchedEffect(routePoints) {
        if (routePoints.size < 2) return@LaunchedEffect

        val boundsBuilder = LatLngBounds.Builder()
        routePoints.forEach { boundsBuilder.include(it) }
        val bounds = boundsBuilder.build()

        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngBounds(bounds, 120),
            durationMs = 700
        )
    }

    LaunchedEffect(originAddress) {
        if (!editingOrigin && originAddress.isNotBlank()) {
            originQuery = originAddress
        }
    }

    LaunchedEffect(destinationAddress) {
        if (!editingDestination && destinationAddress.isNotBlank()) {
            destinationQuery = destinationAddress
        }
    }

    LaunchedEffect(origin) {
        origin?.let {
            originAddress = getAddressFromLatLng(
                apiKey = MAPS_API_KEY,
                lat = it.latitude,
                lng = it.longitude
            )
        }
    }

    LaunchedEffect(origin, destination, originAddress, destinationAddress) {
        onPointsChanged(origin, destination, originAddress, destinationAddress)
    }

    LaunchedEffect(currentRideId) {
        val rideId = currentRideId ?: return@LaunchedEffect

        while (true) {
            try {
                val ride = RetrofitClient.api.getRideById(rideId)

                when (ride.status) {
                    "PENDING_DRIVER" -> {
                        rideUiState = PassengerRideUiState.SEARCHING_DRIVER
                    }

                    "ACCEPTED" -> {
                        rideUiState = PassengerRideUiState.DRIVER_ACCEPTED
                        acceptedDriverName = ride.driver?.name
                    }

                    "IN_PROGRESS" -> {
                        rideUiState = PassengerRideUiState.IN_PROGRESS
                    }

                    "FINISHED" -> {
                        rideUiState = PassengerRideUiState.FINISHED
                        break
                    }

                    "CANCELED" -> {
                        rideUiState = PassengerRideUiState.CANCELED
                        break
                    }

                    else -> {
                        rideUiState = PassengerRideUiState.SEARCHING_DRIVER
                    }
                }
            } catch (e: Exception) {
                rideError = e.message ?: "Erro ao atualizar corrida"
            }

            delay(3000)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (!hasLocationPermission) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Precisamos da sua localização para definir a origem.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text("Permitir localização")
                }
            }
            return@Box
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true
            ),
            onMapClick = { latLng ->
                destination = latLng

                predictions = emptyList()
                placesError = null
                searching = false
                editingDestination = false
                editingOrigin = false

                scope.launch {
                    destinationQuery = "Carregando endereço..."
                    destinationAddress = ""

                    val addr = getAddressFromLatLng(
                        apiKey = MAPS_API_KEY,
                        lat = latLng.latitude,
                        lng = latLng.longitude
                    )

                    val finalAddr =
                        if (addr.isBlank()) "${latLng.latitude}, ${latLng.longitude}"
                        else addr

                    destinationAddress = finalAddr
                    destinationQuery = finalAddr
                }
            }
        ) {
            if (routePoints.isNotEmpty()) {
                Polyline(
                    points = routePoints,
                    width = 12f
                )
            }

            origin?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Origem"
                )
            }

            destination?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Destino"
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {

                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Para onde vamos?",
                                style = MaterialTheme.typography.titleSmall
                            )

                            OutlinedTextField(
                                value = originQuery,
                                onValueChange = {
                                    originQuery = it
                                    editingOrigin = true
                                    editingDestination = false
                                    searchPlaces(it)
                                },
                                label = { Text("Origem") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
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
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            if (predictions.isNotEmpty()) {
                                HorizontalDivider()

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 160.dp)
                                ) {
                                    items(predictions) { p ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { choosePrediction(p) }
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Text(p.getPrimaryText(null).toString())
                                            val secondary = p.getSecondaryText(null).toString()
                                            if (secondary.isNotBlank()) {
                                                Text(
                                                    secondary,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }

                            val hasActiveRideRequest =
                                rideUiState == PassengerRideUiState.REQUESTING ||
                                        rideUiState == PassengerRideUiState.SEARCHING_DRIVER

                            if (origin != null && destination != null) {
                                Button(
                                    onClick = {
                                        if (hasActiveRideRequest) {
                                            scope.launch {
                                                try {
                                                    val rideId = currentRideId
                                                    if (!rideId.isNullOrBlank()) {
                                                        RetrofitClient.api.updateRideStatus(
                                                            rideId = rideId,
                                                            body = UpdateStatusRequest(status = "CANCELED")
                                                        )
                                                    }

                                                    rideUiState = PassengerRideUiState.IDLE
                                                    currentRideId = null
                                                    acceptedDriverName = null
                                                    rideError = null

                                                    onRideStateChanged(
                                                        rideUiState,
                                                        currentRideId,
                                                        acceptedDriverName,
                                                        rideError
                                                    )
                                                } catch (e: Exception) {
                                                    rideError = e.message ?: "Erro ao cancelar corrida"
                                                    onRideStateChanged(
                                                        rideUiState,
                                                        currentRideId,
                                                        acceptedDriverName,
                                                        rideError
                                                    )
                                                }
                                            }
                                        } else {
                                            val o = origin ?: return@Button
                                            val d = destination ?: return@Button

                                            scope.launch {
                                                try {
                                                    rideUiState = PassengerRideUiState.REQUESTING
                                                    rideError = null
                                                    onRideStateChanged(
                                                        rideUiState,
                                                        currentRideId,
                                                        acceptedDriverName,
                                                        rideError
                                                    )

                                                    val response = RetrofitClient.api.createRide(
                                                        CreateRideRequest(
                                                            originLat = o.latitude,
                                                            originLng = o.longitude,
                                                            destLat = d.latitude,
                                                            destLng = d.longitude,
                                                            originAddress = originAddress,
                                                            destinationAddress = destinationAddress
                                                        )
                                                    )

                                                    currentRideId = response.id
                                                    rideUiState = PassengerRideUiState.SEARCHING_DRIVER

                                                    onRideStateChanged(
                                                        rideUiState,
                                                        currentRideId,
                                                        acceptedDriverName,
                                                        rideError
                                                    )
                                                } catch (e: Exception) {
                                                    rideError = e.message ?: "Falha ao solicitar corrida"
                                                    rideUiState = PassengerRideUiState.ERROR

                                                    onRideStateChanged(
                                                        rideUiState,
                                                        currentRideId,
                                                        acceptedDriverName,
                                                        rideError
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasActiveRideRequest) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                ) {
                                    Text(
                                        text = when {
                                            rideUiState == PassengerRideUiState.REQUESTING -> "Solicitando..."
                                            hasActiveRideRequest -> "Cancelar corrida"
                                            else -> "Pedir corrida"
                                        },
                                        color = if (hasActiveRideRequest) {
                                            MaterialTheme.colorScheme.onError
                                        } else {
                                            MaterialTheme.colorScheme.onPrimary
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (rideUiState != PassengerRideUiState.IDLE) {
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            PassengerRideStatusBanner(
                                uiState = rideUiState,
                                driverName = acceptedDriverName
                            )
                        }
                    }
                }
            }
        }