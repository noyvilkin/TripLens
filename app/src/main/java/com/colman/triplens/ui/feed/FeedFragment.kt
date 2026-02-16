package com.colman.triplens.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.colman.triplens.databinding.FragmentFeedBinding

/**
 * Fragment responsible for displaying the social feed of travel posts.
 * It observes the Room database via the FeedViewModel.
 */
class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FeedViewModel
    private lateinit var adapter: FeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = FeedAdapter()
        // Setting the LayoutManager is critical to prevent a white screen
        binding.recyclerViewFeed.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewFeed.adapter = adapter
    }

    private fun setupObservers() {
        // Observe the list of posts from the Room database
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            if (posts != null) {
                // Submit the list to the ListAdapter for efficient UI updates
                adapter.submitList(posts)
            }
        }

        // Observe loading state to show/hide the ProgressBar
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        // Trigger the addition of a new post when the FAB is clicked
        binding.fabAddPost.setOnClickListener {
            viewModel.addNewPost()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}