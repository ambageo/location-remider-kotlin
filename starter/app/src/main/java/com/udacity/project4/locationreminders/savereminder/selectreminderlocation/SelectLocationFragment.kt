package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.createTitle
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private val TAG = SelectLocationFragment::class.java.simpleName
    private val REQUEST_PERMISSION_LOCATION = 1


    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var selectedPoi: PointOfInterest

    lateinit var geofencingClient: GeofencingClient

    companion object {
        const val ACTION_GEOFENCE_EVENT = "GEOFENCE_EVENT"
        internal const val GEOFENCE_RADIUS_METERS = 50f
        val GEOFENCE_EXPIRATION_MILLISECONDS: Long = TimeUnit.HOURS.toMillis(2)
    }

    /*private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }*/

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

//        TODO: add the map setup implementation //DONE
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment =   childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveLocation.setOnClickListener {
            // TODO: call this function after the user confirms on the selected location //DONE
            onLocationSelected()
        }

        return binding.root
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence //DONE
        _viewModel.selectedPOI.value = selectedPoi
        _viewModel.latitude.value = selectedPoi.latLng.latitude
        _viewModel.longitude.value = selectedPoi.latLng.longitude
        _viewModel.reminderSelectedLocationStr.value = selectedPoi.name
        parentFragmentManager.popBackStack()


    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection// DONE
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        Log.d(TAG, "Map is ready")

        enableLocation()
        setMapLongClick(map)
        // TODO: add style to the map //DONE
        setMapStyle(map)
    }

    private fun enableLocation() {

        if ( isPermissionGranted()) {
            map.setMyLocationEnabled(true)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location ->
                    run {
                        // Got last known location. In some rare situations this can be null.
                        val lat = location.latitude
                        val long = location.longitude
                        val currentPosition = LatLng(lat, long)
                        // TODO: zoom to the user location after taking his permission //DONE
                        val zoom = 15f
                        map.addMarker(MarkerOptions().position(currentPosition).title(getString(R.string.you_are_here)))
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, zoom))

                        // TODO: put a marker to location that the user selected //DONE
                        setPoiClick(map)
                    }
                }
        } else {
          requestForegroundAndBackgroundLocationPermissions()
        }
    }

    /*
    * MAP STYLING FUNCTIONS
    * */
    private fun setPoiClick(map: GoogleMap){
        map.setOnPoiClickListener { poi ->
            selectedPoi = poi
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker!!.showInfoWindow()
            binding.saveLocation.visibility = View.VISIBLE
        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            // TODO: In the future , maybe add code to let the user add a title for it
            val title = createTitle(latLng)
            selectedPoi = PointOfInterest(latLng, title, title)
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            binding.saveLocation.visibility = View.VISIBLE
        }
    }

    private fun setMapStyle(map: GoogleMap){
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
            if(!success){
                Log.e(TAG, "Style parsing failed")
            }
        } catch (e:Resources.NotFoundException){
            Log.e(TAG, "Unable to find style. Error: ", e)
        }
    }

    /*
    * LOCATION PERMISSION FUNCTIONS
    * */
    @TargetApi(29)
    private fun isPermissionGranted(): Boolean {
        val context = requireContext()
        val foregroundLocationApproved =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val backgroundPermissionApproved = if(runningQOrLater){
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        Log.d("ggg", "ForegroundPermission: $foregroundLocationApproved , backgroundPermission: $backgroundPermissionApproved")
        return foregroundLocationApproved && backgroundPermissionApproved

    }

    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {

        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            resultCode
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
       /* if( requestCode == REQUEST_PERMISSION_LOCATION ) {
            if( grantResults.isNotEmpty() && (grantResults[0] ==PackageManager.PERMISSION_GRANTED)){
                enableLocation()
            }
        }*/
        if (grantResults.isEmpty() ||
            grantResults[0] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)) {
            Snackbar.make(
                binding.saveLocation,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                // Create an action that opens the settings for the specific app
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        }
    }
}
