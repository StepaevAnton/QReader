package com.avstepaevicloud.qrreader

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.view.View
import android.util.Log
import android.widget.*
import com.avstepaevicloud.qrreader.Helpers.HttpClient
import com.avstepaevicloud.qrreader.Helpers.NetworkManager
import kotlinx.coroutines.experimental.async
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.*

/**
 * Общий класс для всех активити, проверяющих подключение
 */
abstract class NetworkCkeckingActivity : AppCompatActivity() {

    fun String.Companion.Empty() = ""

    /**
     * Проверить подключение
     */
    protected fun isNetworkConnected() {
        val context = this
        async {
            val isNetworkConnected = NetworkManager.isNetworkConnected()
            runOnUiThread {
                if (isNetworkConnected)
                    continueExecution()
                else
                // TODO в ресурсы
                    AlertDialog.Builder(context).setTitle("Подключение к интернету отсутствует!")
                            .setMessage("Проверьте подключение к интернету и попробуйте позже.")
                            .setPositiveButton(android.R.string.ok) { dialog, which ->
                                run {
                                    isNetworkConnected()
                                }
                            }
                            .setIcon(android.R.drawable.ic_dialog_alert).show()
            }
        }
    }

    /**
     * Действие после успешной проверки подключения
     */
    protected abstract fun continueExecution()
}

/**
 * Основное активити. Пин-код, отображение списка событий
 */
class MainActivity : NetworkCkeckingActivity() {

    /**
     * Прогресс бар
     */
    var progressBar: ProgressBar? = null

    /**
     * Лист-вью событий
     */
    var eventsListView: ListView? = null


    /**
     * Список событий
     */
    var events: Array<EventData> = arrayOf<EventData>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        eventsListView = findViewById(R.id.events_list_view)
        progressBar = findViewById(R.id.progress_bar)
        progressBar!!.visibility = View.INVISIBLE

        if (savedInstanceState != null && savedInstanceState.containsKey("events"))
            events = savedInstanceState.getSerializable("events") as Array<EventData>

        if (events.any())
            showEventsList()
        else
            isNetworkConnected()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        if (outState == null)
            return
        outState.putSerializable("events", events)
    }

    /**
     *  Продолжить выполнение после успешной проверки подключения
     */
    override fun continueExecution() {
        showPinCodeDialog()
    }

    /**
     * Отобразить диалог ввода пинкода
     */
    private fun showPinCodeDialog() {

        val builder = AlertDialog.Builder(this)

        builder.setTitle(R.string.enter_pin)

        val customView = View.inflate(this, R.layout.enter_pin_dialog, null)

        builder.setView(customView)
        builder.setPositiveButton("OK", { _, _ ->
            run {
                pinCodeDialogOkClick(customView)
            }
        })

        builder.show()
    }

    /**
     *  Пин код введен
     */
    private fun pinCodeDialogOkClick(dialogView: View) {

        val pinCode = dialogView.findViewById<EditText>(R.id.pin_code).text.toString()
        HttpClient.pinCode = pinCode
        progressBar!!.visibility = View.VISIBLE

        async {
            val eventsList = HttpClient.getEventsList()
            val jsonObject = JSONObject(eventsList)
            val isAnswerCorrect = isAnswerCorrect(jsonObject)
            runOnUiThread {
                if (isAnswerCorrect)
                    parseAndShowEvents(jsonObject.getJSONArray("data"))
                else {
                    // TODO в ресурсы
                    Toast.makeText(this@MainActivity, R.string.incorrect_pin_code, Toast.LENGTH_LONG).show()
                    showPinCodeDialog()
                }
                progressBar!!.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * Сохранить события
     */
    private fun parseAndShowEvents(eventsJson: JSONArray) {

        val mutableList = mutableListOf<EventData>()

        for (i in 0 until eventsJson.length()) {
            try {
                val obj = eventsJson.getJSONObject(i)
                if (!obj.has("id") || !obj.has("title") || !obj.has("code") or (obj.has("hidden") && obj.getInt("hidden") != 0))
                    continue

                val dtStrings = mutableListOf<String>()
                if (obj.has("date"))
                    dtStrings.add(obj.getString("date"))
                if (obj.has("time"))
                    dtStrings.add(obj.getString("time"))

                val eventTypes = mutableMapOf<Int, TicketType>()
                if (obj.has("types")) {
                    val typesJson = obj.getJSONArray("types")
                    for (j in 0 until typesJson.length()) {
                        val eventTypeJson = typesJson.getJSONObject(j)
                        if (!eventTypeJson.has("id"))
                            continue

                        val id = eventTypeJson.getInt("id")
                        var eventTypeCaption = String.Empty()
                        if (eventTypeJson.has("title")) {
                            eventTypeCaption = eventTypeJson.getString("title")
                            if (eventTypeJson.has("info")) {
                                val eventInfo = eventTypeJson.getString("info")
                                eventTypeCaption += " ($eventInfo)"
                            }
                        }

                        eventTypes.put(id, TicketType(id, eventTypeCaption))
                    }
                }

                mutableList.add(EventData(obj.getLong("id"), obj.getString("title"), dtStrings.joinToString(" "), obj.getString("code"), eventTypes))
            }
            catch (e: Exception){
                // ignored
            }
        }

        events = mutableList.toTypedArray()

        showEventsList()
    }

    /**
     * Отобразить список событий
     */
    private fun showEventsList() {
        val arrayList = ArrayList<HashMap<String, String>>()

        events.forEach({ eventData ->
            run {
                val map = HashMap<String, String>()
                map.put("Title", eventData.title)
                map.put("Dt", eventData.dt)
                arrayList.add(map)
            }
        })

        val adapter = SimpleAdapter(this, arrayList, android.R.layout.simple_list_item_2, arrayOf("Title", "Dt"), intArrayOf(android.R.id.text1, android.R.id.text2))
        eventsListView!!.adapter = adapter
        eventsListView!!.visibility = View.VISIBLE
        eventsListView!!.setOnItemClickListener({ _, _, position, _ ->
            run {
                performSegueEventDetailsActivity(position)
            }
        })
    }

    /**
     * Отобразить активити детализации по событию
     */
    private fun performSegueEventDetailsActivity(position: Int) {
        if (events.lastIndex < position)
            return
        val event = events[position]

        val intent = Intent(this, EventDetailsActivity::class.java)
        intent.putExtra("event", event as Serializable)
        startActivity(intent)
    }

    /**
     * Проверка правильного пина (наличие поля code у событий)
     */
    private fun isAnswerCorrect(obj: JSONObject): Boolean {
        try {
            val success = obj.getBoolean("success")
            if (!success)
                return false
            val data = obj.getJSONArray("data")
            if ((data == null) or (data.length() == 0))
                return false
            val firstObj = data[0] as JSONObject

            return firstObj.has("code")
        } catch (e: Exception) {
            Log.getStackTraceString(e).toString()
            return false
        }
    }
}
