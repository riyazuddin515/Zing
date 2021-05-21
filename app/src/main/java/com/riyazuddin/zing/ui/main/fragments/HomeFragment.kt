package com.riyazuddin.zing.ui.main.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riyazuddin.zing.R
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.databinding.FragmentHomeBinding
import com.riyazuddin.zing.other.EventObserver
import com.riyazuddin.zing.other.snackBar
import com.riyazuddin.zing.ui.auth.AuthActivity
import com.riyazuddin.zing.ui.dialogs.CustomDialog
import com.riyazuddin.zing.ui.main.viewmodels.BasePostViewModel
import com.riyazuddin.zing.ui.main.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class HomeFragment : BasePostFragment(R.layout.fragment_home) {

    override val source: String
        get() = (R.id.homeFragment).toString()

    private lateinit var binding: FragmentHomeBinding

    override val basePostViewModel: BasePostViewModel
        get() {
            val vm: HomeViewModel by viewModels()
            return vm
        }

    //    private val viewModel: HomeViewModel by lazy { basePostViewModel as HomeViewModel }
    private lateinit var viewModel: HomeViewModel

    private var currentUser: User? = null

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentHomeBinding.bind(view)

        viewModel = basePostViewModel as HomeViewModel

        subscribeToObservers()
        setUpRecyclerView()

        binding.btnLogout.setOnClickListener {
            CustomDialog("Log Out", " Are you sure to logout of the app?").apply {
                setPositiveListener {
                    Firebase.auth.signOut()
                    Intent(requireActivity(), AuthActivity::class.java).apply {
                        startActivity(this)
                        requireActivity().finish()
                    }
                }
            }.show(parentFragmentManager, null)
        }

        binding.ibRecentChat.setOnClickListener {
            val bundle = Bundle().apply {
                putSerializable("currentUser", currentUser)
            }
            findNavController().navigate(R.id.action_homeFragment_to_recentChatListFragment, bundle)
        }

//        binding.btnSendNotification.setOnClickListener {
//            val cu = "{\"uid\":\"BbZaXWWWnuV5Dx7qQv7EtUTSQ392\",\"profilePicUrl\":\"https:\\/\\/firebasestorage.googleapis.com\\/v0\\/b\\/zing515.appspot.com\\/o\\/profilePics%2FBbZaXWWWnuV5Dx7qQv7EtUTSQ392?alt=media&token=48cd48a7-37fd-422c-a2a0-961702004c1c\",\"name\":\"Mohammed Fayazuddin\",\"bio\":\"I'm on Zing now\",\"postCount\":\"2\",\"followersCount\":\"1\",\"followingCount\":\"1\",\"username\":\"fayazuddin786\"}"
////            val ou = "{\"uid\":\"mmcvWxGSMfPmPvYu1ZJVX0RsZEP2\",\"profilePicUrl\":\"https:\\/\\/firebasestorage.googleapis.com\\/v0\\/b\\/zing515.appspot.com\\/o\\/profilePics%2FBbZaXWWWnuV5Dx7qQv7EtUTSQ392?alt=media&token=48cd48a7-37fd-422c-a2a0-961702004c1c\",\"name\":\"Riyazuddin\",\"bio\":\"I'm on Zing now\",\"postCount\":\"1\",\"followersCount\":\"1\",\"followingCount\":\"1\",\"username\":\"riyazuddin515\"}"
//            val ou = "{\"uid\":\"mmcvWxGSMfPmPvYu1ZJVX0RsZEP2\",\"profilePicUrl\":\"https:\\/\\/firebasestorage.googleapis.com\\/v0\\/b\\/zing515.appspot.com\\/o\\/profilePics%2FmmcvWxGSMfPmPvYu1ZJVX0RsZEP2?alt=media&token=d1864538-3c7f-4a7a-a14b-f200fe052885\",\"name\":\"Riyazuddin\",\"bio\":\"I'm on Zing now\",\"postCount\":\"1\",\"followersCount\":\"1\",\"followingCount\":\"1\",\"username\":\"riyazuddin515\"}"
//            val c = Gson().fromJson(cu, User::class.java)
//            val o = Gson().fromJson(ou, User::class.java)
//            Log.i(TAG, "onViewCreated: $c")
//
////            val pendingIntent = NavDeepLinkBuilder(requireContext())
////                .setGraph(R.navigation.nav_graph_main)
////                .setDestination(R.id.chatFragment)
////                .setArguments(ChatFragmentArgs(o, c).toBundle())
////                .createPendingIntent()
//
//            val pendingIntent = findNavController()
//                .createDeepLink()
//                .setGraph(R.navigation.nav_graph_main)
//                .setDestination(R.id.chatFragment)
//                .setArguments(ChatFragmentArgs(o, c).toBundle())
//                .createPendingIntent()
//
//            val builder = NotificationCompat.Builder(requireContext(), Constants.CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_chat)
//                .setContentTitle("title")
//                .setContentText("body")
//                .setContentIntent(pendingIntent)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
//                .setAutoCancel(true)
//
////            val notificationManager: NotificationManager =
////                getSystemService(requireContext()) as NotificationManager
//
//            NotificationManagerCompat.from(requireContext()).notify(1, builder.build())
//
////            notificationManager.notify(1, builder.build())
//        }
//        lifecycleScope.launch {
//            viewModel.pagingFlow.collect {
//                postAdapter.submitData(it)
//            }
//        }
//
//        lifecycleScope.launch {
//            postAdapter.loadStateFlow.collectLatest {
//                binding.progressBar.isVisible =
//                    it.refresh is LoadState.Loading ||
//                            it.append is LoadState.Loading
//            }
//        }

        if (currentUser == null) {
            viewModel.loadCurrentUser(Firebase.auth.uid!!)
        }else{
            binding.ibRecentChat.isVisible = true
        }

        postAdapter.setOnUserClickListener {
            findNavController().navigate(
                HomeFragmentDirections.globalActionToOthersProfileFragment(
                    it.postedBy
                )
            )
        }
    }

    private fun subscribeToObservers() {
        viewModel.loadCurrentUserStatus.observe(viewLifecycleOwner, EventObserver(
            oneTimeConsume = true,
            onError = { snackBar(it) },
            onLoading = { Log.i(TAG, "subscribeToObservers: loading current user") }
        ) {
            currentUser = it
            binding.ibRecentChat.isVisible = true
        })
    }

    private fun setUpRecyclerView() {
        binding.rvPost.apply {
            adapter = postAdapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    companion object {
        const val TAG = "HomeFragment"
    }

}