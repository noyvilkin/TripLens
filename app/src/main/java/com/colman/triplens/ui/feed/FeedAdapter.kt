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
import java.util.concurrent.TimeUnit

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

            // ── Timestamp ──
            binding.tvTimestamp.text = formatTimeAgo(post.timestamp)

            // ── Thumbnail (first image only) ──
            loadThumbnail(post)

            // ── Country Info Grid ──
            binding.includeCountryInfo.tvCapital.text = post.countryCapital.ifEmpty { "N/A" }
            binding.includeCountryInfo.tvCurrency.text = post.countryCurrency.ifEmpty { "N/A" }
            binding.includeCountryInfo.tvPopulation.text = post.countryPopulation.ifEmpty { "N/A" }
            binding.includeCountryInfo.tvLanguages.text = post.countryLanguages.ifEmpty { "N/A" }

            // ── Enhanced Weather Details ──
            val temp = post.temperature.ifEmpty { null }
            val condition = post.weatherCondition.ifEmpty { null }

            if (temp != null) {
                binding.includeWeatherCard.tvTemperature.text = "${temp}°C"
            } else {
                binding.includeWeatherCard.tvTemperature.text = "N/A"
            }

            if (condition != null) {
                binding.includeWeatherCard.tvWeatherCondition.text = condition
            } else {
                binding.includeWeatherCard.tvWeatherCondition.text = "N/A"
            }

            if (post.weatherIcon.isNotEmpty()) {
                val iconUrl = "https://openweathermap.org/img/wn/${post.weatherIcon}@2x.png"
                binding.includeWeatherCard.ivWeatherIcon.visibility = View.VISIBLE
                Picasso.get()
                    .load(iconUrl)
                    .placeholder(android.R.drawable.ic_menu_compass)
                    .error(android.R.drawable.ic_menu_report_image)
                    .fit()
                    .centerInside()
                    .into(binding.includeWeatherCard.ivWeatherIcon)
            } else {
                binding.includeWeatherCard.ivWeatherIcon.visibility = View.GONE
            }

            binding.includeWeatherCard.tvWindSpeed.text = if (post.windSpeed.isNotEmpty()) {
                "${post.windSpeed} km/h"
            } else {
                "N/A"
            }

            binding.includeWeatherCard.tvHumidity.text = if (post.humidity.isNotEmpty()) {
                "${post.humidity}%"
            } else {
                "N/A"
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
         * Format timestamp to relative time (e.g., "1d ago", "2h ago")
         */
        private fun formatTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "now"
                diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
                diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
                diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
                diff < TimeUnit.DAYS.toMillis(30) -> "${TimeUnit.MILLISECONDS.toDays(diff) / 7}w ago"
                else -> "${TimeUnit.MILLISECONDS.toDays(diff) / 30}mo ago"
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
