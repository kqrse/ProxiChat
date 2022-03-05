package com.kqrse.proxichat.logic

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ChatMessage(
    val senderName: String = "<sender>",
    val avatarFileName: String = "",
    val content: String = "<content>",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: String = "<timestamp>"
) : Parcelable