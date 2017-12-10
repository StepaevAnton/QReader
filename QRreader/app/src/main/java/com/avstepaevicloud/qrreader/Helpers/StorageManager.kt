package com.avstepaevicloud.qrreader.Helpers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.experimental.async
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
    private val dir : File = context.filesDir


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
        if (!dir.exists())
            success = dir.mkdirs()

        if (!success)
            throw TicketIdCheckException("Нет доступа к диску на чтение-запись!")

        val files = dir.listFiles()

        for (file in files)
        {
            try {
                val eventId = file.name.replace(filesExtension, "").toLong()
                val text =   file.readText(Charsets.UTF_8)
                val jsonTicketsInfos = text.split(infosLimiter)
                val ticketsInfoSet = hashSetOf<TicketInfo>()
                for (i in 0 until jsonTicketsInfos.count())
                {
                    val jsonTicketInfo = jsonTicketsInfos[i]
                    val components = jsonTicketInfo.split(infoComponentLimiter)
                    val idComponent = components[0]
                    if (idComponent.isEmpty())
                        continue

                    val id = idComponent.toLong()
                    val dt = SimpleDateFormat.getInstance().parse(components[1])
                    ticketsInfoSet.add(TicketInfo(id, dt))
                }
                cache[eventId] = ticketsInfoSet
            }
            catch (e: Exception){
                Log.getStackTraceString(e).toString()
                continue
            }
        }
    }

    /**
     * Проверить был ли отсканирован этот билет
     */
    fun checkAndAddTicketId(ticketId: Long, eventId: Long) {
        if (!cache.containsKey(eventId))
            cache[eventId] = HashSet<TicketInfo>()

        val currentCache = cache[eventId]
        val scannedTicket = currentCache!!.firstOrNull { it.id == ticketId }
        if (scannedTicket != null)
        {
            val msg = context.applicationContext.getString(com.avstepaevicloud.qrreader.R.string.ticket_already_was_scanned, " " + SimpleDateFormat.getInstance().format(scannedTicket.scanDt))
            throw TicketIdCheckException(msg)
        }

        val ticketInfo = TicketInfo(ticketId, Calendar.getInstance().time)
        currentCache.add(ticketInfo)
        cache[eventId] = currentCache

        async {
            writeChangesToDisc(eventId, ticketInfo)
        }
    }

    /**
     * Записать изменения на диск
     */
    private fun writeChangesToDisc(eventId: Long, ticketInfo: TicketInfo){
        if (!dir.exists())
            dir.mkdirs()

        var file = File(dir.absolutePath + "/" + eventId.toString() + filesExtension)
        if (!file.exists())
        {
            file = File(dir, eventId.toString() + filesExtension)
        }

        file.appendText(infosLimiter + ticketInfo.id + infoComponentLimiter + SimpleDateFormat.getInstance().format(ticketInfo.scanDt), Charsets.UTF_8)
    }
}

data class TicketInfo(val id: Long, val scanDt: Date)

/**
 * Эксепшн проверки идентификатора билета
 */
class TicketIdCheckException(message: String) : Exception(message)
