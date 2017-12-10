package com.avstepaevicloud.qrreader.Helpers

import java.net.URL

/**
 * Класс для работы с запросами
  */
class HttpClient {
    companion object {

        private val GET_EVENTS_LIST_URL = "http://tkt.ac/api/?method=list&pin="

        // Получить список событий
        fun getEventsList(pinCode: String): String {
            return performRequest(GET_EVENTS_LIST_URL + pinCode)
        }

        // Общий метод запросов
        private fun performRequest(url: String): String {
            return URL(url).readText()
        }
    }
}
