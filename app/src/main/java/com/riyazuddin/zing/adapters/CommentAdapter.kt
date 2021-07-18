package com.riyazuddin.zing.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.data.entities.Comment
import com.riyazuddin.zing.databinding.ItemCommentBinding
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class CommentAdapter @Inject constructor(private val glide: RequestManager) :
    PagingDataAdapter<Comment, CommentAdapter.CommentViewHolder>(Companion) {

    private val currentUserUid: String = Firebase.auth.uid!!

    inner class CommentViewHolder(val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.commentId == newItem.commentId
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder(
            ItemCommentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.binding.apply {
            val comment = getItem(position) ?: return
            glide.load(comment.userProfilePic).into(CIVProfilePic)
            tvUsername.text = comment.username
            tvCommentText.text = comment.comment
            val date =
                SimpleDateFormat("d MMM yy hh:mm a", Locale.ENGLISH).format(comment.date ?: Date())
            tvTime.text = date

            CIVProfilePic.setOnClickListener {
                onUserClickListener?.let { click ->
                    click(comment)
                }
            }
            tvUsername.setOnClickListener {
                onUserClickListener?.let { click ->
                    click(comment)
                }
            }

            if (comment.commentedBy == currentUserUid) {
                btnDelete.isVisible = true
                btnDelete.setOnClickListener {
                    onCommentDeleteClickListener?.let {
                        it(comment)
                    }
                }
            }
        }
    }

    private var onCommentDeleteClickListener: ((Comment) -> Unit)? = null
    fun setOnCommentDeleteClickListener(listener: (Comment) -> Unit) {
        onCommentDeleteClickListener = listener
    }

    private var onUserClickListener: ((Comment) -> Unit)? = null
    fun setOnUserClickListener(listener: (Comment) -> Unit) {
        onUserClickListener = listener
    }
}