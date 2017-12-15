package com.avstepaevicloud.qrreader

import java.io.Serializable

/**
 * Дата класс событий
 */
data class EventData(val id: Long, val title: String, val dt: String, val code: String, val types: Map<Int, TicketType>) : Serializable

/**
 * Дата класс типов событий
 */
data class TicketType(val id: Int, val caption: String) : Serializable