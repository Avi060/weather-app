package com.project.weatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var city: String
    val API: String = "70b40caf1fc9475bb3b115715242806"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            getLocation()
        }
    }

    private fun getLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    city = "$latitude,$longitude"
                    weatherTask().execute()
                } else {
                    showError("Could not get access to location")
                }
            }
            .addOnFailureListener {
                showError("Could not get access to location")
            }
    }

    private fun showError(message: String) {
        findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
        findViewById<TextView>(R.id.error).text = message
        findViewById<TextView>(R.id.error).visibility = View.VISIBLE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getLocation()
                } else {
                    showError("Location permission denied")
                }
                return
            }
        }
    }

    inner class weatherTask() : AsyncTask<String, Void, String?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<TextView>(R.id.error).visibility = View.GONE
        }

        override fun doInBackground(vararg p0: String?): String? {
            var response: String?
            try {
                response = URL("https://api.weatherapi.com/v1/forecast.json?q=$city&days=1&key=$API").readText(Charsets.UTF_8)
            } catch (e: Exception) {
                response = null
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                val jsonObj = JSONObject(result.toString())

                val location = jsonObj.getJSONObject("location")
                val current = jsonObj.getJSONObject("current")
                val updatedAt: Long = current.getLong("last_updated_epoch")
                val updatedAtText = "Updated at: " + SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(updatedAt * 1000))
                val temp = current.getDouble("temp_c").toString() + "°C"
                val humidity = current.getDouble("humidity").toString() + "%"
                val windSpeed = current.getDouble("wind_kph").toString() + " km/h"
                val status = current.getJSONObject("condition").getString("text")
                val address = location.getString("name") + ", " + location.getString("region") + ", " + location.getString("country")

                val forecast = jsonObj.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0)
                val astro = forecast.getJSONObject("astro")
                val day = forecast.getJSONObject("day")
                val sunrise = astro.getString("sunrise")
                val sunset = astro.getString("sunset")
                val minTemp = day.getDouble("mintemp_c").toString() + "°C"
                val maxTemp = day.getDouble("maxtemp_c").toString() + "°C"

                val imgSrc = current.getJSONObject("condition").getString("icon")

                findViewById<TextView>(R.id.address).text = address
                findViewById<TextView>(R.id.updated_at).text = updatedAtText
                findViewById<TextView>(R.id.status).text = status
                Glide.with(this@MainActivity).load("https:$imgSrc").into(findViewById(R.id.icon))
                findViewById<TextView>(R.id.temperature).text = temp
                findViewById<TextView>(R.id.wind).text = windSpeed
                findViewById<TextView>(R.id.humidity).text = humidity
                findViewById<TextView>(R.id.sunrise).text = sunrise
                findViewById<TextView>(R.id.sunset).text = sunset
                findViewById<TextView>(R.id.minTemp).text = "Min Temp: $minTemp"
                findViewById<TextView>(R.id.maxTemp).text = "Max Temp: $maxTemp"

                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
            } catch (e: Exception) {
                showError("Error retrieving data")
            }
        }
    }
}
