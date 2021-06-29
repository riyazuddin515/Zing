package com.riyazuddin.zing.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.riyazuddin.zing.data.entities.Post
import com.riyazuddin.zing.databinding.ItemPostGridBinding
import javax.inject.Inject

class PostAdapterStaggered @Inject constructor(
    val glide: RequestManager
) : PagingDataAdapter<Post, PostAdapterStaggered.PostAdapterStaggeredViewHolder>(Companion) {

    inner class PostAdapterStaggeredViewHolder(private val binding: ItemPostGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bing(post: Post) {
            glide.load(post.imageUrl).into(binding.image)
        }
    }

    companion object : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }
    }

    override fun onBindViewHolder(holder: PostAdapterStaggeredViewHolder, position: Int) {
        holder.bing(getItem(position) ?: return)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PostAdapterStaggeredViewHolder {
        return PostAdapterStaggeredViewHolder(
            ItemPostGridBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }
}