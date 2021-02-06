package com.riyazuddin.zing.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.data.entities.LastMessage
import com.riyazuddin.zing.databinding.ItemChatListBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    options: FirestoreRecyclerOptions<LastMessage>,
    val glide: RequestManager
) :
    FirestoreRecyclerAdapter<LastMessage, ChatListAdapter.ChatListViewHolder>(options) {

    inner class ChatListViewHolder(val binding: ItemChatListBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        return ChatListViewHolder(
            ItemChatListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int, model: LastMessage) {
        holder.binding.apply {
            val date =
                SimpleDateFormat("hh:mm a", Locale.US).format(Date(model.date))

            if (model.senderAddReceiverUid[0] == Firebase.auth.uid) {
                tvName.text = model.receiverName
                tvLastMessage.text = model.message
                tvDate.text = date
                glide.load(model.receiverProfilePicUrl).into(CIVProfilePic)
                chatItemCardView.setOnClickListener {
                    onItemClickListener?.let {
                        it(
                            model.senderAddReceiverUid[1],
                            model.receiverName,
                            model.receiverUsername,
                            model.receiverProfilePicUrl
                        )
                    }
                }
            } else {
                tvName.text = model.senderName
                tvLastMessage.text = model.message
                tvDate.text = date
                glide.load(model.senderProfilePicUrl).into(CIVProfilePic)
                chatItemCardView.setOnClickListener {
                    onItemClickListener?.let {
                        it(
                            model.senderAddReceiverUid[0],
                            model.senderName,
                            model.senderUserName,
                            model.senderProfilePicUrl
                        )
                    }
                }
            }
        }
    }

    private var onItemClickListener: ((String, String, String, String) -> Unit)? = null
    fun setOnItemClickListener(listener: (String, String, String, String) -> Unit) {
        onItemClickListener = listener
    }
}