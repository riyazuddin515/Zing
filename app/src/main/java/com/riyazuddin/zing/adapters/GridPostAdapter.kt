package com.riyazuddin.zing.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.*
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.databinding.ItemPostGridBinding
import javax.inject.Inject

class GridPostAdapter @Inject constructor(val glide: RequestManager) :
    RecyclerView.Adapter<GridPostAdapter.PostViewHolder>() {

    inner class PostViewHolder(val binding: ItemPostGridBinding) :
        RecyclerView.ViewHolder(binding.root)

    private val differCallback = object : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }

    }
    private val differ = AsyncListDiffer(this, differCallback)
    var posts: List<Post>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(
            ItemPostGridBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.binding.apply {
            val post = posts[position]
            glide.load(post.imageUrl).into(ivPostImage)

            root.setOnLongClickListener {
                onItemLongListener?.let {
                    it(post)
                }
                false
            }

        }
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    private var onItemLongListener: ((Post) -> Unit)? = null


    fun setItemOnLongListener(listener: (Post) -> Unit) {
        onItemLongListener = listener
    }


}