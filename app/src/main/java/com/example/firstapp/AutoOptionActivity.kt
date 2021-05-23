package com.example.firstapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.firstapp.MQTT.MqttHandler


class AutoOptionActivity : AppCompatActivity(), View.OnClickListener {

    private var mqttHandler: MqttHandler? = null
    private var mPatternOne: Button? = null
    private var mPatternTwo: Button? = null
    private var mStartBtn: Button? = null
    private var mSizeField: EditText? = null
    private var mSeekBar: SeekBar? = null
    private var mSpeedText : TextView? = null

    //Messages
    private val PATTERN_ONE = 1
    private val PATTERN_TWO = 2
    private var mSpeed = 0
    private var mPattern = 0
    private var mSize = 0

    // seekbar pointers
    private var mStartPoint = 0
    private var mEndPoint = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_option)

        mPatternOne = findViewById(R.id.pattern1)
        mPatternTwo = findViewById(R.id.pattern2)
        mSpeedText = findViewById(R.id.velocity)
        mSeekBar = findViewById(R.id.seekBar)
        mStartBtn = findViewById(R.id.start_cleaning)

        mPatternOne?.setOnClickListener(this)
        mPatternTwo?.setOnClickListener(this)
        mStartBtn?.setOnClickListener(this)

        val actionBar = supportActionBar
        actionBar!!.title = ""

        //mqtt car handler
        mqttHandler = MqttHandler(this.applicationContext)
        mqttHandler!!.connectToMqttBroker()

        //Seekbar to get input from user regarding velocity
        mSeekBar?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                mSpeedText?.text = progress.toString()
                mSpeed = Integer.parseInt(mSpeedText?.text as String)
            }
            override fun onStartTrackingTouch(seek: SeekBar) {
                mStartPoint = mSeekBar!!.progress
            }
            override fun onStopTrackingTouch(seek: SeekBar) {
                mEndPoint = mSeekBar!!.progress
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.pattern1 -> {
                v.setBackgroundColor(Color.LTGRAY)
                mPattern = PATTERN_ONE
                mPatternTwo?.setBackgroundColor(Color.WHITE)
            }
            R.id.pattern2 ->{
                v.setBackgroundColor(Color.LTGRAY)
                mPattern = PATTERN_TWO
                mPatternOne?.setBackgroundColor(Color.WHITE)
            }
            R.id.stop ->{
                mqttHandler!!.driveAuto(0,0,0,"")
            }
            R.id.start_cleaning ->{
                getSizeInput()
                sendMessages(mSpeed, mPattern, mSize)
            }
        }
    }

    private fun getSizeInput(){
        mSizeField = findViewById(R.id.size_input);
        var temp: String = mSizeField?.text.toString()
        var value: Int
        if ("" != temp) {
            value = Integer.parseInt(temp)
            mSize = value
        }
    }

    private fun sendMessages(speed : Int, pattern : Int, size : Int) {
        if (speed != 0 && pattern != 0 && size != 0 ){
            mqttHandler!!.driveAuto(speed, pattern, size, "")
        }
    }
}