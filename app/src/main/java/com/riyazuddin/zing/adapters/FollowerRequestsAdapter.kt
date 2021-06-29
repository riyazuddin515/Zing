package com.riyazuddin.zing.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.ItemFollowerRequestBinding
import javax.inject.Inject

class FollowerRequestsAdapter @Inject constructor(
    private val glide: RequestManager
) : PagingDataAdapter<User, FollowerRequestsAdapter.FollowerRequestViewHolder>(Companion) {

    inner class FollowerRequestViewHolder(private val binding: ItemFollowerRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.apply {
                glide.load(user.profilePicUrl).into(CIVProfilePic)
                tvName.text = user.name
                tvUsername.text = user.username

                CIVProfilePic.setOnClickListener {
                    onClickListener?.let {
                        it(user.uid)
                    }
                }
                tvName.setOnClickListener {
                    onClickListener?.let {
                        it(user.uid)
                    }
                }
                tvUsername.setOnClickListener {
                    onClickListener?.let {
                        it(user.uid)
                    }
                }
                ivArrow.setOnClickListener {
                    onClickListener?.let {
                        it(user.uid)
                    }
                }
                btnAllow.setOnClickListener {
                    onActionListener?.let {
                        it(user.uid, true)
                    }
                }
                btnDeny.setOnClickListener {
                    onActionListener?.let {
                        it(user.uid, false)
                    }
                }
            }
        }
    }

    companion object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowerRequestViewHolder {
        return FollowerRequestViewHolder(
            ItemFollowerRequestBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FollowerRequestViewHolder, position: Int) {
        holder.bind(getItem(position) ?: return)
    }

    private var onClickListener: ((String) -> Unit)? = null
    fun setOnClickListener(listener: (String) -> Unit) {
        onClickListener = listener
    }

    private var onActionListener: ((String, Boolean) -> Unit)? = null

    /**
     * true for Allow
     *      &
     * false for Deny
     */
    fun setOnActionListener(listener: (String, Boolean) -> Unit) {
        onActionListener = listener
    }
}