package com.colman.triplens.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.colman.triplens.data.model.Post
import com.colman.triplens.databinding.ItemProfilePostBinding
import com.squareup.picasso.Picasso

class ProfilePostAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit
) : ListAdapter<Post, ProfilePostAdapter.ProfilePostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePostViewHolder {
        val binding = ItemProfilePostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProfilePostViewHolder(binding, onPostClick, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ProfilePostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProfilePostViewHolder(
        private val binding: ItemProfilePostBinding,
        private val onPostClick: (Post) -> Unit,
        private val onEditClick: (Post) -> Unit,
        private val onDeleteClick: (Post) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.cardProfilePost.setOnClickListener { onPostClick(post) }
            binding.btnEdit.setOnClickListener { onEditClick(post) }
            binding.btnDelete.setOnClickListener { onDeleteClick(post) }

            binding.tvPostTitle.text = post.title
            binding.tvPostDescription.text = post.description.ifEmpty { "No description" }
            binding.tvPostDestination.text = post.destination.ifEmpty { "No destination" }

            // Load thumbnail
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
                    .into(binding.ivPostThumbnail)
            } else {
                binding.ivPostThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}
