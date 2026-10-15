package com.bicara

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.LinearLayout
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

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        
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
            text = "Menghubungkan ke server kalender..."
            textSize = 16f
            gravity = Gravity.CENTER
        }
        box.addView(statusTextView)
        
        setContentView(box)

        // Inisialisasi suara (Text-to-Speech)
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                statusTextView.text = "Bahasa Indonesia tidak didukung di perangkat ini."
            } else {
                ambilWaktuDanHijriahOnline()
            }
        } else {
            statusTextView.text = "Gagal menginisialisasi suara."
        }
    }

    private fun ambilWaktuDanHijriahOnline() {
        val calendar = Calendar.getInstance()
        val jam = calendar.get(Calendar.HOUR_OF_DAY)
        val menit = calendar.get(Calendar.MINUTE)
        
        // Format tanggal Masehi hari ini (YYYY-MM-DD)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val tanggalMasehi = dateFormat.format(calendar.time)

        // Mendapatkan status sisa baterai
        val batteryIntent: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val levelBaterai = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1

        // Menjalankan proses jaringan di latar belakang (Background Thread) agar aplikasi tidak macet
        Thread {
            var teksHijriah = "tanggal Hijriah"
            try {
                // Endpoint API Kalender Islam standar Indonesia
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
                        // Indeks ke-1 berisi string format lengkap tanggal Hijriah (contoh: "27 Syaban 1445 H")
                        teksHijriah = dateArray.getString(1)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                teksHijriah = "gagal memuat tanggal Hijriah online"
            }

            // Kembali ke UI Thread untuk menampilkan dan membacakan teks
            runOnUiThread {
                val infoTeks = "Sekarang pukul $jam lewat $menit menit. Tanggal $teksHijriah. Sisa baterai $levelBaterai persen."
                statusTextView.text = infoTeks
                tts.speak(infoTeks, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }.start()
    }

    override onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}