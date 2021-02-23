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
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.databinding.ItemPostBinding
import javax.inject.Inject

class PostAdapter @Inject constructor(val glide: RequestManager) :
    PagingDataAdapter<Post, PostAdapter.PostViewHolder>(Companion) {

    class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    companion object : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(
            ItemPostBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.binding.apply {
            val post = getItem(position) ?: return
            glide.load(post.userProfilePic).into(CIVProfilePic)
            glide.load(post.imageUrl).into(ivPostImage)
            tvUsername.text = post.username
            val likeCount = post.likeCount
            tvLikedBy.text = when {
                likeCount <= 0 -> "No Likes"
                likeCount == 1 -> "Liked by 1 Person"
                else -> "Liked by $likeCount persons"
            }
            if (post.caption.isEmpty())
                tvCaption.isVisible = false
            else tvCaption.text = post.caption
            ibDelete.isVisible = (post.postedBy == Firebase.auth.uid!!)
            ibLike.setImageResource(
                if (post.isLiked) R.drawable.ic_like_red
                else R.drawable.ic_outline_like
            )
            rootLayout.isVisible = true
            ibLike.setOnClickListener {
                onLikeClickListener?.let { click ->
                    click(post, position)
                }
            }
            ibDelete.setOnClickListener {
                onDeleteClickListener?.let { click ->
                    click(post)
                }
            }
            CIVProfilePic.setOnClickListener {
                onUserClickListener?.let { click ->
                    click(post)
                }
            }
            tvUsername.setOnClickListener {
                onUserClickListener?.let { click ->
                    click(post)
                }
            }
            tvLikedBy.setOnClickListener {
                onLikedByClickListener?.let { click ->
                    click(post)
                }
            }
            ibComment.setOnClickListener {
                onCommentClickListener?.let { click ->
                    click(post)
                }
            }
        }
    }


    private var onLikeClickListener: ((Post, Int) -> Unit)? = null
    private var onDeleteClickListener: ((Post) -> Unit)? = null
    private var onLikedByClickListener: ((Post) -> Unit)? = null
    private var onCommentClickListener: ((Post) -> Unit)? = null
    private var onUserClickListener: ((Post) -> Unit)? = null

    fun setOnLikeClickListener(listener: (Post, Int) -> Unit) {
        onLikeClickListener = listener
    }

    fun setOnDeleteClickListener(listener: (Post) -> Unit) {
        onDeleteClickListener = listener
    }

    fun setOnLikedByClickListener(listener: (Post) -> Unit) {
        onLikedByClickListener = listener
    }

    fun setOnCommentClickListener(listener: (Post) -> Unit) {
        onCommentClickListener = listener
    }

    fun setOnUserClickListener(listener: (Post) -> Unit) {
        onUserClickListener = listener
    }


}