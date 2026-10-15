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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var statusTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var intervalMenit: Long = 0

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        
        // Buat ScrollView agar semua tombol pilihan interval bisa digulir ke bawah
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

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
            setPadding(0, 20, 0, 30)
        }
        box.addView(statusTextView)

        // Tombol Matikan Interval
        box.addView(buatTombol("Matikan Interval Otomatis", Color.parseColor("#D32F2F")) { aturInterval(0) })

        // Daftar pilihan interval kelipatan 5 menit (dari 5 hingga 60 menit)
        val daftarMenit = intArrayOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60)
        for (m in daftarMenit) {
            box.addView(buatTombol("Bunyi Setiap $m Menit") { aturInterval(m.toLong()) })
        }
        
        scrollView.addView(box)
        setContentView(scrollView)

        // Inisialisasi suara (Text-to-Speech)
        tts = TextToSpeech(this, this)
    }

    private fun buatTombol(teks: String, warnaBg: Int = Color.parseColor("#1976D2"), onClick: () -> Unit): Button {
        return Button(this).apply {
            text = teks
            setTextColor(Color.WHITE)
            setBackgroundColor(warnaBg)
            setOnClickListener { onClick() }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)
            layoutParams = params
        }
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

        val infoTeks = "Sekarang pukul $jam lewat $menit menit. Sisa baterai $levelBaterai persen."
        statusTextView.text = infoTeks
        
        // Membacakan suara
        tts.speak(infoTeks, TextToSpeech.QUEUE_FLUSH, null, null)
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
        
        // Mulai hitung mundur pertama kali
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