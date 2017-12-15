package com.avstepaevicloud.qrreader

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.support.v7.app.AlertDialog
import com.avstepaevicloud.qrreader.Helpers.*
import kotlinx.coroutines.experimental.async
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date


class EventDetailsActivity : AppCompatActivity() {

    // Константы
    // Звуки
    private val errorTone: Int = ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_NORMAL
    private val okTone: Int = ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE
    // Ключи для сохранения состояния
    private val ticketInfoKey = "ticketInfo"
    private val ticketsLoadedKey = "ticketsLoaded"

    // UI элементы
    private var eventNameTextView: TextView? = null
    private var eventDtTextView: TextView? = null
    private var ticketDetailsTextView: TextView? = null
    private var scanCodeButton: Button? = null

    private var event: EventData? = null
        set(value) {
            if (value == null) return

            eventNameTextView!!.text = value.title
            eventDtTextView!!.text = value.dt
            field = value
        }

    private var lastScannedTicketInfo: String = ""

    /**
     * Формат даты для получения билетов
     */
    @SuppressLint("SimpleDateFormat")
    private val getTicketDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    /**
     * Признак того, что билеты уже были загружены
     */
    private var ticketsLoaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        eventNameTextView = findViewById(R.id.event_label)
        eventDtTextView = findViewById(R.id.event_dt)
        scanCodeButton = findViewById(R.id.scan_code_button)
        ticketDetailsTextView = findViewById(R.id.ticket_details)

        scanCodeButton!!.setOnClickListener { startCodeScanning() }

        event = intent.getSerializableExtra("event") as EventData

        if (event == null) finish()

        if (savedInstanceState != null && savedInstanceState.containsKey(ticketInfoKey))
            ticketDetailsTextView!!.text = savedInstanceState[ticketInfoKey] as String

        if (savedInstanceState != null && savedInstanceState.containsKey(ticketsLoadedKey))
        {
            ticketsLoaded = savedInstanceState[ticketsLoadedKey] as Boolean
        }

        if (ticketsLoaded) return

        async {
            val response = JSONObject(HttpClient.getTickets(event!!.id.toString()))

            val success = response.getBoolean("success")
            if (!success) return@async

            val data = response.getJSONObject("data")
            val ticketsInfo = HashSet<TicketInfo>()
            loop@ for (key in data.keys()) {
                try {
                    val obj = data.getJSONObject(key)
                    if (!obj.has("event_id") || !obj.has("status")) continue
                    if (obj.getLong("event_id") != event!!.id) continue

                    val statusCode = obj.getInt("status")
                    val ticketState = when (statusCode) {
                        6 -> TicketStateEnum.Scanned
                        8 -> TicketStateEnum.Rejected
                        else -> continue@loop
                    }

                    var dt: Date?
                    try {
                        dt = getTicketDateFormat.parse(obj.getString("status_time"))
                    } catch (e: Exception) {
                        dt = null
                    }

                    ticketsInfo.add(TicketInfo(key.toLong(), dt, ticketState))
                } catch (e: Exception) {
                    continue
                }
            }

            StorageManager.getInstance(applicationContext).merge(event!!.id, ticketsInfo)
            ticketsLoaded = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        if (outState == null) return

        outState.putString(ticketInfoKey, lastScannedTicketInfo)
        outState.putBoolean(ticketsLoadedKey, ticketsLoaded)
    }

    /**
     * Запустить сканер
     */
    private fun startCodeScanning() {
        val intent = Intent(this, ScannerActivityView::class.java)
        startActivityForResult(intent, 2)
    }

    /**
     * Делегат обработки результата сканирования
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        val barcode = data?.extras?.getString("BarCode")
        if (barcode.isNullOrEmpty()) return

        processScanResult(barcode!!)
    }

    /**
     * Обработка результата сканирования
     */
    private fun processScanResult(scanResultAsString: String) {
        try {
            val scanResult = ScanResultParser.unsafeParse(scanResultAsString, event!!.code, this)

            // Идентификатор выбранного мероприятия и билета не совпадают
            if (scanResult.scanResult.eventId != event!!.id && scanResult.success)
            {
                //throw TicketIdCheckException(applicationContext.getString(R.string.wrong_event_id_on_ticket))
                scanResult.success = false
                scanResult.msg = applicationContext.getString(R.string.wrong_event_id_on_ticket)
            }

            if (scanResult.success)
                {
                    try {
                        StorageManager.getInstance(applicationContext).checkAndAddTicketId(scanResult.scanResult.ticketId, scanResult.scanResult.eventId)
                    }
                    catch (e: TicketIdCheckException)
                    {
                        if (scanResult.success)
                        {
                            scanResult.success = false
                            scanResult.msg = e.message ?: "Произошла неожиданная ошибка!"
                        }
                    }
                }


            val ticketType = getTicketTypyAsString(scanResult.scanResult.ticketType)

            // TODO строки в ресурсы
            var addInfo = ""
            if (scanResult.scanResult.ticketType == 0) {
                addInfo = "\r\nРяд: ${scanResult.scanResult.row}\r\nМесто: ${scanResult.scanResult.seat}"
            }

            val ticketInfo = "№ ${scanResult.scanResult.ticketId}\r\nТип: $ticketType$addInfo"

            lastScannedTicketInfo = "Последний отсканированный билет:\r\n\r\n$ticketInfo"

            if (!scanResult.success)
            {
                lastScannedTicketInfo = lastScannedTicketInfo + "\r\nОшибка: ${scanResult.msg}"
                ticketDetailsTextView!!.text = lastScannedTicketInfo

                throw ResultParsingException(scanResult.msg)
            }

            ticketDetailsTextView!!.text = lastScannedTicketInfo

            playSound(okTone)

            AlertDialog.Builder(this).setMessage("Билет " + ticketInfo)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }.setIcon(android.R.drawable.ic_dialog_info).show()

        } catch (e: Exception) {
            var errorMsg = applicationContext.getString(R.string.qr_code_has_invalid_format)

            if (e is ResultParsingException || e is TicketIdCheckException){
                errorMsg = e.message
            }

            AlertDialog.Builder(this).setMessage(errorMsg).setTitle(R.string.error)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }.setIcon(android.R.drawable.ic_dialog_alert).show()

            playSound(errorTone)
        }
    }

    /**
     * Получить строковое представление типа билета
     */
    private fun getTicketTypyAsString(ticketType: Int): String? {
        val res = event?.types?.get(ticketType)?.caption

        return if (res.isNullOrEmpty()) "" else res
    }

    /**
     * Воспроизвести звук
     */
    private fun playSound(tone: Int) {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
            toneGenerator.startTone(tone)
        } catch (e: Exception) {
            // ignored
        }
    }
}
