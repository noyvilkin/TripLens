package com.colman.triplens.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.colman.triplens.data.model.Post
import com.colman.triplens.databinding.ItemPostBinding
import com.squareup.picasso.Picasso

class FeedAdapter : ListAdapter<Post, FeedAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            binding.tvUserName.text = post.userName
            binding.tvDestination.text = post.destination
            binding.tvPostTitle.text = post.title
            binding.tvPostDescription.text = post.description
            binding.tvCountryInfo.text = "Capital: ${post.countryCapital} | Pop: ${post.countryPopulation}"
            binding.tvWeatherInfo.text = "Weather: ${post.temperature}°C, ${post.weatherCondition}"

            // Main Travel Image
            if (post.travelImage.isNotEmpty()) {
                Picasso.get()
                    .load(post.travelImage)
                    .placeholder(android.R.drawable.progress_horizontal) // Shows while loading
                    .error(android.R.drawable.stat_notify_error)       // Shows if link is broken
                    .into(binding.ivPostImage)
            }

            // Profile Picture Adjustment
            if (post.userProfileImage.isNotEmpty()) {
                Picasso.get()
                    .load(post.userProfileImage)
                    .placeholder(android.R.drawable.ic_menu_gallery) // Default icon while loading
                    .into(binding.ivUserProfile)
            } else {
                // Fallback if URL is empty
                binding.ivUserProfile.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}