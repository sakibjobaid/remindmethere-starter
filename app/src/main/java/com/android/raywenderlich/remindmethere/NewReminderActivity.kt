/*
 * Copyright (c) 2018 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.android.raywenderlich.remindmethere

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import android.widget.SeekBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_new_reminder.*
import kotlin.math.roundToInt


class NewReminderActivity : BaseActivity(), OnMapReadyCallback {

  private lateinit var map: GoogleMap
  private val tag= "sima"

  private var reminder = Reminder(latLng = null, radius = null, message = null)

  private val radiusBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
      updateRadiusWithProgress(progress)

      showReminderUpdate()
    }
  }

  private fun updateRadiusWithProgress(progress: Int) {
    Log.d(tag,"updateRadiusWithProgress")

    val radius = getRadius(progress)
    reminder.radius = radius
    radiusDescription.text = getString(R.string.radius_description, radius.roundToInt().toString())
  }

  companion object {
    private const val EXTRA_LAT_LNG = "EXTRA_LAT_LNG"
    private const val EXTRA_ZOOM = "EXTRA_ZOOM"

    fun newIntent(context: Context, latLng: LatLng, zoom: Float): Intent {


      Log.d("sima","Remainderactivity");
      val intent = Intent(context, NewReminderActivity::class.java)
      intent
          .putExtra(EXTRA_LAT_LNG, latLng)
          .putExtra(EXTRA_ZOOM, zoom)
      return intent
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_new_reminder)
    Log.d(tag,"RemainderactivityOncreate");

    val mapFragment = supportFragmentManager
        .findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)

    instructionTitle.visibility = View.GONE
    instructionSubtitle.visibility = View.GONE
    radiusBar.visibility = View.GONE
    radiusDescription.visibility = View.GONE
    message.visibility = View.GONE

    // still dont know the use of this line
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
  }

  override fun onSupportNavigateUp(): Boolean {
    Log.d(tag,"onSupportNavigateUp")

    finish()
    return true
  }

  override fun onMapReady(googleMap: GoogleMap) {
    map = googleMap
    map.uiSettings.isMapToolbarEnabled = false

    centerCamera()

    showConfigureLocationStep()
  }

  private fun centerCamera() {
    Log.d(tag,"centerCamera2")

    val latLng = intent.extras.get(EXTRA_LAT_LNG) as LatLng
    val zoom = intent.extras.get(EXTRA_ZOOM) as Float
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
  }

  private fun showConfigureLocationStep() {

    Log.d(tag,"showConfigureLocationStep");

    marker.visibility = View.VISIBLE
    instructionTitle.visibility = View.VISIBLE
    instructionSubtitle.visibility = View.VISIBLE
    radiusBar.visibility = View.GONE
    radiusDescription.visibility = View.GONE
    message.visibility = View.GONE
    instructionTitle.text = getString(R.string.instruction_where_description)
    next.setOnClickListener {
      reminder.latLng = map.cameraPosition.target
      showConfigureRadiusStep()
    }
    showReminderUpdate()
  }

  private fun showConfigureRadiusStep() {
    Log.d("sima","showConfigureRadiusStep");

    marker.visibility = View.GONE
    instructionTitle.visibility = View.VISIBLE
    instructionSubtitle.visibility = View.GONE
    radiusBar.visibility = View.VISIBLE
    radiusDescription.visibility = View.VISIBLE
    message.visibility = View.GONE
    instructionTitle.text = getString(R.string.instruction_radius_description)
    next.setOnClickListener {
      showConfigureMessageStep()
    }
    radiusBar.setOnSeekBarChangeListener(radiusBarChangeListener)
    updateRadiusWithProgress(radiusBar.progress)
    map.animateCamera(CameraUpdateFactory.zoomTo(15f))
    /// without this line change of radius and geofence along with marker will not be shown
    showReminderUpdate()
  }

  private fun getRadius(progress: Int) = 100 + (2 * progress.toDouble() + 1) * 100

  private fun showConfigureMessageStep() {
    Log.d("sima","showConfigureMessageStep");

    marker.visibility = View.GONE
    instructionTitle.visibility = View.VISIBLE
    instructionSubtitle.visibility = View.GONE
    radiusBar.visibility = View.GONE
    radiusDescription.visibility = View.GONE
    message.visibility = View.VISIBLE
    instructionTitle.text = getString(R.string.instruction_message_description)
    next.setOnClickListener {
      hideKeyboard(this, message)

       ///message.text is equivalent to message.getText()
      reminder.message = message.text.toString()

      if (reminder.message.isNullOrEmpty()) {
        message.error = getString(R.string.error_required)
      } else {
        addReminder(reminder)
      }
    }
    message.requestFocusWithKeyboard()

    showReminderUpdate()
  }

  private fun addReminder(reminder: Reminder) {

    Log.d("sima","addRemainder")
    getRepository().add(reminder,
        success = {
          setResult(Activity.RESULT_OK)
          finish()
        },
        failure = {
          Snackbar.make(main, it, Snackbar.LENGTH_LONG).show()
        })
  }

  private fun showReminderUpdate() {
    Log.d(tag,"showReminderUpdate");

    map.clear()
    showReminderInMap(this, map, reminder)
  }
}
