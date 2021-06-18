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
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.Message
import com.riyazuddin.zing.databinding.ItemChatLeftBinding
import com.riyazuddin.zing.databinding.ItemChatRightBinding
import com.riyazuddin.zing.other.Constants
import com.riyazuddin.zing.other.Constants.DELIVERED
import com.riyazuddin.zing.other.Constants.SEEN
import com.riyazuddin.zing.other.Constants.SENDING
import com.riyazuddin.zing.other.Constants.SENT
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ChatAdapter @Inject constructor(
    private val glide: RequestManager,
    private val viewModel: ChatViewModel
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val MSG_TYPE_LEFT = 0
        const val MSG_TYPE_RIGHT = 1

        const val TAG = "ChatAdapter"
    }

    private val differCallback = object : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.messageId == newItem.messageId
        }
    }

    private val differ = AsyncListDiffer(this, differCallback)

    var messages: List<Message>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    inner class LeftViewHolder(val binding: ItemChatLeftBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class RightViewHolder(val binding: ItemChatRightBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]

        if (message.senderAndReceiverUid.size == 2) {
            return if (message.senderAndReceiverUid[0] == Firebase.auth.uid) {
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val date =
            SimpleDateFormat("hh:mm a", Locale.US).format(Date(message.date))
        if (message.senderAndReceiverUid.size == 2) {
            if (message.senderAndReceiverUid[0] == Firebase.auth.uid) {
                val rightViewHolder = RightViewHolder(ItemChatRightBinding.bind(holder.itemView))
                rightViewHolder.binding.apply {
                    tvDate.text = date

                    if (message.type == "DELETED") {
                        tvMessage.text = message.message
                        tvMessage.setTypeface(null, Typeface.ITALIC)
                        tvMessage.isVisible = true
                        return@apply
                    }

                    message.url.isNotEmpty().let {
                        if (message.type == Constants.IMAGE) {
                            glide.load(message.url).into(messageImage)
                            messageImage.isVisible = true
                        }
                    }
                    message.message.isNotEmpty().let {
                        tvMessage.text = message.message
                        tvMessage.isVisible = true
                    }
                    when (message.status) {
                        SENDING -> {
                            glide.load(R.drawable.ic_sending).into(ivStatus)
                            ivStatus.isVisible = true
                        }
                        SENT -> {
                            glide.load(R.drawable.ic_sent).into(ivStatus)
                            ivStatus.isVisible = true
                        }
                        DELIVERED -> {
                            glide.load(R.drawable.ic_delivered).into(ivStatus)
                            ivStatus.isVisible = true
                        }
                        SEEN -> {
                            glide.load(R.drawable.ic_seen_ticks).into(ivStatus)
                            ivStatus.isVisible = true
                        }
                    }
                    root.setOnLongClickListener {
                        if (message.senderAndReceiverUid[0] == Firebase.auth.uid) {
                            onItemLongClickListener?.let {
                                it(message, position)
                            }
                        }
                        true
                    }
                }
            } else {
                val leftViewHolder = LeftViewHolder(ItemChatLeftBinding.bind(holder.itemView))
                leftViewHolder.binding.apply {
                    tvDate.text = date

                    if (message.type == "DELETED") {
                        tvMessage.text = message.message
                        tvMessage.isVisible = true
                        return@apply
                    }

                    message.url.isNotEmpty().let {
                        if (message.type == Constants.IMAGE) {
                            glide.load(message.url).into(messageImage)
                            messageImage.isVisible = true
                        }
                    }
                    message.message.isNotEmpty().let {
                        tvMessage.text = message.message
                        tvMessage.isVisible = true
                    }
                    root.setOnLongClickListener {
                        if (message.senderAndReceiverUid[0] == Firebase.auth.uid) {
                            onItemLongClickListener?.let {
                                it(message, position)
                            }
                        }
                        true
                    }
                }
                if (message.status != SEEN) {
                    GlobalScope.launch(Dispatchers.IO) {
                        viewModel.updateMessageStatusAsSeen(message)
                        Log.i(TAG, "onBindViewHolder: invoked updateMessageStatusAsSeen()")
                    }
                }

            }
        }

    }

    private var onItemLongClickListener: ((Message, Int) -> Unit)? = null
    fun setOnItemLongClickListener(listener: (Message, Int) -> Unit) {
        onItemLongClickListener = listener
    }


    override fun getItemCount(): Int = messages.size

}