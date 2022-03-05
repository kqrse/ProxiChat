package com.kqrse.proxichat.logic

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.kqrse.proxichat.R
import com.kqrse.proxichat.databinding.CardChatMessageBinding
import com.kqrse.proxichat.databinding.CardChatMessageSameSenderBinding

class ChatMessageAdapter(private val chatMessageList : ArrayList<ChatMessage>,
                         private val context: Context, private val channelID: Int)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class DiffSenderMessage (binding : CardChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        val senderName: TextView = binding.chatMessageSender
        val content: TextView = binding.chatMessageContent
        val avatar: ImageView = binding.chatMessageAvatar
        val card: CardView = binding.chatMessageCard
    }

    inner class SameSenderMessage (binding : CardChatMessageSameSenderBinding) : RecyclerView.ViewHolder(binding.root) {
        val content: TextView = binding.sameSenderContent
        val card: CardView = binding.sameSenderCard
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return 0
        if ((chatMessageList[position-1].senderName == chatMessageList[position].senderName) and
            (chatMessageList[position-1].avatarFileName == chatMessageList[position].avatarFileName)) return 1
        else return 0
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                val binding = CardChatMessageBinding.inflate(
                    LayoutInflater.from(viewGroup.context), viewGroup, false
                )
                DiffSenderMessage(binding)
            }
            else -> {
                val binding = CardChatMessageSameSenderBinding.inflate(
                    LayoutInflater.from(viewGroup.context), viewGroup, false
                )
                SameSenderMessage(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = chatMessageList[position]
        when (getItemViewType(position)) {
            0 -> {
                val diffSenderMessage = holder as DiffSenderMessage
                diffSenderMessage.senderName.text = currentItem.senderName
                diffSenderMessage.content.text = currentItem.content

                if (currentItem.avatarFileName.isEmpty()) {
                    diffSenderMessage.avatar.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.default_avatar))
                    Log.d("shark", "No avatar available")
                } else {
                    FirebaseStorage.getInstance().getReference("image/${currentItem.avatarFileName}").downloadUrl.addOnSuccessListener { uri: Uri ->
                        Glide.with(context)
                            .load(uri.toString())
                            .into(diffSenderMessage.avatar)
                        Log.d("shark", "Loaded avatar")
                    }.addOnFailureListener {
                        diffSenderMessage.avatar.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.default_avatar))
                        Log.d("shark", "Failed to load avatar")
                    }
                }

                when (channelID) {
                    0 -> diffSenderMessage.card.backgroundTintList = ContextCompat.getColorStateList(context, R.color.accent_red)
                    1 -> diffSenderMessage.card.backgroundTintList = ContextCompat.getColorStateList(context, R.color.accent_blue)
                    2 -> diffSenderMessage.card.backgroundTintList = ContextCompat.getColorStateList(context, R.color.accent_green)
                    3 -> diffSenderMessage.card.backgroundTintList = ContextCompat.getColorStateList(context, R.color.accent_yellow)
                }
            }
            else -> {
                val sameSenderMessage = holder as SameSenderMessage
                sameSenderMessage.content.text = currentItem.content
                when (channelID) {
                    0 -> sameSenderMessage.card.backgroundTintList = ContextCompat.getColorStateList(context, R.color.accent_red)
                    1 -> sameSenderMessage.card.backgroundTintList = ContextCompat.getColorStateList(context, R.color.accent_blue)
                    2 -> sameSenderMessage.card.backgroundTintList = ContextCompat.getColorStateList(context, R.color.accent_green)
                    3 -> sameSenderMessage.card.backgroundTintList = ContextCompat.getColorStateList(context, R.color.accent_yellow)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return chatMessageList.size
    }


}