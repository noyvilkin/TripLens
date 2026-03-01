package com.colman.triplens.ui.post

import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.colman.triplens.R
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

            // Update comments count
            binding.tvCommentsTitle.text = "Comments (${comments.size})"
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

        // Format timestamp
        val timeAgo = DateUtils.getRelativeTimeSpanString(
            post.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        )
        binding.tvTimestamp.text = timeAgo

        if (post.userProfileImage.isNotEmpty()) {
            Picasso.get()
                .load(post.userProfileImage)
                .placeholder(android.R.drawable.ic_menu_compass)
                .into(binding.ivUserProfile)

            // Also set for comment input
            Picasso.get()
                .load(post.userProfileImage)
                .placeholder(android.R.drawable.ic_menu_compass)
                .into(binding.ivUserProfileComment)
        }

        // Country Flag
        if (post.countryFlag.isNotEmpty()) {
            binding.ivCountryFlag.visibility = View.VISIBLE
            Picasso.get()
                .load(post.countryFlag)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(binding.ivCountryFlag)
        } else {
            binding.ivCountryFlag.visibility = View.GONE
        }

        // ── Swipeable hero images ──
        setupImagePager(post)

        // Title & captions
        binding.tvTitle.text = post.title
        binding.tvCaption.text = post.description.ifEmpty { "No caption provided." }

        // Long description (only visible in detail view)
        val longDesc = post.longDescription.ifEmpty { post.description }
        binding.tvLongDescription.text = longDesc.ifEmpty { "No detailed description available." }

        // ── Country Info Cards ──
        if (post.countryCapital.isNotEmpty()) {
            binding.cardCapital.visibility = View.VISIBLE
            binding.tvCapital.text = post.countryCapital
        } else {
            binding.cardCapital.visibility = View.GONE
        }

        if (post.countryCurrency.isNotEmpty()) {
            binding.cardCurrency.visibility = View.VISIBLE
            binding.tvCurrency.text = post.countryCurrency
        } else {
            binding.cardCurrency.visibility = View.GONE
        }

        if (post.countryPopulation.isNotEmpty()) {
            binding.cardPopulation.visibility = View.VISIBLE
            binding.tvPopulation.text = post.countryPopulation
        } else {
            binding.cardPopulation.visibility = View.GONE
        }

        if (post.countryLanguages.isNotEmpty()) {
            binding.cardLanguages.visibility = View.VISIBLE
            binding.tvLanguages.text = post.countryLanguages
        } else {
            binding.cardLanguages.visibility = View.GONE
        }

        // ── Weather ──
        val temp = post.temperature.ifEmpty { null }
        val condition = post.weatherCondition.ifEmpty { null }
        val humidity = post.humidity.ifEmpty { null }
        val windSpeed = post.windSpeed.ifEmpty { null }

        if (temp != null || condition != null) {
            binding.cardWeather.visibility = View.VISIBLE

            // Temperature
            if (temp != null) {
                binding.tvTemperature.text = "${temp}°C"
                binding.tvTemperature.visibility = View.VISIBLE
            } else {
                binding.tvTemperature.visibility = View.GONE
            }

            // Condition
            if (condition != null) {
                binding.tvWeatherCondition.text = condition
                binding.tvWeatherCondition.visibility = View.VISIBLE
            } else {
                binding.tvWeatherCondition.visibility = View.GONE
            }

            // Weather icon
            if (post.weatherIcon.isNotEmpty()) {
                val iconUrl = "https://openweathermap.org/img/wn/${post.weatherIcon}@2x.png"
                binding.ivWeatherIcon.visibility = View.VISIBLE
                Picasso.get()
                    .load(iconUrl)
                    .placeholder(android.R.drawable.ic_menu_day)
                    .error(android.R.drawable.ic_menu_day)
                    .into(binding.ivWeatherIcon)
            } else {
                binding.ivWeatherIcon.visibility = View.VISIBLE
                binding.ivWeatherIcon.setImageResource(android.R.drawable.ic_menu_day)
            }

            // Humidity
            if (humidity != null) {
                binding.tvHumidity.text = humidity
                binding.tvHumidity.visibility = View.VISIBLE
            } else {
                binding.tvHumidity.visibility = View.GONE
            }

            // Wind speed
            if (windSpeed != null) {
                binding.tvWindSpeed.text = windSpeed
                binding.tvWindSpeed.visibility = View.VISIBLE
            } else {
                binding.tvWindSpeed.visibility = View.GONE
            }
        } else {
            binding.cardWeather.visibility = View.GONE
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

        if (urls.isNotEmpty()) {
            // Setup thumbnails
            setupThumbnails(urls)

            // Show page indicator if multiple images
            if (urls.size > 1) {
                binding.tvPageIndicator.visibility = View.VISIBLE
                binding.tvPageIndicator.text = "Image 1 of ${urls.size}"

                binding.vpImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        binding.tvPageIndicator.text = "Image ${position + 1} of ${urls.size}"
                        highlightThumbnail(position)
                    }
                })
            } else {
                binding.tvPageIndicator.visibility = View.GONE
            }
        } else {
            binding.thumbnailScroll.visibility = View.GONE
            binding.tvPageIndicator.visibility = View.GONE
        }
    }

    private fun setupThumbnails(urls: List<String>) {
        if (urls.size <= 1) {
            binding.thumbnailScroll.visibility = View.GONE
            return
        }

        binding.thumbnailScroll.visibility = View.VISIBLE
        binding.thumbnailContainer.removeAllViews()

        val thumbnailSizePx = (64 * resources.displayMetrics.density).toInt()
        val thumbnailMarginPx = (8 * resources.displayMetrics.density).toInt()

        urls.forEachIndexed { index, url ->
            val thumbnailView = ImageView(requireContext()).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    thumbnailSizePx,
                    thumbnailSizePx
                ).apply {
                    marginEnd = thumbnailMarginPx
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(4)

                // Add border for first thumbnail
                if (index == 0) {
                    setBackgroundColor(Color.parseColor("#7C4DFF"))
                    setPadding(6)
                } else {
                    setBackgroundColor(Color.TRANSPARENT)
                    setPadding(0)
                }

                setOnClickListener {
                    binding.vpImages.currentItem = index
                }
            }

            Picasso.get()
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .fit()
                .centerCrop()
                .into(thumbnailView)

            binding.thumbnailContainer.addView(thumbnailView)
        }
    }

    private fun highlightThumbnail(position: Int) {
        val borderWidth = (3 * resources.displayMetrics.density).toInt()
        for (i in 0 until binding.thumbnailContainer.childCount) {
            val thumbnail = binding.thumbnailContainer.getChildAt(i) as? ImageView
            if (i == position) {
                thumbnail?.setPadding(borderWidth)
                thumbnail?.setBackgroundColor(Color.parseColor("#7C4DFF"))
            } else {
                thumbnail?.setPadding(0)
                thumbnail?.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
