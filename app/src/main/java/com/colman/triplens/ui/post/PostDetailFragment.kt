package com.colman.triplens.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.colman.triplens.data.model.Post
import com.colman.triplens.databinding.FragmentPostDetailBinding
import com.colman.triplens.ui.common.ImagePagerAdapter
import com.colman.triplens.util.BrandedSnackbar
import com.squareup.picasso.Picasso

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PostDetailViewModel
    private lateinit var commentAdapter: CommentAdapter

    private val args: PostDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[PostDetailViewModel::class.java]
        setupCommentsRecyclerView()
        setupClickListeners()
        setupObservers()

        // Load the post using the SafeArgs postId
        viewModel.loadPost(args.postId)
    }

    private fun setupCommentsRecyclerView() {
        commentAdapter = CommentAdapter()
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComments.adapter = commentAdapter
    }

    private fun setupClickListeners() {
        binding.fabBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.fabSendComment.setOnClickListener {
            val text = binding.etComment.text?.toString() ?: ""
            if (text.isNotBlank()) {
                viewModel.addComment(text)
                binding.etComment.text?.clear()
            }
        }
    }

    private fun setupObservers() {
        // Post data
        viewModel.post.observe(viewLifecycleOwner) { post ->
            if (post != null) {
                bindPostData(post)
            }
        }

        // Comments
        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            commentAdapter.submitList(comments)
            binding.tvNoComments.visibility =
                if (comments.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoadingComments.observe(viewLifecycleOwner) { loading ->
            binding.progressComments.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.commentError.observe(viewLifecycleOwner) { error ->
            error?.let {
                BrandedSnackbar.showError(binding.root, it)
                viewModel.clearCommentError()
            }
        }
    }

    private fun bindPostData(post: Post) {
        // User info
        binding.tvUserName.text = post.userName
        binding.tvDestination.text = post.destination

        if (post.userProfileImage.isNotEmpty()) {
            Picasso.get()
                .load(post.userProfileImage)
                .placeholder(android.R.drawable.ic_menu_compass)
                .into(binding.ivUserProfile)
        }

        // ── Swipeable hero images ──
        setupImagePager(post)

        // Title & captions
        binding.tvTitle.text = post.title
        binding.tvCaption.text = post.description.ifEmpty { "No caption provided." }

        // Long description (only visible in detail view)
        val longDesc = post.longDescription.ifEmpty { post.description }
        binding.tvLongDescription.text = longDesc.ifEmpty { "No detailed description available." }

        // ── Weather ──
        val temp = post.temperature.ifEmpty { null }
        val condition = post.weatherCondition.ifEmpty { null }
        if (temp != null || condition != null) {
            binding.cardWeather.visibility = View.VISIBLE
            binding.tvWeatherInfo.text = buildString {
                if (temp != null) append("${temp}°C")
                if (condition != null) {
                    if (temp != null) append(", ")
                    append(condition)
                }
            }
            if (post.weatherIcon.isNotEmpty()) {
                val iconUrl = "https://openweathermap.org/img/wn/${post.weatherIcon}@2x.png"
                Picasso.get().load(iconUrl).into(binding.ivWeatherIcon)
            }
        } else {
            binding.cardWeather.visibility = View.VISIBLE
            binding.tvWeatherInfo.text = "Weather data unavailable"
        }

        // ── Country info ──
        val hasCountryData = post.countryCapital.isNotEmpty() ||
                post.countryPopulation.isNotEmpty() ||
                post.countryLanguages.isNotEmpty() ||
                post.countryCurrency.isNotEmpty()

        if (hasCountryData) {
            binding.cardCountry.visibility = View.VISIBLE
            binding.tvCountryInfo.text = buildString {
                if (post.countryCapital.isNotEmpty()) append("Capital: ${post.countryCapital}\n")
                if (post.countryPopulation.isNotEmpty()) append("Population: ${post.countryPopulation}\n")
                if (post.countryLanguages.isNotEmpty()) append("Languages: ${post.countryLanguages}\n")
                if (post.countryCurrency.isNotEmpty()) append("Currency: ${post.countryCurrency}")
            }.trimEnd()
        } else {
            binding.cardCountry.visibility = View.VISIBLE
            binding.tvCountryInfo.text = "Country data unavailable"
        }

        // Flag
        if (post.countryFlag.isNotEmpty()) {
            binding.ivCountryFlag.visibility = View.VISIBLE
            Picasso.get().load(post.countryFlag).into(binding.ivCountryFlag)
        } else {
            binding.ivCountryFlag.visibility = View.GONE
        }
    }

    private fun setupImagePager(post: Post) {
        val urls = if (post.imageUrls.isNotEmpty()) {
            post.imageUrls.split(",").filter { it.isNotBlank() }
        } else if (post.travelImage.isNotEmpty()) {
            listOf(post.travelImage)
        } else {
            emptyList()
        }

        binding.vpImages.adapter = ImagePagerAdapter(urls)

        if (urls.size > 1) {
            binding.tvPageIndicator.visibility = View.VISIBLE
            binding.tvPageIndicator.text = "1 / ${urls.size}"

            binding.vpImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.tvPageIndicator.text = "${position + 1} / ${urls.size}"
                }
            })
        } else {
            binding.tvPageIndicator.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
