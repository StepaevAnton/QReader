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

    private val errorTone: Int = ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_NORMAL
    private val okTone: Int = ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE

    private var event: EventData? = null
        set(value) {
            if (value == null)
                return

            eventNameTextView!!.text = value.title
            eventDtTextView!!.text = value.dt
            field = value
        }

    private var eventNameTextView: TextView? = null
    private var eventDtTextView: TextView? = null
    private var scanCodeButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        eventNameTextView = findViewById(R.id.event_label)
        eventDtTextView = findViewById(R.id.event_dt)
        scanCodeButton = findViewById(R.id.scan_code_button)
        scanCodeButton!!.setOnClickListener { startCodeScanning() }

        event = intent.getSerializableExtra("event") as EventData

        if (event == null)
            finish()
    }

    /**
     * Запустить сканер
      */
    private fun startCodeScanning(){
        val intent = Intent(this, ScannerActivityView::class.java)
        startActivityForResult(intent, 2)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            val barcode = data?.extras?.getString("BarCode")
            if (barcode.isNullOrEmpty())
                return

            val scanResult = ScanResult(123,456,1,35,4)//ScanResultParser.unsafeParse(barcode!!, event!!.code)
            if (scanResult.eventId != event!!.id)
                throw TicketIdCheckException("Билет не соответствует выбранному мероприятию!")

            StorageManager.getInstance(applicationContext).checkAndAddTicketId(scanResult.ticketId, 789)//scanResult.eventId)
            // TODO здесь ли воспроизводить okTone
            playSound(okTone)
        }
        catch (e: Exception)
        {
            var errorMsg = applicationContext.getString(R.string.qr_code_has_invalid_format)

            if (e is ResultParsingException || e is TicketIdCheckException)
                errorMsg = e.message

            AlertDialog.Builder(this).setMessage(errorMsg).setTitle(R.string.error)
                    .setPositiveButton(android.R.string.ok){_, _ -> }.setIcon(android.R.drawable.ic_dialog_alert).show()

            playSound(errorTone)
        }
    }

    /**
     * Воспроизвести звук
     */
    private fun playSound(tone: Int) {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
            toneGenerator.startTone(tone)
        }
        catch (e: Exception){
            // ignored
        }
    }
}



//        if (barcode == null || barcode.equals("")) {
//            Toast.makeText(this@EventDetailsActivity, "Bar Code Not Found", Toast.LENGTH_LONG).show()
//            return
//        }
//
//        //bar_code_id_txt?.text = barcode
//
//        var base64string = barcode!!.replace(PREFIX, StringEmpty)
//
//        var bytes = Base64.decode(base64string, 8)
//
//        var data = bytes.slice(0..10).toByteArray()
//        var sign = bytes.slice(11..18).toByteArray()
//
//        var digestInput = data + "123456".toByteArray(Charsets.UTF_8)
//
//        var md5Digest = MessageDigest.getInstance("MD5")
//        md5Digest.reset()
//        md5Digest.update(digestInput)
//
//        val md5Hash = md5Digest.digest()