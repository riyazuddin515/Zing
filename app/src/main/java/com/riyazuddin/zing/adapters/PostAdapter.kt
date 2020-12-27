package com.riyazuddin.zing.adapters

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
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.databinding.ItemPostBinding
import javax.inject.Inject

class PostAdapter @Inject constructor(val glide: RequestManager) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        val binding = ItemPostBinding.bind(itemView)
    }

    private val differCallback = object : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }
    }
    private val differ = AsyncListDiffer(this, differCallback)

    var posts: List<Post>
        get() = differ.currentList
        set(value) = differ.submitList(value)

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
        val post = posts[position]
        holder.apply {
            glide.load(post.userProfilePic).into(binding.CIVProfilePic)
            glide.load(post.imageUrl).into(binding.ivPostImage)
            binding.tvUsername.text = post.username
            val likeCount = post.likedBy.size
            binding.tvLikedBy.text = when {
                likeCount <= 0 -> "No Likes"
                likeCount == 1 -> "Liked by 1 Person"
                else -> "Liked by $likeCount persons"
            }
            if (post.caption.isEmpty())
                binding.tvCaption.isVisible = false
            else binding.tvCaption.text = post.caption
            binding.ibDelete.isVisible = (post.authorUid == Firebase.auth.uid!!)
            binding.ibLike.setImageResource(
                if (post.isLiked) R.drawable.ic_like_red
                else R.drawable.ic_outline_like
            )

            binding.ibLike.setOnClickListener {
                onLikeClickListener?.let { click ->
                    click(post, position)
                }
            }
            binding.ibDelete.setOnClickListener {
                onDeleteClickListener?.let { click ->
                    click(post)
                }
            }
            binding.CIVProfilePic.setOnClickListener {
                onUserClickListener?.let { click ->
                    click(post)
                }
            }
            binding.tvUsername.setOnClickListener {
                onUserClickListener?.let { click ->
                    click(post)
                }
            }
            binding.tvLikedBy.setOnClickListener {
                onLikedByClickListener?.let { click ->
                    click(post)
                }
            }
            binding.ibComment.setOnClickListener {
                onCommentClickListener?.let { click ->
                    click(post)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return posts.size
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