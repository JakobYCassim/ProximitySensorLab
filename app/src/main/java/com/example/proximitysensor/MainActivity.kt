package com.example.proximitysensor

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.util.UUID

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var proximity: Sensor? = null
    private var isEnabled: Boolean = false
    private var client: Mqtt5BlockingClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker.sundaebytestt.com")
            .serverPort(1883)
            .build()
            .toBlocking()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event == null) return
        if(event.sensor.type != Sensor.TYPE_PROXIMITY) return

        val distanceToNearestObject = event.values[0]
        val textView = findViewById<TextView>(R.id.SensorStatus)

        Log.d("SENSOR", "DISTANCE = $distanceToNearestObject")
        val textToSend = if(distanceToNearestObject < event.sensor.maximumRange) {
             "Jakob's phone is near an object"
        } else {
            "Jakob's phone is NOT near an object"
        }

        textView.text = textToSend

        try{
            client?.publishWith()?.topic("lab/proximity")?.payload(textToSend.toByteArray())?.send()
        } catch (e: Exception) {
            Toast.makeText(this, "An error occurred while sending a message to the broker", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d("SENSOR","Accuracy Changed")
    }

    fun enableSensor(view : View) {
        if(isEnabled){
            val text = "Sensor already enabled"
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
        proximity?.also { proximity ->
            sensorManager.registerListener(this,proximity,SensorManager.SENSOR_DELAY_NORMAL)
        }
        isEnabled = true
        Log.d("SENSOR","The Sensor has been enabled")

        try {
            client?.connect()
        } catch (e:Exception){
            Toast.makeText(this,"An error occurred when connecting to broker", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    fun disableSensor(view: View) {
        if(!isEnabled) {
            val text = "Sensor is already disabled"
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }

        sensorManager.unregisterListener(this)
        isEnabled = false
        findViewById<TextView>(R.id.SensorStatus).text = "The phone is not receiving data from the proximity sensor"

        try {
            client?.disconnect()
        } catch (e: Exception) {
            Toast.makeText(this, "An error occurred while disconnecting from the broker", Toast.LENGTH_SHORT).show()
        }

    }


}