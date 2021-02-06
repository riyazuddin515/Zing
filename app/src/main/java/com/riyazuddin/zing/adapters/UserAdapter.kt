package com.riyazuddin.zing.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.ItemUserBinding
import java.io.Serializable
import javax.inject.Inject

class UserAdapter @Inject constructor(val glide: RequestManager) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>(), Serializable {

    class UserViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    private val differCallback = object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }
    }

    private val differ = AsyncListDiffer(this, differCallback)

    var users: List<User>
        get() = differ.currentList
        set(value) = differ.submitList(value)

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
            val user = users[position]
            glide.load(user.profilePicUrl).into(CIVProfilePic)
            tvName.text = user.name
            tvUsername.text = user.username
            userItemCardView.setOnClickListener {
                onUserClickListener?.let { click ->
                    click(user)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }

    private var onUserClickListener: ((User) -> Unit)? = null
    fun setOnUserClickListener(listener: (User) -> Unit) {
        onUserClickListener = listener
    }
}