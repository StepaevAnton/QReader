package com.avstepaevicloud.qrreader.Helpers

import android.content.Context
import android.net.ConnectivityManager
import java.net.InetAddress
import kotlinx.coroutines.experimental.async


/**
 * Created by StepaevAV on 09.11.17.
 */

class NetworkManager {
    companion object {

        /**
         * Проверить подключение к интернету
         */
         fun isNetworkConnected() : Boolean {

                try{
                    val address = InetAddress.getByName("google.com")
                    return !address.equals("")
                }
                catch (e: Exception){
                    return false;
                }
            
// Старая версия проверки. Невалидна, так как проверяет подключение к сети, а не к интернету
//            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//            val networkInfo = connectivityManager.activeNetworkInfo
//            var IsConnectedToNetwork = networkInfo != null && networkInfo.isConnected

        }
    }
}