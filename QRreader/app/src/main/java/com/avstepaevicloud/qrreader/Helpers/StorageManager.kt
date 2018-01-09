package com.avstepaevicloud.qrreader.Helpers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.experimental.async
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

/**
 * Created by StepaevAV on 19.11.17.
 */

/**
 * Класс для работы с диском
 */
class StorageManager private constructor(val context: Context) {

    /**
     *  Конструктор синглтона
     */
    companion object : SingletonHolder<StorageManager, Context>({ context -> StorageManager(context) })

    /**
     * Ссылка на директорию приложения
     */
    private val dir: File = context.filesDir

    /**
     * Информация лежит на диске в виде .txt файлов, содержащих строковое представление информации об отсканированных билетах (json и xml нецелесообразны, ввиду ресурсоемкости добавления информации)
     * infosLimiter - разделить между инфо о билетах
     * infoComponentLimiter - разделитель внутри инфо о билете
     */
    private val infosLimiter = "|"
    private val infoComponentLimiter = "!"
    private var filesExtension = ".txt"

    /**
     * Закэшированная информация об отсканенных билетах
     */
    private val cache: HashMap<Long, HashSet<TicketInfo>> = hashMapOf()

    /**
     * Инициализация (поднять с диска всю информацию о проверенных ранее билетах)
     */
    init {
        var success = true
        if (!dir.exists()) success = dir.mkdirs()

        if (!success) throw TicketIdCheckException("Нет доступа к диску на чтение-запись!")

        val files = dir.listFiles()

        for (file in files) {
            try {
                val eventId = file.name.replace(filesExtension, "").toLong()
                val text = file.readText(Charsets.UTF_8)
                val jsonTicketsInfos = text.split(infosLimiter)
                val ticketsInfoSet = hashSetOf<TicketInfo>()
                for (i in 0 until jsonTicketsInfos.count()) {
                    val jsonTicketInfo = jsonTicketsInfos[i]
                    val components = jsonTicketInfo.split(infoComponentLimiter)
                    val idComponent = components[0]
                    if (idComponent.isEmpty()) continue

                    val id = idComponent.toLong()
                    val dt = SimpleDateFormat.getInstance().parse(components[1])
                    val state = TicketStateEnum.valueOf(components[2])

                    ticketsInfoSet.add(TicketInfo(id, dt, state))
                }
                cache[eventId] = ticketsInfoSet
            } catch (e: Exception) {
                Log.getStackTraceString(e).toString()
                continue
            }
        }
    }

    /**
     * Смерджиться с инфой об отсканированных билетах с сервера
     * TODO чересчур кучеряво
     */
    fun merge(eventId: Long, ticketInfos: HashSet<TicketInfo>) {
        if (!cache.containsKey(eventId) || cache[eventId] == null) {
            cache[eventId] = HashSet<TicketInfo>()
        }

        val currentCache = cache[eventId]

        // Идентификаторы билетов в кэше
        val cachedTicketsIds = currentCache!!.map { it.id }
        // Идентификаторы полученных билетов
        val receivedTicketsIds = ticketInfos.map { it.id }
        // Временная коллекция для итерирования
        @Suppress("UNCHECKED_CAST")
        val tmpCollection = currentCache.clone() as HashSet<TicketInfo>//hashSetOf(currentCache)

        tmpCollection.addAll(ticketInfos)

        val ticketsToPost = HashSet<TicketInfo>()

        for (ticketInfo in tmpCollection) {
            var cached = false
            var received = false
            if (cachedTicketsIds.contains(ticketInfo.id)){
                cached = true
            }

            if (receivedTicketsIds.contains(ticketInfo.id)){
                received = true
            }

            // Билет и в кэше и прилетел - все ок
            if (cached && received){
                continue
            }
            // Билет только прилетел - кладем в кэш
            else if (received) {
                currentCache.add(ticketInfo)
                writeChangesToDiscAsync(eventId, ticketInfo)
            }
            // Билет только в кэше - постим на сервер
            else if (cached){
                ticketsToPost.add(ticketInfo)

            }
        }

        HttpClient.postTickets(context, eventId.toString(), ticketsToPost, {})
    }

    /**
     * Проверить был ли отсканирован этот билет
     */
    fun checkAndAddTicketId(ticketId: Long, eventId: Long, completionHandler: (response: String?) -> Unit) {
        if (!cache.containsKey(eventId)){
            cache[eventId] = HashSet()
        }

        val currentCache = cache[eventId]
        val scannedTicket = currentCache!!.firstOrNull { it.id == ticketId }
        if (scannedTicket != null) {
            val msg = when (scannedTicket.state) {
                TicketStateEnum.Scanned -> context.applicationContext.getString(com.avstepaevicloud.qrreader.R.string.ticket_already_was_scanned, if (scannedTicket.scanDt != null) " "
                        + SimpleDateFormat.getInstance().format(scannedTicket.scanDt) else "")
                TicketStateEnum.Rejected -> context.applicationContext.getString(com.avstepaevicloud.qrreader.R.string.ticket_was_rejected)
            //else -> throw NotImplementedError("scannedTicket.state")
            }

            throw TicketIdCheckException(message = msg)
        }

        val ticketInfo = TicketInfo(ticketId, Calendar.getInstance().time, TicketStateEnum.Scanned)

        currentCache.add(ticketInfo)
        writeChangesToDiscAsync(eventId, ticketInfo)
        HttpClient.postTickets(context, eventId.toString(), hashSetOf(ticketInfo), completionHandler)
    }

    /**
     * Записать изменения на диск асинхронно
     */
    private fun writeChangesToDiscAsync(eventId: Long, ticketInfo: TicketInfo) {
        async {
            if (!dir.exists()){
                dir.mkdirs()
            }

            var file = File(dir.absolutePath + "/" + eventId.toString() + filesExtension)
            if (!file.exists()){
                file = File(dir, eventId.toString() + filesExtension)
            }

            file.appendText(infosLimiter + ticketInfo.id + infoComponentLimiter + (if (ticketInfo.scanDt != null) {
                SimpleDateFormat.getInstance().format(ticketInfo.scanDt)
            } else "") + infoComponentLimiter + ticketInfo.state, Charsets.UTF_8)
        }
    }
}

/**
 * Дата класс описания билета
 */
data class TicketInfo(val id: Long, val scanDt: Date?, val state: TicketStateEnum)

/**
 * Состояние билета
 */
enum class TicketStateEnum {
    // Отсканирован
    Scanned,
    // Отменен
    Rejected
}

/**
 * Эксепшн проверки идентификатора билета
 */
class TicketIdCheckException(message: String) : Exception(message)
