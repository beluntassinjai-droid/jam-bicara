package com.bicara

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var statusTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var intervalMenit: Long = 0
    private var isUserSelecting = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        
        // Layar dibiarkan normal (tidak dipaksa menyala terus)
        
        val box = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32) 
        }
        
        box.addView(TextView(this).apply { 
            text = "Jam Bicara"
            textSize = 26f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        })
        
        statusTextView = TextView(this).apply { 
            text = "Memuat waktu, Hijriah, dan baterai..."
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 30)
        }
        box.addView(statusTextView)

        box.addView(TextView(this).apply {
            text = "Pilih Interval Waktu Berbunyi:"
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 10)
        })

        val spinnerInterval = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 10, 0, 20)
            }
        }

        val pilihanInterval = arrayOf(
            "Matikan Interval Otomatis",
            "Setiap 5 Menit",
            "Setiap 10 Menit",
            "Setiap 15 Menit",
            "Setiap 20 Menit",
            "Setiap 25 Menit",
            "Setiap 30 Menit",
            "Setiap 35 Menit",
            "Setiap 40 Menit",
            "Setiap 45 Menit",
            "Setiap 50 Menit",
            "Setiap 55 Menit",
            "Setiap 60 Menit"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, pilihanInterval)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerInterval.adapter = adapter

        spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (!isUserSelecting) {
                    isUserSelecting = true
                    return
                }

                val menitDipilih = when (position) {
                    0 -> 0L
                    1 -> 5L
                    2 -> 10L
                    3 -> 15L
                    4 -> 20L
                    5 -> 25L
                    6 -> 30L
                    7 -> 35L
                    8 -> 40L
                    9 -> 45L
                    10 -> 50L
                    11 -> 55L
                    12 -> 60L
                    else -> 0L
                }
                if (::tts.isInitialized) {
                    aturInterval(menitDipilih)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        box.addView(spinnerInterval)
        setContentView(box)

        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                statusTextView.text = "Bahasa Indonesia tidak didukung di perangkat ini."
            } else {
                ucapWaktuDanBaterai()
            }
        } else {
            statusTextView.text = "Gagal menginisialisasi suara."
        }
    }

    private fun ucapWaktuDanBaterai() {
        val calendar = Calendar.getInstance()
        val jam = calendar.get(Calendar.HOUR_OF_DAY)
        val menit = calendar.get(Calendar.MINUTE)
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val tanggalMasehi = dateFormat.format(calendar.time)

        val batteryIntent: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val levelBaterai = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1

        Thread {
            var teksHijriah = "tanggal Hijriah"
            try {
                val url = URL("https://api.myquran.com/v2/cal/hijr/$tanggalMasehi")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(responseString)
                    val status = jsonObject.getBoolean("status")
                    if (status) {
                        val dataObj = jsonObject.getJSONObject("data")
                        val dateArray = dataObj.getJSONArray("date")
                        teksHijriah = dateArray.getString(1)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            runOnUiThread {
                val infoTeks = "Sekarang pukul $jam lewat $menit menit. Tanggal $teksHijriah. Sisa baterai $levelBaterai persen."
                statusTextView.text = infoTeks
                tts.speak(infoTeks, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }.start()
    }

    private fun aturInterval(menit: Long) {
        intervalMenit = menit
        handler.removeCallbacksAndMessages(null)

        if (menit == 0L) {
            statusTextView.text = "Interval otomatis dimatikan."
            tts.speak("Interval otomatis dimatikan.", TextToSpeech.QUEUE_FLUSH, null, null)
            return
        }

        statusTextView.text = "Interval diatur: Berbunyi setiap $menit menit."
        tts.speak("Interval diatur setiap $menit menit.", TextToSpeech.QUEUE_FLUSH, null, null)

        runnable = object : Runnable {
            override fun run() {
                ucapWaktuDanBaterai()
                handler.postDelayed(this, intervalMenit * 60 * 1000)
            }
        }
        
        handler.postDelayed(runnable!!, intervalMenit * 60 * 1000)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}