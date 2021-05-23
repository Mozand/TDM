package com.example.firstapp.MQTT
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Color.RED
import android.util.Log
 import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firstapp.R
import org.eclipse.paho.client.mqttv3.*

class MqttHandler : AppCompatActivity {

        //connection to Mqtt
        private val TAG = "app"
        private val EXTERNAL_MQTT_BROKER = "aerostun.dev"
        private val LOCALHOST = "10.0.2.2"
        private val MQTT_SERVER = "tcp://" + EXTERNAL_MQTT_BROKER + ":1883"
        private val QOS = 1
        private var isConnected = false
        private var context: Context? = null

        //Subscription topics
        private val CAMERA_SUB = "/smartcar/group16/camera"
        private val ULTRASOUND_SUB = "/smartcar/group16/obstacleMsg"
        private val TRAVELED_DIS = "/smartcar/group16/distance"
        private val SPEED_SUB = "/smartcar/group16/speed"
        private val EMPTYBAG="smartcar/group16/Emptybag"

        // Publishing topics
        private val THROTTLE_CONTROL = "/smartcar/group16/control/throttle"
        private val STEERING_CONTROL = "/smartcar/group16/control/steering"
        private val AUTO_SPEED = "/smartcar/group16/auto/speed"
        private val AUTO_PATTERN = "/smartcar/group16/auto/pattern"
        private val AUTO_SIZE = "/smartcar/group16/auto/size"
        private val AUTO_START = "/smartcar/group16/auto/start"

        // Camera
        private val IMAGE_WIDTH = 320
        private val IMAGE_HEIGHT = 240

        //messages related to connection to mqtt broker
        private val SUCCESSFUL_CONNECTION = "Connected to MQTT broker"
        private val FAILED_CONNECTION = "Failed to connect to MQTT broker"
        private val LOST_CONNECTION = "Connection to MQTT broker lost"
        private val DISCONNECTED = "Disconnected from broker"

        private var mCameraView: ImageView? = null
        private var mMqttClient: MqttClient? = null
        private var mTraveledDistance: TextView? = null
        private var mSpeed: TextView? = null
        private var mFront: TextView? = null
        private var mBagfull:Int?=null

        //Constructors
        constructor(context: Context?, mCameraView: ImageView?) {
            mMqttClient = MqttClient(context, MQTT_SERVER, TAG)
            this.mCameraView = mCameraView
            this.context = context
        }

        constructor(context: Context?) {
            mMqttClient = MqttClient(context, MQTT_SERVER, TAG)
            this.context = context
        }

        constructor(context: Context?, mTraveledDistance : TextView?, mSpeed : TextView?, mFront : TextView?) {
            mMqttClient = MqttClient(context, MQTT_SERVER, TAG)
            this.context = context
            this.mTraveledDistance = mTraveledDistance
            this.mSpeed = mSpeed
            this.mFront = mFront
        }
        constructor(context:Context?,mBagfull:Int?){
            mMqttClient = MqttClient(context, MQTT_SERVER, TAG)
            this.context=context
            this.mBagfull=mBagfull
            }



        override fun onResume() {
            connectToMqttBroker()
            super.onResume()
        }

        override fun onPause() {
            super.onPause()
            mMqttClient?.disconnect(object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.i(TAG, DISCONNECTED)
                }
                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {}
            })
        }

        fun connectToMqttBroker() {
            if (!isConnected) {
                mMqttClient?.connect(TAG, "", object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken) {
                        isConnected = true
                        Log.i(TAG, SUCCESSFUL_CONNECTION)
                        // Toast.makeText(getApplicationContext(), successfulConnection, Toast.LENGTH_SHORT).show();

                        mMqttClient?.subscribe(ULTRASOUND_SUB, QOS, null)
                        mMqttClient?.subscribe(CAMERA_SUB, QOS, null)
                        mMqttClient?.subscribe(TRAVELED_DIS, QOS, null)
                        mMqttClient?.subscribe(SPEED_SUB, QOS, null)
                        mMqttClient?.subscribe(EMPTYBAG, QOS, null)

                    }

                    override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                        Log.e(TAG, FAILED_CONNECTION)
                        //Toast.makeText(getApplicationContext(), failedConnection, Toast.LENGTH_SHORT).show();
                    }
                }, object : MqttCallback {
                    override fun connectionLost(cause: Throwable) {
                        isConnected = false
                        Log.w(TAG, LOST_CONNECTION)
                        //Toast.makeText(getApplicationContext(), connectionLost, Toast.LENGTH_SHORT).show();
                    }

                    @Throws(Exception::class)
                    override fun messageArrived(topic: String, message: MqttMessage) {
                        if (topic == CAMERA_SUB) {
                            val bm =
                                Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
                            val payload = message.payload
                            val colors = IntArray(IMAGE_WIDTH * IMAGE_HEIGHT)
                            for (ci in colors.indices) {
                                val r = payload[3 * ci]
                                val g = payload[3 * ci + 1]
                                val b = payload[3 * ci + 2]
                                colors[ci] = Color.rgb(r.toInt(), g.toInt(), b.toInt())
                            }
                            bm.setPixels(colors, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT)

                            mCameraView?.setImageBitmap(bm)
                        }
                        if (topic == TRAVELED_DIS) {
                            val distance = message.toString()
                            mTraveledDistance?.setText(distance + " m")
                        }
                        if (topic == ULTRASOUND_SUB) {
                            val ultraSound = message.toString()
                            if (ultraSound > 1.toString()) {
                                mFront?.setText("WARNING")
                                mFront?.setTextColor(RED)
                            }else{
                                mFront?.setText("")
                            }
                        }
                        if (topic == SPEED_SUB) {
                            val speed = message.toString()
                            mSpeed?.setText(speed)

                        }
                        if(topic == EMPTYBAG) {
                            val bagStatus = message.toString()
                            mBagfull = bagStatus.toInt()

                        }else {
                            Log.i(
                                TAG,
                                "[MQTT] Topic: $topic | Message: $message"
                            )
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken) {
                        Log.d(TAG, "Message delivered")
                    }
                })
            }
        }


        fun publish(topic: String?, message: String?, qos: Int, publishCallback: IMqttActionListener?) {
            if (message != null) {
                mMqttClient?.publish(topic, message, qos, publishCallback)
            }
        }

        fun subscribe(topic: String?, qos: Int, subscriptionCallback: IMqttActionListener?) {
            mMqttClient?.subscribe(topic, qos, subscriptionCallback)
        }

        fun notConnected(){
            if (!isConnected) {
                val notConnected = "Not connected (yet)"
                Log.e(TAG, notConnected)
                Toast.makeText(context!!.applicationContext, notConnected, Toast.LENGTH_SHORT).show()
                return
            }
        }

        fun drive(throttleSpeed: Int, steeringAngle: Int, actionDescription: String?) {
            notConnected()
            Log.i(TAG, actionDescription!!)
            mMqttClient?.publish(THROTTLE_CONTROL, Integer.toString(throttleSpeed), QOS, null)
            mMqttClient?.publish(STEERING_CONTROL, Integer.toString(steeringAngle), QOS, null)
        }


        fun sendSpeed(speed : Int, actionDescription: String?) {
            notConnected()
            Log.i(TAG, actionDescription!!)
            mMqttClient?.publish(AUTO_SPEED, Integer.toString(speed), QOS, null)
        }

        fun sendPattern(pattern : Int, actionDescription: String?) {
            notConnected()
            Log.i(TAG, actionDescription!!)
            mMqttClient?.publish(AUTO_PATTERN, Integer.toString(pattern), QOS, null)
        }

        fun sendSize(size : Int, actionDescription: String?){
            notConnected()
            Log.i(TAG, actionDescription!!)
            mMqttClient?.publish(AUTO_SIZE, Integer.toString(size), QOS, null)
        }

       fun startCleaning(msg : String, actionDescription: String?){
        notConnected()
        Log.i(TAG, actionDescription!!)
        mMqttClient?.publish(AUTO_START, msg, QOS, null)
    }


    }



