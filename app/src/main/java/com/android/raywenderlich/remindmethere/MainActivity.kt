package com.android.raywenderlich.remindmethere

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : BaseActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        private const val MY_LOCATION_REQUEST_CODE = 329
        private const val NEW_REMINDER_REQUEST_CODE = 330
        private const val EXTRA_LAT_LNG = "EXTRA_LAT_LNG"


        fun newIntent(context: Context, latLng: LatLng): Intent {
            Log.d("sima","mainactivity new intent")
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(EXTRA_LAT_LNG, latLng)
            return intent
        }
    }

    private var map: GoogleMap? = null
    public val tag = "sima"

    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(tag, "oncreate1");

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        newReminder.visibility = View.GONE
        currentLocation.visibility = View.GONE

        newReminder.setOnClickListener {
            map?.run {
                Log.d(tag, "RemButtonclicked")
                val intent = NewReminderActivity.newIntent(
                        this@MainActivity,
                        cameraPosition.target,
                        cameraPosition.zoom)
                startActivityForResult(intent, NEW_REMINDER_REQUEST_CODE)
            }
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(tag, "1st permission");

            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_LOCATION_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(tag, "onActivityResult")
        if (requestCode == NEW_REMINDER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            showReminders()

            val reminder = getRepository().getLast()
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(reminder?.latLng, 15f))

            Snackbar.make(main, R.string.reminder_added_success, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        Log.d(tag, "onRequestPermission");

        if (requestCode == MY_LOCATION_REQUEST_CODE) {
            onMapAndPermissionReady()
        }
    }


    private fun onMapAndPermissionReady() {
        Log.d(tag, "onMap&PermissionReady");
        if (map != null
                && ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map?.isMyLocationEnabled = true
            currentLocation.visibility = View.VISIBLE
            val bestProvider = locationManager.getBestProvider(Criteria(), false)
            val location = locationManager.getLastKnownLocation(bestProvider)
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

            }
            currentLocation.setOnClickListener {
                val bestProvider = locationManager.getBestProvider(Criteria(), false)
                val location = locationManager.getLastKnownLocation(bestProvider)
                //Log.d(tag,"button is responding0")
                if (location != null) {
                    Log.d(tag, "currentLocation deep ideOnMapAndPermission")
                    val latLng = LatLng(location.latitude, location.longitude)
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
            showReminders()
            centerCamera()
            newReminder.visibility = View.VISIBLE
        }
    }

    private fun centerCamera() {
        Log.d(tag, "centerCamera");
        if (intent.extras != null && intent.extras.containsKey(EXTRA_LAT_LNG)) {
            Log.d(tag, "if centerCamera");
            val latLng = intent.extras.get(EXTRA_LAT_LNG) as LatLng
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    private fun showReminders() {
        Log.d(tag, "showRemainder");
        map?.run {
            clear()
            for (reminder in getRepository().getAll()) {
                showReminderInMap(this@MainActivity, this, reminder)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.run {
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isMapToolbarEnabled = false
            setOnMarkerClickListener(this@MainActivity)
        }

        onMapAndPermissionReady()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val reminder = getRepository().get(marker.tag as String)
        Log.d(tag,"onMarkerClick")

        if (reminder != null) {
            showReminderRemoveAlert(reminder)
        }

        return true
    }

    private fun showReminderRemoveAlert(reminder: Reminder) {

        Log.d(tag,"showReminderRemoveAlert")

        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.run {
            setMessage(getString(R.string.reminder_removal_alert))
            setButton(AlertDialog.BUTTON_POSITIVE,
                    getString(R.string.reminder_removal_alert_positive)) { dialog, _ ->
                removeReminder(reminder)
                dialog.dismiss()
            }
            setButton(AlertDialog.BUTTON_NEGATIVE,
                    getString(R.string.reminder_removal_alert_negative)) { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    private fun removeReminder(reminder: Reminder) {

        Log.d(tag,"remove Reminder")

        getRepository().remove(
                reminder,
                success = {
                    showReminders()
                    Snackbar.make(main, R.string.reminder_removed_success, Snackbar.LENGTH_LONG).show()
                },
                failure = {
                    Snackbar.make(main, it, Snackbar.LENGTH_LONG).show()
                })
    }
}
