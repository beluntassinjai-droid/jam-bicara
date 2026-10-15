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
            text = "Memuat waktu dan baterai..."
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

        // Mendapatkan status sisa baterai
        val batteryIntent: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val levelBaterai = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1

        // Kalkulasi sederhana Hijriah (bisa disesuaikan dengan rumus astronomi akurat nanti)
        // Untuk sementara kita tampilkan format waktu dan baterai secara jelas
        val infoTeks = "Sekarang pukul $jam lewat $menit menit. Sisa baterai $levelBaterai persen."
        
        statusTextView.text = infoTeks
        
        // Membacakan suara
        tts.speak(infoTeks, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}