package com.avstepaevicloud.qrreader

import java.io.Serializable

/**
 * Дата класс событий
 */
data class EventData(val id: Long, val title: String, val dt: String, val code: String, val types: Map<Int, EventType>) : Serializable

/**
 * Дата класс типов событий
 */
data class EventType(val id: Int, val caption: String) : Serializable