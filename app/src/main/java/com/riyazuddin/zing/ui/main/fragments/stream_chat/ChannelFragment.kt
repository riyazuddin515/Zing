package com.riyazuddin.zing.ui.main.fragments.stream_chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewbinding.ViewBinding
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentChannelBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.NavGraphArgsConstants.CURRENT_USER_ARG
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.fragments.BindingFragment
import com.riyazuddin.zing.ui.main.viewmodels.GetStreamViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.chat.android.client.models.Filters
import io.getstream.chat.android.ui.channel.list.viewmodel.ChannelListViewModel
import io.getstream.chat.android.ui.channel.list.viewmodel.bindView
import io.getstream.chat.android.ui.channel.list.viewmodel.factory.ChannelListViewModelFactory

@AndroidEntryPoint
class ChannelFragment : BindingFragment<FragmentChannelBinding>() {

    companion object {
        const val TAG = "ChannelFragment"
    }

    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentChannelBinding::inflate

    private val args: ChannelFragmentArgs by navArgs()

    private val getStreamViewModel by viewModels<GetStreamViewModel>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeTOObservers()
        setupClickListeners()
        setupChannelListView()
    }

    private fun setupChannelListView() {
        val filter = Filters.and(
            Filters.eq("type", "messaging"),
            Filters.`in`("members", listOf(args.currentUser.uid))
        )
        val viewModelFactory =
            ChannelListViewModelFactory(filter, ChannelListViewModel.DEFAULT_SORT)
        val viewModel: ChannelListViewModel by viewModels { viewModelFactory }

        viewModel.bindView(binding.channelListView, this)
    }

    private fun subscribeTOObservers() {
        getStreamViewModel.updateNotificationTokenStatus.observe(viewLifecycleOwner, EventObserver(
            onLoading = {
                binding.progressCircular.isVisible = true
            },
            onError = {
                binding.progressCircular.isVisible = false
                snackBar(it)
            }
        ) {
            binding.progressCircular.isVisible = false
            snackBar("Notification Token Updated Successfully")
        })
    }

    private fun setupClickListeners() {
        binding.channelListView.setChannelItemClickListener { channel ->
            val bundle = Bundle().apply {
                putString("channelId", channel.cid)
            }
            findNavController().navigate(R.id.action_channelFragment_to_streamChatFragment, bundle)
        }
        binding.fabNewChat.setOnClickListener {
            val bundle = Bundle().apply {
                putSerializable(CURRENT_USER_ARG, args.currentUser)
            }
            findNavController().navigate(
                R.id.action_channelFragment_to_newChatFragment,
                bundle
            )
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.ibNotificationUpdate.setOnClickListener {
            CustomDialog(
                getString(R.string.update_notification_token),
                getString(R.string.update_notification_toke_message),
                getString(R.string.update),
                getString(R.string.cancel)
            ).apply {
                setPositiveListener {
                    getStreamViewModel.setFCMTokenInStream()
                }
            }.show(childFragmentManager, null)
        }
    }
}