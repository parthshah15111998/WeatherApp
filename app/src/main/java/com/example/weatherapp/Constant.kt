package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constant {

    const val APP_ID: String = "4bb68cf068530653d14dcb245d7925a0"
    const val BASE_URL: String = "https://api.openweathermap.org/data/"
    const val METRIC_UNIT: String = "metric"
    const val PREFERENCE_NAME="WeatherAppPreference"
    const val WEATHER_RESPONSE_DATA="weather_response_data"


    fun isNetworkAvailable(context: Context):Boolean{
        val connectivityManage = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val network=connectivityManage.activeNetwork ?: return false
            val activityNetwork=connectivityManage.getNetworkCapabilities(network) ?: return false

            return when{
                activityNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)-> true
                activityNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->true
                activityNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)->true
                else->false
            }


        }else{
            val networkInfo = connectivityManage.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }

    }

}