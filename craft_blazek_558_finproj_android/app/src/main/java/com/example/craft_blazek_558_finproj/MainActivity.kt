/*
// RPi MQTT App
// ECE 558 | Winter 2022 | Professor Kravitz | Final Project

Created By:
        -David Craft <dcraft@pdx.edu> and Josh Blazek <blazek@pdx.edu>

Project:
Various sensors connected to RPi.  Sending images and sensor data to this phone.
*/
package com.example.craft_blazek_558_finproj

import android.app.TimePickerDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.craft_blazek_558_finproj.databinding.ActivityMainBinding
import org.eclipse.paho.client.mqttv3.*
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener {

    private val TAG = MainActivity::class.java.simpleName

    private lateinit var binding: ActivityMainBinding
    private lateinit var mqttClient: MQTTClient
    private lateinit var mqttClientID: String

    var currentHour = 0
    var currentMinut: Int = 0
    var hour: Int = 0
    var minute: Int = 0

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //accessing the seekbar from our layout
        val seekBar = findViewById<SeekBar>(R.id.interval)

        //Create fragment
        val imgFragment = SimpleFragment()

        binding.btnFragment.setOnClickListener{
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.flFragment, imgFragment)
                addToBackStack(null)
                commit()
            }
        }


        //setup button to select a time for light schedule
        binding.btnPick.setOnClickListener {
            val calendar: Calendar = Calendar.getInstance()

            hour = calendar.get(Calendar.HOUR)
            minute = calendar.get(Calendar.MINUTE)
            val timePickerDialog = TimePickerDialog(this@MainActivity, this@MainActivity, hour, minute,
                DateFormat.is24HourFormat(this))

            timePickerDialog.show()
        }
        // check if internet connection is available
        // exit if not available
        if (!isConnected()) {
            Log.d(TAG, "Internet connection NOT available")
            Toast.makeText(this, "Internet connection NOT available", Toast.LENGTH_LONG).show()
            finish()
        } else{
            Log.d(TAG, "Connected to the Internet")
            Toast.makeText(this, "Connected to the Internet", Toast.LENGTH_LONG).show()
        }

        // open mQTT Broker communication
        mqttClientID = MqttClient.generateClientId()
        mqttClient = MQTTClient(this, MQTT_SERVER_URI, mqttClientID)

        //set initial state of the buttons
        binding.connectBtn.isEnabled = true
        binding.disconnectBtn.isEnabled = false
        binding.liteonBtn.isEnabled = false
        binding.liteoffBtn.isEnabled = false
        binding.fanonBtn.isEnabled = false
        binding.fanoffBtn.isEnabled = false
        binding.pumponBtn.isEnabled = false
        binding.pumpoffBtn.isEnabled = false

        // Connect and Disconnect listeners
        binding.connectBtn.setOnClickListener {
            //connect to MQTT Broker and subscribe to the status topic
            mqttClient.connect(
                MQTT_USERNAME,
                MQTT_PWD,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "Connection success")

                        val successMsg = "MQTT Connection to $MQTT_SERVER_URI Established"
                        Toast.makeText(this@MainActivity, successMsg,Toast.LENGTH_LONG).show()
                        binding.connectBtn.isEnabled  = false
                        binding.disconnectBtn.isEnabled = true

                        // enable the LED buttons
                        binding.liteonBtn.isEnabled = true
                        binding.liteoffBtn.isEnabled = true
                        binding.fanonBtn.isEnabled = true
                        binding.fanoffBtn.isEnabled = true
                        binding.pumponBtn.isEnabled = true
                        binding.pumpoffBtn.isEnabled = true

                        // subscribe to the topics
                        // These could probably be combined, but I opted to have a separate function to
                        // subscribe to each topic for easier debug.
                        subscribeToRPiTopics()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d(TAG, "Connection failure: ${exception.toString()}")

                        val failureMsg = "MQTT Connection to $MQTT_SERVER_URI failed:${exception.toString()}"
                        Toast.makeText(this@MainActivity, failureMsg,Toast.LENGTH_LONG).show()
                        exception?.printStackTrace()
                    }
                },
                object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.d(TAG, "Connection lost ${cause.toString()}")
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val msg = "Received message: ${message.toString()} from topic: $topic"
                        Log.d(TAG, msg)
                        // update status textview
                        // since a message arrived I'm assuming the topic string is not null
                        // set values of textviews and progress bars to incoming messages
                        if (topic == CELCIUS_TOPIC){
                            binding.celciusTextView.text = message.toString()
                        }
                        if (topic == HUMIDITY_TOPIC) {
                            binding.humidityTextView.text = message.toString()
                        }
                        if (topic == LIGHT_SENS_TOPIC) {
                            binding.lumTextView.text = message.toString()
                        }
                        if (topic == SOIL_TOPIC){
                            binding.soilTextView.text = message.toString()
                        }
                        if (topic == WATER_LEVEL_TOPIC){
                            binding.waterLevTextView.text = message.toString()
                        }
                        if (topic == FAN_TOPIC){
                            binding.fanTextView.text = message.toString()
                        }
                        if (topic == PUMP_TOPIC){
                            binding.pumpTextView.text = message.toString()
                        }
                        if (topic == LIGHTS_MAT_TOPIC){
                            binding.lightsMatTextView.text = message.toString()
                        }
                        if (topic == FAHR_GRAPH_TOPIC) {
                            binding.tempBar.progress = (message.toString()).toInt()
                        }
                        if (topic == LUM_GRAPH_TOPIC) {
                            binding.lumsBar.progress = (message.toString()).toInt()
                        }
                        if (topic == HUMIDITY_GRAPH_TOPIC) {
                            binding.humiBar.progress = (message.toString()).toInt()
                        }
                        if (topic == SOIL_GRAPH_TOPIC) {
                            binding.soilBar.progress = (message.toString()).toInt()
                        }
                        if (topic == WATER_GRAPH_TOPIC) {
                            binding.watBar.progress = (message.toString()).toInt()
                        }
                        if (topic == MILDEW_DETECT) {
                            val msgDetect = "\t\t\t     ALERT!!!\n$message"
                            Toast.makeText(this@MainActivity,msgDetect,Toast.LENGTH_LONG).show()
                        }
                        if (topic == PLANT_IMAGE) {
                            val msgImage = "image received!"
                            Toast.makeText(this@MainActivity,msgImage,Toast.LENGTH_LONG).show()

                            // Image handling...
                            val image: ImageView = findViewById(R.id.imgPicker)

                            //val bMap = BitmapFactory.decodeFile("/sdcard/test2.png")
                            val bMap = BitmapFactory.decodeByteArray(message?.payload, 0, message?.payload!!.size)
                            image.setAlpha(255)
                            image.setImageBitmap(bMap)
                            Timer("popup for image", false).schedule(2000) {
                                image.setAlpha(0)
                            }
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d(TAG, "Delivery complete")
                    }
                }
            )
        }

        binding.disconnectBtn.setOnClickListener {
            // Disconnect from MQTT Broker if connected
            if (mqttClient.isConnected()){
                mqttClient.disconnect(object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "Disconnected from $MQTT_SERVER_URI")

                        val successMsg = "Disconnected from $MQTT_SERVER_URI"
                        Toast.makeText(this@MainActivity, successMsg,Toast.LENGTH_LONG).show()
                        binding.connectBtn.isEnabled  = true
                        binding.disconnectBtn.isEnabled = false

                        // disable the LED buttons
                        binding.liteonBtn.isEnabled = false
                        binding.liteoffBtn.isEnabled = false
                        binding.fanonBtn.isEnabled = false
                        binding.fanoffBtn.isEnabled = false
                        binding.pumponBtn.isEnabled = false
                        binding.pumpoffBtn.isEnabled = false
                    }
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d(TAG, "Failed to disconnect exception: ${exception.toString()}")

                        val failureMsg =
                            "Disconnect from $MQTT_SERVER_URI failed:${exception.toString()}"
                        Toast.makeText(this@MainActivity, failureMsg, Toast.LENGTH_LONG).show()
                        exception?.printStackTrace()
                    }
                } )
            } else {
                Log.d(TAG, "Impossible to disconnect, no server connected")
            }
        }
        //  change listeners
        binding.liteonBtn.setOnClickListener {
            // Publish the LiteOn message
            changeState(LITEON_MESSAGE, false, true)
        }
        binding.liteoffBtn.setOnClickListener {
            // Publish the LiteOff message
            changeState(LITEOFF_MESSAGE, true, false)
        }
        binding.fanonBtn.setOnClickListener {
            changeFanState(FANON_MESSAGE, false, true)
        }
        binding.fanoffBtn.setOnClickListener {
            changeFanState(FANOFF_MESSAGE, true, false)
        }
        binding.pumponBtn.setOnClickListener {
            changePumpState(PUMPON_MESSAGE, false, true)
        }
        binding.pumpoffBtn.setOnClickListener {
            changePumpState(PUMPOFF_MESSAGE, true, false)
        }

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                //here we can write some code to do something when progress is changed
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                //here we can write some code to do something whenever the user touche the seekbar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Publish the Interval message
                changeInterval(seekBar.progress.toString())
                // show some message after user stopped scrolling the seekbar
                Toast.makeText(this@MainActivity, "Discrete Value of Interval is  " + seekBar.progress +" secs", Toast.LENGTH_SHORT).show()
            }
        })


    }

    // helper functions

    override fun onTimeSet(p0: TimePicker?, hourOfDay: Int, minuteOfDay: Int) {
        currentHour = hourOfDay
        currentMinut = minuteOfDay
        val startTime = String.format("%02d",currentHour)+":"+String.format("%02d",currentMinut)
        binding.schTextView.setText(startTime)

        setLightSch(startTime)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isConnected() : Boolean {
        var result = false
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        if (capabilities != null) {
            result = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
                else -> false
            }
        }
        return result
    }

    private fun subscribeToRPiTopics(){
        // subscribe to status topic only if connected to broker
        if (mqttClient.isConnected()){
            mqttClient.subscribe( topic = CELCIUS_TOPIC, qos = 1)
            mqttClient.subscribe( topic = HUMIDITY_TOPIC, qos = 1)
            mqttClient.subscribe( topic = LIGHT_SENS_TOPIC, qos = 1)
            mqttClient.subscribe( topic = SOIL_TOPIC, qos = 1)
            mqttClient.subscribe( topic = WATER_LEVEL_TOPIC, qos = 1)
            mqttClient.subscribe( topic = FAN_TOPIC, qos = 1)
            mqttClient.subscribe( topic = PUMP_TOPIC, qos = 1)
            mqttClient.subscribe( topic = LIGHTS_MAT_TOPIC, qos = 1)
            mqttClient.subscribe( topic = SOIL_GRAPH_TOPIC, qos = 1)
            mqttClient.subscribe( topic = WATER_GRAPH_TOPIC, qos = 1)
            mqttClient.subscribe( topic = FAHR_GRAPH_TOPIC, qos = 1)
            mqttClient.subscribe( topic = HUMIDITY_GRAPH_TOPIC, qos = 1)
            mqttClient.subscribe( topic = LUM_GRAPH_TOPIC, qos = 1)
            mqttClient.subscribe( topic = MILDEW_DETECT, qos = 1)
            mqttClient.subscribe( topic = PLANT_IMAGE, qos = 1)

        } else {
            val msg = "Cannot subscribe to topics: Not connected to server"
            Log.d(TAG, msg)
            Toast.makeText(this@MainActivity,msg,Toast.LENGTH_LONG).show()
        }
    }


    private fun changeState(message: String, enableLiteOnBtn: Boolean, enableLiteOffBtn: Boolean){
        if (mqttClient.isConnected()){
            val topic = LITE_CON_TOPIC
            mqttClient.publish(
                topic,
                message,
                1,
                false,
                object : IMqttActionListener{
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        val msg = "Successfully published message: $message to topic: $topic"
                        Log.d(TAG, msg)
                        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_LONG).show()

                        binding.liteonBtn.isEnabled = enableLiteOnBtn
                        binding.liteoffBtn.isEnabled = enableLiteOffBtn
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val msg = "Failed to publish message: $message to topic: $topic exception: ${exception.toString()}"
                        Log.d(TAG, msg)
                        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else{
            Log.d(TAG, "Impossible to publish, no server connected")
        }
    }


    private fun changeFanState(message: String, enableOnBtn: Boolean, enableOffBtn: Boolean){
        if (mqttClient.isConnected()){
            val topic = FAN_CON_TOPIC
            mqttClient.publish(
                topic,
                message,
                2,
                false,
                object : IMqttActionListener{
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        val msg = "Successfully published message: $message to topic: $topic"
                        Log.d(TAG, msg)
                        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_LONG).show()

                        binding.fanonBtn.isEnabled = enableOnBtn
                        binding.fanoffBtn.isEnabled = enableOffBtn
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val msg = "Failed to publish message: $message to topic: $topic exception: ${exception.toString()}"
                        Log.d(TAG, msg)
                        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else{
            Log.d(TAG, "Impossible to publish, no server connected")
        }
    }


    private fun changePumpState(message: String, enableOnBtn: Boolean, enableOffBtn: Boolean){
        if (mqttClient.isConnected()){
            val topic = PUMP_CON_TOPIC
            mqttClient.publish(
                topic,
                message,
                2,
                false,
                object : IMqttActionListener{
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        val msg = "Successfully published message: $message to topic: $topic"
                        Log.d(TAG, msg)
                        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_LONG).show()

                        binding.pumponBtn.isEnabled = enableOnBtn
                        binding.pumpoffBtn.isEnabled = enableOffBtn
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val msg = "Failed to publish message: $message to topic: $topic exception: ${exception.toString()}"
                        Log.d(TAG, msg)
                        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else{
            Log.d(TAG, "Impossible to publish, no server connected")
        }
    }

    private fun setLightSch(message: String){
        if (mqttClient.isConnected()){
            val topic = SCHEDULE_TOPIC
            mqttClient.publish(
                topic,
                message,
                2,
                false,
                object : IMqttActionListener{
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        val msg = "Successfully published message: $message to topic: $topic"
                        Log.d(TAG, msg)
                        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_LONG).show()

                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val msg = "Failed to publish message: $message to topic: $topic exception: ${exception.toString()}"
                        Log.d(TAG, msg)
                        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else{
            Log.d(TAG, "Impossible to publish, no server connected")
        }
    }

    private fun changeInterval(message: String){
        if (mqttClient.isConnected()){
            val topic = INTERVAL_TOPIC
            mqttClient.publish(
                topic,
                message,
                1,
                false,
                object : IMqttActionListener{
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        val msg = "Successfully published message: $message to topic: $topic"
                        Log.d(TAG, msg)
                        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_LONG).show()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val msg = "Failed to publish message: $message to topic: $topic exception: ${exception.toString()}"
                        Log.d(TAG, msg)
                        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else{
            Log.d(TAG, "Impossible to publish, no server connected")
        }
    }

}
