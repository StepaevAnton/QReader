package com.avstepaevicloud.qrreader

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.support.v7.app.AlertDialog
import com.avstepaevicloud.qrreader.Helpers.*


class EventDetailsActivity : AppCompatActivity() {

    // Константы
    private val errorTone: Int = ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_NORMAL
    private val okTone: Int = ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE
    private val ticketInfoKey = "ticketInfo"

    // UI элементы
    private var eventNameTextView: TextView? = null
    private var eventDtTextView: TextView? = null
    private var ticketDetailsTextView: TextView? = null
    private var scanCodeButton: Button? = null

    private var event: EventData? = null
        set(value) {
            if (value == null)
                return

            eventNameTextView!!.text = value.title
            eventDtTextView!!.text = value.dt
            field = value
        }

    private var lastScannedTicketInfo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        eventNameTextView = findViewById(R.id.event_label)
        eventDtTextView = findViewById(R.id.event_dt)
        scanCodeButton = findViewById(R.id.scan_code_button)
        ticketDetailsTextView = findViewById(R.id.ticket_details)

        scanCodeButton!!.setOnClickListener { startCodeScanning() }

        event = intent.getSerializableExtra("event") as EventData

        if (event == null)
            finish()

        if (savedInstanceState != null && savedInstanceState.containsKey(ticketInfoKey))
            ticketDetailsTextView!!.text = savedInstanceState[ticketInfoKey] as String

        // DEBUG
        //processScanResult("123")
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        if (outState == null)
            return
        outState.putString(ticketInfoKey, lastScannedTicketInfo)
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
        if (barcode.isNullOrEmpty())
            return

        processScanResult(barcode!!)
    }

    /**
     * Обработка результата сканирования
     */
    private fun processScanResult(scanResultAsString: String) {
        try {
            val scanResult = ScanResultParser.unsafeParse(scanResultAsString, event!!.code, this)

           if (scanResult.eventId != event!!.id)
                throw TicketIdCheckException(applicationContext.getString(R.string.wrong_event_id_on_ticket))

            StorageManager.getInstance(applicationContext).checkAndAddTicketId(scanResult.ticketId, scanResult.eventId)//scanResult.eventId)

            val ticketType = getTicketTypyAsString(scanResult.ticketType)

            var addInfo = ""
            if (scanResult.ticketType == 0)
                addInfo = "\r\nРяд: ${scanResult.row}\r\nМесто: ${scanResult.seat}"

            val ticketInfo = "№ ${scanResult.ticketId}\r\nТип: $ticketType$addInfo"

            lastScannedTicketInfo = "Последний отсканированный билет:\r\n\r\n$ticketInfo"

            playSound(okTone)

            AlertDialog.Builder(this).setMessage("Билет " + ticketInfo)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }.setIcon(android.R.drawable.ic_dialog_info).show()

            ticketDetailsTextView!!.text = lastScannedTicketInfo

        } catch (e: Exception) {
            var errorMsg = applicationContext.getString(R.string.qr_code_has_invalid_format)

            if (e is ResultParsingException || e is TicketIdCheckException)
                errorMsg = e.message

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
