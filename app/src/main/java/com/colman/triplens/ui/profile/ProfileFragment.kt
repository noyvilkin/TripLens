package com.colman.triplens.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.colman.triplens.R
import com.colman.triplens.base.MainActivity
import com.colman.triplens.databinding.FragmentProfileBinding
import com.colman.triplens.util.BrandedSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private lateinit var adapter: ProfilePostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when returning (e.g. after editing profile)
        viewModel.refreshUser()
    }

    private fun setupRecyclerView() {
        adapter = ProfilePostAdapter(
            onPostClick = { post ->
                // Navigate to post detail
                val action = ProfileFragmentDirections
                    .actionProfileFragmentToPostDetailFragment(post.id)
                findNavController().navigate(action)
            },
            onEditClick = { post ->
                // Navigate to AddPostFragment in edit mode
                val action = ProfileFragmentDirections
                    .actionProfileFragmentToAddPostFragment(post.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { post ->
                showDeleteConfirmation(post.id)
            }
        )
        binding.rvMyPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyPosts.adapter = adapter
    }

    private fun setupObservers() {
        // User info
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvDisplayName.text = user.displayName?.ifEmpty { "No Name" } ?: "No Name"
                binding.tvEmail.text = user.email ?: ""

                val photoUrl = user.photoUrl?.toString()
                if (!photoUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(photoUrl)
                        .placeholder(android.R.drawable.ic_menu_myplaces)
                        .fit().centerCrop()
                        .into(binding.ivProfileImage)
                } else {
                    binding.ivProfileImage.setImageResource(android.R.drawable.ic_menu_myplaces)
                }
            }
        }

        // User posts
        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            binding.tvEmptyPosts.visibility =
                if (posts.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // Post deleted — branded Snackbar
        viewModel.postDeleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted) {
                BrandedSnackbar.showSuccess(binding.root, getString(R.string.post_deleted))
                viewModel.clearPostDeleted()
            }
        }

        // Error (e.g. post deletion failure) — branded Snackbar
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                BrandedSnackbar.showError(binding.root, it)
                viewModel.clearError()
            }
        }

        // Profile update success — refresh toolbar profile image
        viewModel.profileUpdateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                (activity as? MainActivity)?.loadProfileImage()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            val dialog = EditProfileDialogFragment()
            dialog.show(childFragmentManager, "EditProfileDialog")
        }
    }

    private fun showDeleteConfirmation(postId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_post_title)
            .setMessage(R.string.delete_post_message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePost(postId)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
