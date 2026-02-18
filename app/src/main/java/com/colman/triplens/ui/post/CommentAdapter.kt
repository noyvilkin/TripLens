package com.colman.triplens.ui.post

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.colman.triplens.data.model.Comment
import com.colman.triplens.databinding.ItemCommentBinding
import com.squareup.picasso.Picasso

class CommentAdapter : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentViewHolder(
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: Comment) {
            binding.tvCommentUserName.text = comment.userName.ifEmpty { "Anonymous" }
            binding.tvCommentText.text = comment.text

            // Relative time ("2h ago", "3 days ago")
            binding.tvCommentTime.text = DateUtils.getRelativeTimeSpanString(
                comment.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )

            if (comment.userProfileImage.isNotEmpty()) {
                Picasso.get()
                    .load(comment.userProfileImage)
                    .placeholder(android.R.drawable.ic_menu_compass)
                    .into(binding.ivCommentUserProfile)
            } else {
                binding.ivCommentUserProfile.setImageResource(android.R.drawable.ic_menu_compass)
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment) = oldItem == newItem
    }
}
