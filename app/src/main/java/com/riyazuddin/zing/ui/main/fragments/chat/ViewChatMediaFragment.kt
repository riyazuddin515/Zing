package com.riyazuddin.zing.ui.main.fragments.chat

import android.app.DownloadManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.storage.FirebaseStorage
import com.riyazuddin.zing.R
import com.riyazuddin.zing.databinding.FragmentViewChatMediaBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ViewChatMediaFragment : Fragment(R.layout.fragment_view_chat_media) {

    @Inject
    lateinit var storage: FirebaseStorage

    @Inject
    lateinit var glide: RequestManager

    private lateinit var binding: FragmentViewChatMediaBinding
    private val args: ViewChatMediaFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentViewChatMediaBinding.inflate(layoutInflater)
        val animation = TransitionInflater.from(requireContext()).inflateTransition(
            android.R.transition.explode
        )
        sharedElementEnterTransition = animation
        sharedElementReturnTransition = animation
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = args.username
        args.time?.let {
            binding.toolbar.subtitle = it
        }
        postponeEnterTransition()
        binding.imageView.apply {
            transitionName = args.id
            startEnterTransitionAfterLoadingImage(args.url, this)
        }

        binding.rootLayout.setOnClickListener {
            binding.appBarLayout.isVisible = !binding.appBarLayout.isVisible
        }

        binding.ivDownload.setOnClickListener {
            val request = DownloadManager.Request(Uri.parse(args.url))
            val title = "${args.id}.jpg"
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title)
            request.setMimeType("image/jpeg")
            request.setAllowedOverMetered(true)
            request.setTitle(resources.getString(R.string.app_name))
            request.setDescription(getString(R.string.downloading))
            val manager =
                requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(requireContext(), getString(R.string.downloading), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun startEnterTransitionAfterLoadingImage(imageAddress: String, imageView: ImageView) {
        glide.load(imageAddress)
            .dontAnimate()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    startPostponedEnterTransition()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    startPostponedEnterTransition()
                    return false
                }
            }).into(imageView)
    }

}