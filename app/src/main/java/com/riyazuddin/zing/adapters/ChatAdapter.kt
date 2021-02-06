package com.riyazuddin.zing.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.databinding.ItemChatLeftBinding
import com.riyazuddin.zing.databinding.ItemChatRightBinding
import com.riyazuddin.zing.other.Constants.IMAGE
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(val glide: RequestManager, options: FirestoreRecyclerOptions<Message>) :
    FirestoreRecyclerAdapter<Message, RecyclerView.ViewHolder>(options) {

    companion object {
        const val MSG_TYPE_LEFT = 0
        const val MSG_TYPE_RIGHT = 1
    }

    inner class LeftViewHolder(val binding: ItemChatLeftBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class RightViewHolder(val binding: ItemChatRightBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)

        if (message.senderAddReceiverUid.size == 2) {
            return if (message.senderAddReceiverUid[0] == Firebase.auth.uid) {
                MSG_TYPE_RIGHT
            } else MSG_TYPE_LEFT
        }
        return -1
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == MSG_TYPE_RIGHT) {
            return RightViewHolder(
                ItemChatRightBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        return LeftViewHolder(
            ItemChatLeftBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        message: Message
    ) {
        val date =
            SimpleDateFormat("hh:mm a", Locale.US).format(Date(message.date))
        if (message.senderAddReceiverUid.size == 2) {
            if (message.senderAddReceiverUid[0] == Firebase.auth.uid!!) {
                val rightViewHolder = RightViewHolder(ItemChatRightBinding.bind(holder.itemView))
                rightViewHolder.binding.apply {
                    message.message.isNotEmpty().let {
                        tvMessage.text = message.message
                        tvMessage.isVisible = true
                    }
                    message.url.isNotEmpty().let {
                        if (message.type == IMAGE) {
                            glide.load(message.url).into(messageImage)
                            messageImage.isVisible = true
                            if (message.message.isEmpty())
                                tvMessage.isVisible = false
                        }
                    }
                    tvDate.text = date
                    root.setOnLongClickListener {
                        onItemLongClickListener?.let {
                            it(message)
                        }
                        true
                    }
                }
            } else {
                val leftViewHolder = LeftViewHolder(ItemChatLeftBinding.bind(holder.itemView))
                leftViewHolder.binding.apply {
                    message.message.isNotEmpty().let {
                        tvMessage.text = message.message
                        tvMessage.isVisible = true
                    }
                    tvDate.text = date
                    message.url.isNotEmpty().let {
                        if (message.type == IMAGE) {
                            glide.load(message.url).into(messageImage)
                            messageImage.isVisible = true
                        }
                    }
                    root.setOnLongClickListener {
                        onItemLongClickListener?.let {
                            it(message)
                        }
                        true
                    }
                }
            }
        }

    }

    private var onItemLongClickListener: ((Message) -> Unit)? = null
    fun setOnItemLongClickListener(listener: (Message) -> Unit) {
        onItemLongClickListener = listener
    }
}