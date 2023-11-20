package com.example.followerplant

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var tvHumedad: TextView
    private lateinit var humidityProgress: ProgressBar
    private lateinit var btnControlRelay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvHumedad = findViewById(R.id.tvHumedad)
        humidityProgress = findViewById(R.id.humidityProgress)
        btnControlRelay = findViewById(R.id.btnControlRelay)

        // Llamar a la función para obtener y mostrar la humedad actual desde el Arduino.
        getHumidityFromArduino()

        btnControlRelay.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // El botón ha sido presionado, activar el relé
                    controlRelay(true)
                    true // Retornar true indica que el evento ha sido manejado
                }
                MotionEvent.ACTION_UP -> {
                    // El botón ha sido soltado, desactivar el relé
                    controlRelay(false)
                    true
                }
                else -> false // Retornar false para las demás acciones que no nos interesan
            }
        }

    }

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 5000  // Intervalo de actualización en milisegundos (5 segundos)

    private val updateHumidityRunnable = object : Runnable {
        override fun run() {
            getHumidityFromArduino()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateHumidityRunnable)  // Comenzar las actualizaciones cuando la aplicación esté en primer plano
    }


    private fun updateHumidityDisplay(humidity: Int) {
        tvHumedad.text = "Humedad: $humidity%"
        humidityProgress.progress = humidity
    }

    private fun getHumidityFromArduino() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url("http://192.168.1.135/humidity").build()

                // Aquí se inicia el bloque 'use', y cualquier variable definida dentro de este bloque sólo puede ser usada aquí.
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val responseBody = response.body?.string()?.trim()
                    val humidityValue = responseBody?.toIntOrNull()
                    humidityValue?.let {
                        withContext(Dispatchers.Main) {
                            updateHumidityDisplay(it)
                        }
                    } ?: throw IOException("Received non-integer value: $responseBody")
                }
                // 'response' no puede ser usado fuera del bloque 'use'.
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Actualiza la UI para mostrar el error.
                    tvHumedad.text = "Error al obtener la humedad: ${e.message}"
                }
            }
        }
    }



    private fun controlRelay(state: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val url = if (state) "http://192.168.1.135/activate" else "http://192.168.1.135/deactivate"
                val request = Request.Builder().url(url).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code $response")
                    }
                    // Aquí podrías implementar lógica adicional si necesitas manejar la respuesta.
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Aquí puedes implementar lógica para manejar el error, por ejemplo, actualizando la interfaz de usuario.
            }
        }
    }


}