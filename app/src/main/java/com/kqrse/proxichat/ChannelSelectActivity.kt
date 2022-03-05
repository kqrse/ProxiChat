package com.kqrse.proxichat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.kqrse.proxichat.databinding.ActivityChannelSelectBinding
import com.kqrse.proxichat.logic.ChatMessage
import com.kqrse.proxichat.logic.ChatMessageAdapter
import kotlin.math.abs

class ChannelSelectActivity : AppCompatActivity() {
    lateinit var binding: ActivityChannelSelectBinding
    lateinit var latLongArr: DoubleArray
    lateinit var fusedLocation: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback
    var requestingLocationUpdates = false
    lateinit var database: FirebaseDatabase
//    lateinit var myRef: DatabaseReference

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ), 100)

        supportActionBar?.hide()

        latLongArr = DoubleArray(2)

        setLocationRequest()
        setLocationCallback()
        setFusedLocation()

        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        fusedLocation.lastLocation.addOnSuccessListener { loc : Location? ->
            val latLongArr = DoubleArray(2)
            latLongArr[0] = loc!!.latitude
            latLongArr[1] = loc.longitude
        }

        database = Firebase.database
        setupMessagesListener()

        binding.channelSelectCard1.setOnClickListener { navigateToChannel(0) }
        binding.channelSelectCard2.setOnClickListener { navigateToChannel(1) }
        binding.channelSelectCard3.setOnClickListener { navigateToChannel(2) }
        binding.channelSelectCard4.setOnClickListener { navigateToChannel(3) }
        binding.channelSelectSettingsCard.setOnClickListener { navigateToSettings() }
    }

    private fun setupMessagesListener() {
        for (i in 0..3) {
            val myRef = database.getReference(i.toString())
            myRef.child("messages").addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d("shark", "Current Location: [${latLongArr[0].toFloat()}][${latLongArr[1].toFloat()}]")
                        var messageExists = false

                        for (messageSnapshot in snapshot.children) {
                            val message = messageSnapshot.getValue(ChatMessage::class.java)

                            if (isMessageWithinTimeAndRange(message!!)) {
                                when (i) {
                                    0 -> binding.channelSelectRedChatBubble.visibility = View.VISIBLE
                                    1 -> binding.channelSelectBlueChatBubble.visibility = View.VISIBLE
                                    2 -> binding.channelSelectGreenChatBubble.visibility = View.VISIBLE
                                    3 -> binding.channelSelectYellowChatBubble.visibility = View.VISIBLE
                                }
                                Log.d("shark", "Room ID: ${i} has an active message")
                                messageExists = true
                                break
                            }
                        }
                        if (!messageExists) {
                            when (i) {
                                0 -> binding.channelSelectRedChatBubble.visibility = View.INVISIBLE
                                1 -> binding.channelSelectBlueChatBubble.visibility = View.INVISIBLE
                                2 -> binding.channelSelectGreenChatBubble.visibility = View.INVISIBLE
                                3 -> binding.channelSelectYellowChatBubble.visibility = View.INVISIBLE
                            }
                            Log.d("shark", "Room ID: $i has no active messages")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
        }
    }

    private fun isMessageWithinTimeAndRange(message: ChatMessage): Boolean {
        val sharedPrefs = this.getSharedPreferences("proxiChat", Context.MODE_PRIVATE)
        val lat = sharedPrefs.getFloat("latitude", 0f)
        val long = sharedPrefs.getFloat("longitude", 0f)

        val distance = FloatArray(2)
        Location.distanceBetween(message.latitude, message.longitude, lat.toDouble(),
            long.toDouble(), distance)

        // Message is sent within last 10 minutes upon loading channel
        val isWithinTime = abs(message.timestamp.toDouble()-System.currentTimeMillis()) < 600000
        val isWithinRange = 100.0.toLong() > distance[0].toLong()

        if (isWithinRange and isWithinTime) {
//            Log.d("shark", "Time elapsed: " +
//                    (abs(message.timestamp.toDouble()-System.currentTimeMillis())/1000f).toString() +
//                    "s. ----- Distance: " + distance[0].toLong().toString() + "m")
            Log.d("shark", "Message received from: [${message.latitude}][${message.longitude}]," +
                    " Time elapsed: ${abs(message.timestamp.toDouble()-System.currentTimeMillis()) /1000f}s")
        }
        return isWithinRange and isWithinTime
    }

    private fun navigateToSettings() {
        val intent: Intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToChannel(channelID: Int) {
        val intent: Intent = Intent(this, ChatChannelActivity::class.java)
        intent.putExtra("channelID", channelID)
        startActivity(intent)
    }

    private fun setLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 500
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun setLocationCallback() {
        locationCallback = object: LocationCallback () {
            override fun onLocationResult(locResult: LocationResult) {
                super.onLocationResult(locResult)
                locResult ?: return
                latLongArr[0] = locResult.lastLocation.latitude
                latLongArr[1] = locResult.lastLocation.longitude

                Log.d("shark", "LOCATION UPDATED: [${latLongArr[0]}][${latLongArr[1]}]")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setFusedLocation() {
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        fusedLocation.lastLocation.addOnSuccessListener { loc : Location ->
            latLongArr[0] = loc.latitude
            latLongArr[1] = loc.longitude
        }
        fusedLocation.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        requestingLocationUpdates = true
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocation.removeLocationUpdates(locationCallback)
        requestingLocationUpdates = false
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocation.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
        requestingLocationUpdates = true
    }
}