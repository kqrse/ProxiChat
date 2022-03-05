package com.kqrse.proxichat

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.kqrse.proxichat.databinding.ActivityChatChannelBinding
import com.kqrse.proxichat.logic.ChatMessage
import com.kqrse.proxichat.logic.ChatMessageAdapter
import kotlin.math.abs

class ChatChannelActivity: AppCompatActivity() {
    lateinit var binding: ActivityChatChannelBinding
    lateinit var database: FirebaseDatabase
    lateinit var myRef: DatabaseReference
    var channelID = -1
    lateinit var fusedLocation: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback
    lateinit var latLongArr: DoubleArray
    lateinit var displayName: String
    lateinit var avatarFileName: String
    lateinit var chatMessageList: ArrayList<ChatMessage>
    var requestingLocationUpdates = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatChannelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.chatChannelToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        latLongArr = DoubleArray(2)
        setLocationRequest()
        setLocationCallback()
        setFusedLocation()

        channelID = intent.getIntExtra("channelID", -1)
        setupChannel()

        database = Firebase.database
        myRef = database.getReference(channelID.toString())

        binding.chatChannelRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatChannelRecyclerView.setHasFixedSize(true)

        getChatData()

        binding.chatChannelSendButton.setOnClickListener { sendMessage() }
        binding.chatChannelMessageBox.setOnEditorActionListener{ _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                sendMessage()
                return@setOnEditorActionListener true
            }
            false
        }
        chatMessageList = ArrayList<ChatMessage>()
        binding.chatChannelRecyclerView.adapter = ChatMessageAdapter(chatMessageList,
            this@ChatChannelActivity, channelID)
        getUserData()
    }

    private fun setupChannel() {
        when (channelID) {
            0 -> {
                binding.chatChannelToolbar.title = "Red Channel"
                binding.chatChannelToolbar.background = ContextCompat.getDrawable(this, R.color.secondary_red)
                binding.chatChannelSendButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_red)
                binding.chatChannelMessageBox.backgroundTintList = ContextCompat.getColorStateList(this, R.color.accent_red)
            }
            1 -> {
                binding.chatChannelToolbar.title = "Blue Channel"
                binding.chatChannelToolbar.background = ContextCompat.getDrawable(this, R.color.secondary_blue)
                binding.chatChannelSendButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_blue)
                binding.chatChannelMessageBox.backgroundTintList = ContextCompat.getColorStateList(this, R.color.accent_blue)
            }
            2 -> {
                binding.chatChannelToolbar.title = "Green Channel"
                binding.chatChannelToolbar.background = ContextCompat.getDrawable(this, R.color.secondary_green)
                binding.chatChannelSendButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_green)
                binding.chatChannelMessageBox.backgroundTintList = ContextCompat.getColorStateList(this, R.color.accent_green)
            }
            3 -> {
                binding.chatChannelToolbar.title = "Yellow Channel"
                binding.chatChannelToolbar.background = ContextCompat.getDrawable(this, R.color.secondary_yellow)
                binding.chatChannelSendButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_yellow)
                binding.chatChannelMessageBox.backgroundTintList = ContextCompat.getColorStateList(this, R.color.accent_yellow)
            }
        }
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

                saveLocationData()

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
            saveLocationData()
        }
        fusedLocation.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        requestingLocationUpdates = true
    }

    private fun saveLocationData() {
        val sharedPref = this@ChatChannelActivity.getSharedPreferences("proxiChat", Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putFloat("latitude", latLongArr[0].toFloat())
            putFloat("longitude", latLongArr[1].toFloat())
            apply()
        }
    }

    private fun getUserData() {
        val sharedPrefs = this.getSharedPreferences("proxiChat", Context.MODE_PRIVATE) ?: return
        displayName = sharedPrefs.getString("displayName", "Unnamed").toString()
        avatarFileName = sharedPrefs.getString("displayPic", "").toString()
    }

    private fun getChatData() {
        myRef.child("messages").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("shark", "Current Location: [${latLongArr[0].toFloat()}][${latLongArr[1].toFloat()}]")
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(ChatMessage::class.java)

                        if (isMessageWithinTimeAndRange(message!!)) {
                            if (!chatMessageList.contains(message)) chatMessageList.add(message)
                        }

                    }

                    binding.chatChannelRecyclerView.adapter?.notifyDataSetChanged()

                    if ((binding.chatChannelRecyclerView.adapter as ChatMessageAdapter).itemCount > 1) {
                        binding.chatChannelRecyclerView.smoothScrollToPosition(
                            binding.chatChannelRecyclerView.adapter!!.itemCount-1)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
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
            Log.d("shark", "Message received from: [${message.latitude}][${message.longitude}]," +
                    " Time elapsed: ${abs(message.timestamp.toDouble()-System.currentTimeMillis())/1000f}s")
        }
        return isWithinRange and isWithinTime
    }

    private fun sendMessage() {
        if (binding.chatChannelMessageBox.text.isNotEmpty()) {
            val key = myRef.child("messages").push()

            val message = ChatMessage(
                displayName,
                avatarFileName,
                binding.chatChannelMessageBox.text.toString(),
                latLongArr[0],
                latLongArr[1],
                System.currentTimeMillis().toString()
            )

            key.setValue(message)
            binding.chatChannelMessageBox.text.clear()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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