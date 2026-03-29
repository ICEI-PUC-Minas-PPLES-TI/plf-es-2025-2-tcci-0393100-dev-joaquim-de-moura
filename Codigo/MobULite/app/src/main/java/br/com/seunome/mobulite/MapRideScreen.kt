package br.com.seunome.mobulite

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import android.os.Looper
import br.com.seunome.mobulite.data.remote.fetchRoutePoints
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority
import com.google.maps.android.compose.Polyline
import com.google.android.gms.maps.model.LatLngBounds
import br.com.seunome.mobulite.data.remote.getAddressFromLatLng

@SuppressLint("MissingPermission")
@Composable
fun MapRideScreen(
    modifier: Modifier = Modifier,
    onPointsChanged: (origin: LatLng?, dest: LatLng?, originText: String, destText: String) -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Places client
    val placesClient: PlacesClient = remember {
        Places.createClient(context)
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var origin by remember { mutableStateOf<LatLng?>(null) }
    var destination by remember { mutableStateOf<LatLng?>(null) }


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

    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var routeError by remember { mutableStateOf<String?>(null) }

    // Autocomplete UI state
    var query by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var placesError by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var originAddress by remember { mutableStateOf("") }
    var destinationAddress by remember { mutableStateOf("") }
    val cameraPositionState = rememberCameraPositionState()
    var editingOrigin by remember { mutableStateOf(false) }
    var editingDestination by remember { mutableStateOf(true) } // destino é o foco principal
    var originQuery by remember { mutableStateOf("") }
    var destinationQuery by remember { mutableStateOf("") }

    // Pega localização atual
    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            onDispose { }
        } else {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .build()

            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

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
            .setCountries("BR") // restringe Brasil
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
        query = prediction.getFullText(null).toString()

        val placeId = prediction.placeId
        val fields = listOf(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS)
        val fetchReq = FetchPlaceRequest.builder(placeId, fields).build()

        placesClient.fetchPlace(fetchReq)
            .addOnSuccessListener { res ->
                val latLng = res.place.latLng
                if (latLng != null) {
                    if (editingOrigin) {
                        origin = latLng
                        originQuery = prediction.getFullText(null).toString()
                        originAddress = originQuery
                        editingOrigin = false
                    } else {
                        destination = latLng
                        destinationQuery = prediction.getFullText(null).toString()
                        destinationAddress = destinationQuery
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

    Column(modifier) {

        if (!hasLocationPermission) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Precisamos da sua localização para definir a origem.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }) { Text("Permitir localização") }
            }
            return@Column
        }

        // 🔎 Campo de busca + sugestões
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            // Painel estilo app de transporte
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Para onde vamos?", style = MaterialTheme.typography.titleMedium)

                    // ORIGEM
                    OutlinedTextField(
                        value = originQuery,
                        onValueChange = {
                            originQuery = it
                            editingOrigin = true
                            editingDestination = false
                            searchPlaces(it)
                        },
                        label = { Text("Origem") },
                        placeholder = { Text("Minha localização") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // DESTINO
                    OutlinedTextField(
                        value = destinationQuery,
                        onValueChange = {
                            destinationQuery = it
                            editingDestination = true
                            editingOrigin = false
                            searchPlaces(it)
                        },
                        label = { Text("Destino") },
                        placeholder = { Text("Digite o destino") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )



                    // Sugestões
                    if (searching) {
                        Text("Buscando...", style = MaterialTheme.typography.bodySmall)
                    }

                    placesError?.let {
                        Text("Erro: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    if (predictions.isNotEmpty()) {
                        Divider()
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                        ) {
                            items(predictions) { p ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { choosePrediction(p) }
                                        .padding(vertical = 10.dp)
                                ) {
                                    Text(p.getPrimaryText(null).toString())
                                    val secondary = p.getSecondaryText(null).toString()
                                    if (secondary.isNotBlank()) Text(secondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(origin, destination) {
            val o = origin
            val d = destination

            // se ainda não tem os 2 pontos, limpa a rota
            if (o == null || d == null) {
                routePoints = emptyList()
                routeError = null
                return@LaunchedEffect
            }

            routeError = null
            routePoints = try {
                fetchRoutePoints(
                    apiKey = "AIzaSyDkk3z1jiuVuXYqKoJJExNl6i-7acH4ujM",
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
                    apiKey = "AIzaSyDkk3z1jiuVuXYqKoJJExNl6i-7acH4ujM",
                    lat = it.latitude,
                    lng = it.longitude
                )
            }
        }
        LaunchedEffect(origin, destination, originAddress, destinationAddress) {
            onPointsChanged(origin, destination, originAddress, destinationAddress)
        }

        // 🗺️ MAPA
        GoogleMap(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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
                        apiKey = "SUA_API_KEY_AQUI",
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
                Marker(state = MarkerState(position = it), title = "Origem")
            }
            destination?.let {
                Marker(state = MarkerState(position = it), title = "Destino")
            }
        }

    }
}