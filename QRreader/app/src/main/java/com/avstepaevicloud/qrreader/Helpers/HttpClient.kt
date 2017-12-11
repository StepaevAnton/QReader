package com.avstepaevicloud.qrreader.Helpers

import android.app.Application
import android.content.Context
import android.text.TextUtils
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.JsonRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URL
import java.nio.charset.Charset

/**
 * Класс для работы с запросами
  */
class HttpClient {
    companion object {

        /**
         * Введенный пин-код, не совсем адекватное местоположение, но раз используется он только здесь, то пусть тут и лежит
         */
        var pinCode: String? = ""

        private val GET_EVENTS_LIST_URL = "http://tkt.ac/api/?method=list"
        private val GET_TICKETS_URL = "http://tkt.ac/api/?method=getTickets"
        private val POST_TICKET_URL = "http://tkt.ac/api/?method=updateTickets"

        private var token: String? = ""

        /**
         * Получить список событий
          */
        fun getEventsList(): String {
            val url = GET_EVENTS_LIST_URL + "&pin=$pinCode"

            return performRequest(url)
        }

        /**
         * Получить список билетов на событие
         */
        fun getTickets(eventId: String) : String {
            val url = GET_TICKETS_URL + "&event=$eventId&pin=$pinCode"

            return performRequest(url)
        }

        // Общий метод запросов
        private fun performRequest(url: String): String {
//            val jsonObjReq = object : JsonObjectRequest(Method.GET, url, JSONObject(),
//                    Response.Listener<JSONObject> { response ->
//                        //completionHandler(response)
//                    },
//                    Response.ErrorListener { error ->
//                        //completionHandler(null)
//                    }) {
//                override fun getHeaders(): Map<String, String> {
//                    val headers = HashMap<String, String>()
//                    headers.put("Content-Type", "application/json")
//                    return headers
//                }
//            }
//
//            VolleyManager.getInstance(context).addToRequestQueue(jsonObjReq)

            return URL(url).readText()
        }

        fun postTickets(context: Context, eventId: String, params: JSONArray, completionHandler: (response: String?) -> Unit){

        //var token = ""
        //CookieHandler.setDefault(CookieManager())

            // TODO debug
            CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))


            val url = POST_TICKET_URL + "&event=$eventId&pin=$pinCode"

//            val jsonObjReq = object : JsonArrayRequest(Method.POST, url, params,
//                    Response.Listener<JSONArray> { response ->
//                        completionHandler(response)
//                    },
//                    Response.ErrorListener { error ->
//                        completionHandler(null)
//                    }) {
//                override fun getHeaders(): Map<String, String> {
//                    val headers = HashMap<String, String>()
//                    headers.put("Content-Type", "application/json")
//                    return headers
//                }
//            }

            val jsonObjReq = object : JsonRequest<String>(Method.POST, url, params.toString(),
                    Response.Listener<String> { response ->
                        completionHandler(response)
                    },
                    Response.ErrorListener { error ->
                        completionHandler(null)
                    }) {
                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    headers.put("Content-Type", "application/json")
                    //headers.put("XSRF-TOKEN", if (token.isNullOrEmpty()) "" else token!!)
                    return headers
                }

                override fun parseNetworkResponse(response: NetworkResponse?): Response<String> {
                    var tmp = response?.data?.toString(Charset.defaultCharset())

//                    var cookie = response?.headers?.getValue("Set-Cookie")
//                    token = cookie!!.replace("XSRF-TOKEN=", "")//.replace("; path=/", "")
//                    postTickets(context, eventId,params, completionHandler)

                    //"Set-Cookie" -> "XSRF-TOKEN=85258b59f9fca7a689dc6596b71b18c989632d10; path=/"
                    return Response.success(tmp, null)

                }
            }

            VolleyManager.getInstance(context).addToRequestQueue(jsonObjReq)
        }
    }
}

/**
 * Менеджер работы с Volley
 */
class VolleyManager private constructor(val context: Context) {

    companion object : SingletonHolder<VolleyManager, Context>({ context -> VolleyManager(context) }){
        private val TAG = VolleyManager::class.java.simpleName

    }

//    override fun onCreate() {
//        super.onCreate()
//        //instance = this
//    }

    val requestQueue: RequestQueue? = null
        get() {
            if (field == null) {
                return Volley.newRequestQueue(context.applicationContext)
            }
            return field
        }

    fun <T> addToRequestQueue(request: Request<T>, tag: String) {
        request.tag = if (TextUtils.isEmpty(tag)) TAG else tag
        requestQueue?.add(request)
    }

    fun <T> addToRequestQueue(request: Request<T>) {
        request.tag = TAG
        requestQueue?.add(request)
        requestQueue?.start()
    }

    fun cancelPendingRequests(tag: Any) {
        if (requestQueue != null) {
            requestQueue!!.cancelAll(tag)
        }
    }

//    companion object {
////        @get:Synchronized var instance: BackendVolley? = null
////            private set
//    }
}
