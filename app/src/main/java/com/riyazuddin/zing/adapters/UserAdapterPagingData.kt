package com.riyazuddin.zing.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.ItemUserBinding
import javax.inject.Inject

class UserAdapterPagingData @Inject constructor(val glide: RequestManager) :
    PagingDataAdapter<User, UserAdapterPagingData.UserViewHolder>(Companion) {

    class UserViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    companion object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(
            ItemUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.binding.apply {
            val user = getItem(position) ?: return@apply
            glide.load(user.profilePicUrl).into(CIVProfilePic)
            tvName.text = user.name
            tvUsername.text = user.username
            userItemLayout.setOnClickListener {
                onUserClickListener?.let { click ->
                    click(user)
                }
            }
        }
    }

    private var onUserClickListener: ((User) -> Unit)? = null
    fun setOnUserClickListener(listener: (User) -> Unit) {
        onUserClickListener = listener
    }
}