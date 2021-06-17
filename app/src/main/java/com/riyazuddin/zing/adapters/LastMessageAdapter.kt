package com.riyazuddin.zing.adapters

import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.data.entities.LastMessage
import com.riyazuddin.zing.databinding.ItemRecentChatBinding
import com.riyazuddin.zing.other.Constants.IMAGE
import com.riyazuddin.zing.other.Constants.SEEN
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class LastMessageAdapter @Inject constructor(private val glide: RequestManager) :
    RecyclerView.Adapter<LastMessageAdapter.LastMessageViewHolder>() {

    inner class LastMessageViewHolder(val binding: ItemRecentChatBinding) :
        RecyclerView.ViewHolder(binding.root)

    private val differCallback = object : DiffUtil.ItemCallback<LastMessage>() {
        override fun areItemsTheSame(oldItem: LastMessage, newItem: LastMessage): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: LastMessage, newItem: LastMessage): Boolean {
            return oldItem.message.messageId == newItem.message.messageId
        }
    }

    private val differ = AsyncListDiffer(this, differCallback)

    var lastMessages: List<LastMessage>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LastMessageViewHolder {
        return LastMessageViewHolder(
            ItemRecentChatBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LastMessageViewHolder, position: Int) {
        val lastMessage = lastMessages[position]
        holder.binding.apply {
            val isCurrentUserIsSender =
                Firebase.auth.uid == lastMessage.message.senderAndReceiverUid[0]

            if (isCurrentUserIsSender) {
                Log.i(TAG, "onBindViewHolder: if")
                glide.load(lastMessage.receiver.profilePicUrl).into(CIVProfilePic)
                tvName.text = lastMessage.receiver.name
            } else {
                Log.i(TAG, "onBindViewHolder: else")
                glide.load(lastMessage.sender.profilePicUrl).into(CIVProfilePic)
                tvName.text = lastMessage.sender.name
            }


            if (!isCurrentUserIsSender && lastMessage.message.status != SEEN) {
                tvLastMessage.typeface = Typeface.DEFAULT_BOLD
                ivUnSeen.isVisible = true
            } else {
                tvLastMessage.typeface = Typeface.DEFAULT
                ivUnSeen.isVisible = false
            }

            val date =
                SimpleDateFormat("hh:mm a", Locale.US).format(Date(lastMessage.message.date))
            tvDate.text = date
            if (lastMessage.message.type == IMAGE) {
                val s = "🖼 Photo"
                tvLastMessage.text = s
            } else {
                tvLastMessage.text = lastMessage.message.message
            }
            root.setOnClickListener {
                onItemClickListener?.let {
                    it(lastMessage)
                }
            }
        }
    }

    override fun getItemCount(): Int = lastMessages.size

    private var onItemClickListener: ((LastMessage) -> Unit)? = null
    fun setOnItemClickListener(listener: (LastMessage) -> Unit) {
        onItemClickListener = listener
    }

    companion object {
        const val TAG = "LastMessageAdapter"
    }
}