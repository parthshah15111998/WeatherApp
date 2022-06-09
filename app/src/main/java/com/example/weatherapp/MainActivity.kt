package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.weatherapp.Constant.isNetworkAvailable
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.network.WeatherServices
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.example.weatherapp.models.WeatherResponse
import com.google.android.gms.common.internal.Constants
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient:FusedLocationProviderClient
    private var mProgressDialog:Dialog? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mFusedLocationClient=LocationServices.getFusedLocationProviderClient(this)

        sharedPreferences = getSharedPreferences(Constant.PREFERENCE_NAME,Context.MODE_PRIVATE)

        setupUI()

        if (!isLocationEnabled()){
            Toast.makeText(this,"Your Location turn off",Toast.LENGTH_SHORT).show()

            val intent= Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION).withListener(object :
            MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if(report!!.areAllPermissionsGranted()){
                    requestLocationData()
                }
                if (report.isAnyPermissionPermanentlyDenied){
                    Toast.makeText(this@MainActivity,"You have permission denied",Toast.LENGTH_SHORT).show()
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permission: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                showRationalDialogForPermission()
            }
        }).onSameThread().check()
        }
    }


    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()!!
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            Log.e("Current Latitude", "$latitude")
            val longitude = mLastLocation.longitude
            Log.e("Current Longitude", "$longitude")
            getLocationWeatherDetails(latitude,longitude)
        }
    }

    private fun showRationalDialogForPermission(){
        AlertDialog.Builder(this).setMessage(""+
                "It Look like you turn off permission required"+
                "for this features").setPositiveButton("Go to Setting")
        { _, _ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }.show()
    }

    private fun getLocationWeatherDetails(latitude:Double,longitude:Double){
        if (Constant.isNetworkAvailable(this)) {

            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constant.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service: WeatherServices =
                retrofit.create(WeatherServices::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constant.METRIC_UNIT, Constant.APP_ID
            )

            showCustomProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse> {

                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        hideProgressDialog()
                        val weatherList: WeatherResponse = response.body()!!
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val weatherResponseJsonString=Gson().toJson(weatherList)
                            val editor = sharedPreferences.edit()
                            editor.putString(Constant.WEATHER_RESPONSE_DATA,weatherResponseJsonString)
                            editor.apply()
                            setupUI()
                        }
                        Log.i("Response Result", "$weatherList")
                    } else {
                        val sc = response.code()
                        when (sc) {
                            400 -> Log.e("Error 400", "Bad Request")
                            404 -> Log.e("Error 404", "Not Found")
                            else -> Log.e("Error", "Generic Error")
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Errorrrrr", t.message.toString())
                    hideProgressDialog()
                }
            })
            // END

        } else{
            Toast.makeText(this,"You have not connect the internet",Toast.LENGTH_SHORT).show()
        }
    }


    private fun isLocationEnabled():Boolean{

        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }

    private fun showCustomProgressDialog(){
        mProgressDialog = Dialog(this)

        mProgressDialog!!.setContentView(R.layout.dialog_box)

        mProgressDialog!!.show()
    }

    @SuppressLint("ResourceType")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_item , menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh ->{
                requestLocationData()
                true
            }else -> super.onOptionsItemSelected(item)

        }
    }

    private fun hideProgressDialog(){
        if (mProgressDialog != null){
            mProgressDialog!!.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupUI(){
        val weatherResponseJsonString=sharedPreferences.getString(Constant.WEATHER_RESPONSE_DATA,"")

        if (!weatherResponseJsonString.isNullOrEmpty()){
            val weatherList = Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)

            for (i in weatherList.weather.indices){
                Log.e("Weather Name", weatherList.weather.toString())

                binding.tvMain.text=weatherList.weather[i].main
                binding.tvMainDescription.text= weatherList.weather[i].description
                binding.tvTemp.text=weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())

                binding.tvSunriseTime.text=unixTime(weatherList.sys.sunrise)
                binding.tvSunsetTime.text=unixTime(weatherList.sys.sunset)

                binding.tvHumidity.text=weatherList.main.humidity.toString() + "per cent"
                binding.tvMin.text=weatherList.main.temp_min.toString() + "min"
                binding.tvMax.text=weatherList.main.temp_max.toString() + "max"
                binding.tvSpeed.text=weatherList.wind.speed.toString()
                binding.tvName.text=weatherList.name
                binding.tvCountry.text=weatherList.sys.country


            }

        }

    }

    private fun getUnit(value: String): String? {
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value){
            value ="°F"
        }
        return value
    }

    private fun unixTime(timex:Long):String{
        var date = Date(timex * 1000)
        var sdf = SimpleDateFormat("HH:mm:ss")
        sdf.timeZone= TimeZone.getDefault()
        return sdf.format(date)
    }


}