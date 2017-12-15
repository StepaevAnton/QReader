package com.avstepaevicloud.qrreader.Helpers

import android.content.Context
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.net.*
import java.nio.charset.Charset
import java.text.SimpleDateFormat

/**
 * Класс для работы с запросами
 */
class HttpClient {
    companion object {

        /**
         * Введенный пин-код, не совсем адекватное местоположение, но раз используется он только здесь, то пусть тут и лежит
         */
        var pinCode: String? = ""

        /**
         * Формат даты для  API
         */
        private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")

        /**
         * Методы API (аргументы формируются внутри функций)
         */
        private val GET_EVENTS_LIST_URL = "http://tkt.ac/api/?method=list"
        private val GET_TICKETS_URL = "http://tkt.ac/api/?method=getTickets"
        private val POST_TICKET_URL = "http://tkt.ac/api/?method=updateTickets"

        //private var token: String? = ""

        /**
         * Получить список событий
         */
        fun getEventsList(): String {
            val url = "$GET_EVENTS_LIST_URL&pin=$pinCode"

            return performRequest(url)
        }

        /**
         * Получить список билетов на событие
         */
        fun getTickets(eventId: String): String {
            val url = "$GET_TICKETS_URL&event=$eventId&pin=$pinCode"

            return performRequest(url)
        }

        // Общий метод запросов
        private fun performRequest(url: String): String {
            return URL(url).readText()
        }

        /**
         * Запостить билеты
         */
        fun postTickets(context: Context, eventId: String, ticketInfos: HashSet<TicketInfo>, completionHandler: (response: String?) -> Unit) {

            val params = JSONArray()

            for (ticketInfo in ticketInfos)
            {
                val param = JSONObject()
                param.put("id", ticketInfo.id.toString())
                param.put("time", if (ticketInfo.scanDt != null) dateFormat.format(ticketInfo.scanDt) else "")//SimpleDateFormat.getInstance().format(ticketInfo.scanDt))//"25.11.2017 23:59:59"
                params.put(param)
            }

            if (ticketInfos.count() == 0) return

            val url = "$POST_TICKET_URL&event=$eventId&pin=$pinCode"

            val jsonObjReq = object : JsonRequest<String>(Method.POST, url, params.toString(),
                    Response.Listener<String> { response ->
                        completionHandler(response)
                    },
                    Response.ErrorListener { _ ->
                        completionHandler(null)
                    }) {

                override fun parseNetworkResponse(response: NetworkResponse?): Response<String> {
                    val tmp = response?.data?.toString(Charset.defaultCharset())

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

    companion object : SingletonHolder<VolleyManager, Context>({ context -> VolleyManager(context) }) {
        private val TAG = VolleyManager::class.java.simpleName

    }

    val requestQueue: RequestQueue? = null
        get() {
            if (field == null) {
                return Volley.newRequestQueue(context.applicationContext)
            }
            return field
        }
//
//    fun <T> addToRequestQueue(request: Request<T>, tag: String) {
//        request.tag = if (TextUtils.isEmpty(tag)) TAG else tag
//        requestQueue?.add(request)
//    }

    fun <T> addToRequestQueue(request: Request<T>) {
        request.tag = TAG
        requestQueue?.add(request)
        requestQueue?.start()
    }

//    fun cancelPendingRequests(tag: Any) {
//        if (requestQueue != null) {
//            requestQueue!!.cancelAll(tag)
//        }
//    }

//    companion object {
////        @get:Synchronized var instance: BackendVolley? = null
////            private set
//    }
}
