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
import kotlin.collections.HashMap

class LastMessageAdapter @Inject constructor(
    private val glide: RequestManager
) : RecyclerView.Adapter<LastMessageAdapter.LastMessageViewHolder>() {

    private var simpleDateFormat: SimpleDateFormat = SimpleDateFormat("hh:mm a", Locale.US)

    inner class LastMessageViewHolder(val binding: ItemRecentChatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(lastMessage: LastMessage) {
            if (hashMap.containsKey(lastMessage.chatThread) and (hashMap[lastMessage.chatThread] == false)){
                hashMap[lastMessage.chatThread] = true
                userSyncListener?.let {
                    it(lastMessage.chatThread, lastMessage.otherUser.uid)
                    Log.i(TAG, "bind: ${lastMessage.otherUser.username} Syncing...")
                }
            }
            binding.apply {
                val isCurrentUserIsSender =
                    Firebase.auth.uid == lastMessage.message.senderAndReceiverUid[0]

                glide.load(lastMessage.otherUser.profilePicUrl).into(CIVProfilePic)
                tvName.text = lastMessage.otherUser.name

                if (!isCurrentUserIsSender && lastMessage.message.status != SEEN) {
                    tvLastMessage.typeface = Typeface.DEFAULT_BOLD
                    ivUnSeen.isVisible = true
                } else {
                    tvLastMessage.typeface = Typeface.DEFAULT
                    ivUnSeen.isVisible = false
                }

                tvDate.text = simpleDateFormat.format(lastMessage.message.date ?: Date())
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
    }

    private val differCallback = object : DiffUtil.ItemCallback<LastMessage>() {
        override fun areItemsTheSame(oldItem: LastMessage, newItem: LastMessage): Boolean {
            return oldItem.chatThread == newItem.chatThread
        }

        override fun areContentsTheSame(oldItem: LastMessage, newItem: LastMessage): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, differCallback)

    private val hashMap = HashMap<String,Boolean>()

    var lastMessages: List<LastMessage>
        get() = differ.currentList
        set(value){
            if (hashMap.isEmpty()){
                for (e in value)
                    hashMap[e.chatThread] = false
                differ.submitList(value)
            }
            else{
                for (e in value) {
                    if (!hashMap.containsKey(e.chatThread))
                        hashMap[e.chatThread] = true
                }
                differ.submitList(value)
            }
        }

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
        holder.bind(lastMessages[position])
    }

    override fun getItemCount(): Int = lastMessages.size

    private var onItemClickListener: ((LastMessage) -> Unit)? = null
    fun setOnItemClickListener(listener: (LastMessage) -> Unit) {
        onItemClickListener = listener
    }

    private var userSyncListener: ((String, String) -> Unit)? = null
    fun setUserSyncListener(listener: (String, String) -> Unit) {
        userSyncListener = listener
    }

    companion object {
        const val TAG = "LastMessageAdapter"
    }
}