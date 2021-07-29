package com.riyazuddin.get_stream_chat.ui

import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding
import com.riyazuddin.get_stream_chat.databinding.FragmentChannelBinding

class ChannelFragment : BindingFragment<FragmentChannelBinding>() {

    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentChannelBinding::inflate
}