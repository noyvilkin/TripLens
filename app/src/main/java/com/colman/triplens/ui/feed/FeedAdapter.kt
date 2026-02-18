package com.colman.triplens.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.colman.triplens.data.model.Post
import com.colman.triplens.databinding.ItemPostBinding
import com.squareup.picasso.Picasso

class FeedAdapter(
    private val onPostClick: (Post) -> Unit
) : ListAdapter<Post, FeedAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onPostClick)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PostViewHolder(
        private val binding: ItemPostBinding,
        private val onPostClick: (Post) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            // Click listener for navigation to detail
            binding.cardPost.setOnClickListener { onPostClick(post) }

            binding.tvUserName.text = post.userName
            binding.tvDestination.text = post.destination
            binding.tvPostTitle.text = post.title

            // Short caption — truncated in feed
            binding.tvPostDescription.text = post.description.ifEmpty { "Tap to see details…" }

            // ── Thumbnail (first image only) ──
            loadThumbnail(post)

            // ── Weather summary ──
            val temp = post.temperature.ifEmpty { null }
            val condition = post.weatherCondition.ifEmpty { null }
            if (temp != null || condition != null) {
                binding.tvWeatherInfo.text = buildString {
                    if (temp != null) append("${temp}°C")
                    if (condition != null) {
                        if (temp != null) append("\n")
                        append(condition)
                    }
                }
                if (post.weatherIcon.isNotEmpty()) {
                    val iconUrl = "https://openweathermap.org/img/wn/${post.weatherIcon}@2x.png"
                    Picasso.get().load(iconUrl).into(binding.ivWeatherIcon)
                }
            } else {
                binding.tvWeatherInfo.text = "Weather\nunavailable"
            }

            // ── Country summary ──
            val capital = post.countryCapital.ifEmpty { null }
            val population = post.countryPopulation.ifEmpty { null }
            if (capital != null || population != null) {
                binding.tvCountryInfo.text = buildString {
                    if (capital != null) append("Capital: $capital")
                    if (population != null) {
                        if (capital != null) append("\n")
                        append("Pop: $population")
                    }
                }
            } else {
                binding.tvCountryInfo.text = "Country info\nunavailable"
            }

            // ── Country flag (in header) ──
            if (post.countryFlag.isNotEmpty()) {
                binding.ivCountryFlag.visibility = View.VISIBLE
                Picasso.get().load(post.countryFlag).into(binding.ivCountryFlag)
            } else {
                binding.ivCountryFlag.visibility = View.GONE
            }

            // ── Profile Picture ──
            if (post.userProfileImage.isNotEmpty()) {
                Picasso.get()
                    .load(post.userProfileImage)
                    .placeholder(android.R.drawable.ic_menu_compass)
                    .into(binding.ivUserProfile)
            } else {
                binding.ivUserProfile.setImageResource(android.R.drawable.ic_menu_compass)
            }
        }

        /**
         * Show only the first uploaded photo as the feed thumbnail.
         */
        private fun loadThumbnail(post: Post) {
            val firstUrl = if (post.imageUrls.isNotEmpty()) {
                post.imageUrls.split(",").firstOrNull { it.isNotBlank() }
            } else {
                post.travelImage.ifEmpty { null }
            }

            if (firstUrl != null) {
                Picasso.get()
                    .load(firstUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .fit().centerCrop()
                    .into(binding.ivTravelImage)
            } else {
                binding.ivTravelImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}
