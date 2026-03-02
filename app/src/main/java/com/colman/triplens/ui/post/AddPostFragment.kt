package com.colman.triplens.ui.post

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.colman.triplens.R
import com.colman.triplens.databinding.FragmentAddPostBinding
import com.colman.triplens.util.BrandedSnackbar
import com.squareup.picasso.Picasso
import java.io.File

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AddPostViewModel
    private var currentPhotoUri: Uri? = null

    private val args: AddPostFragmentArgs by navArgs()

    // Camera permission
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else BrandedSnackbar.showError(binding.root, getString(R.string.camera_permission_required))
    }

    // Camera capture
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) currentPhotoUri?.let { viewModel.addImage(it) }
    }

    // Gallery multi-pick
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(AddPostViewModel.MAX_IMAGES)
    ) { uris ->
        val existingCount = viewModel.existingImageUrls.value.orEmpty().size
        val newCount = viewModel.selectedImages.value?.size ?: 0
        val remaining = AddPostViewModel.MAX_IMAGES - existingCount - newCount
        uris.take(remaining).forEach { uri ->
            val localUri = copyToLocalFile(uri)
            viewModel.addImage(localUri ?: uri)
        }
        if (uris.size > remaining) {
            BrandedSnackbar.showError(
                binding.root,
                getString(R.string.max_images_warning, AddPostViewModel.MAX_IMAGES)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[AddPostViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Set toolbar title ────────────────────────────────────
        (activity as? AppCompatActivity)?.supportActionBar?.title =
            getString(R.string.create_new_post)

        setupCountryAutoComplete()
        setupClickListeners()
        setupObservers()

        // Check if we're in edit mode
        val postId = args.postId
        if (postId.isNotEmpty()) {
            viewModel.loadPostForEditing(postId)
            // Override title for edit mode
            (activity as? AppCompatActivity)?.supportActionBar?.title =
                getString(R.string.edit_post)
        }
    }

    /**
     * Set up the destination field with a country autocomplete adapter.
     */
    private fun setupCountryAutoComplete() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf<String>()
        )
        binding.actvDestination.setAdapter(adapter)

        viewModel.countryNames.observe(viewLifecycleOwner) { countries ->
            adapter.clear()
            adapter.addAll(countries)
            adapter.notifyDataSetChanged()
        }

        binding.actvDestination.setOnItemClickListener { _, _, _, _ ->
            binding.tilDestination.error = null
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) launchCamera()
            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.btnSubmit.setOnClickListener {
            val dest = binding.actvDestination.text.toString().trim()

            val validCountries = viewModel.countryNames.value.orEmpty()
            if (dest.isNotEmpty() && validCountries.none { it.equals(dest, ignoreCase = true) }) {
                binding.tilDestination.error = "Please select a valid country from the list"
                return@setOnClickListener
            }
            binding.tilDestination.error = null

            viewModel.submitPost(
                title = binding.etTitle.text.toString().trim(),
                description = binding.etDescription.text.toString().trim(),
                longDescription = binding.etLongDescription.text.toString().trim(),
                destination = dest
            )
        }
    }

    private fun setupObservers() {
        // New image picks
        viewModel.selectedImages.observe(viewLifecycleOwner) { _ ->
            refreshAllImagePreviews()
        }

        // Existing image URLs (edit mode)
        viewModel.existingImageUrls.observe(viewLifecycleOwner) { _ ->
            refreshAllImagePreviews()
        }

        // Edit mode: pre-fill form when post data is loaded
        viewModel.editingPost.observe(viewLifecycleOwner) { post ->
            if (post != null) {
                binding.etTitle.setText(post.title)
                binding.etDescription.setText(post.description)
                binding.etLongDescription.setText(post.longDescription)
                binding.actvDestination.setText(post.destination, false)
                binding.btnSubmit.text = getString(R.string.update)
            }
        }

        // Loading state — show progress bar and disable button
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSubmit.isEnabled = !loading
        }

        // Error — branded Snackbar (SingleLiveEvent prevents duplicates on rotation)
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                BrandedSnackbar.showError(binding.root, it)
                viewModel.clearError()
            }
        }

        // Post saved — show success Snackbar on the activity view so it
        // persists through the back-navigation, then navigate up.
        viewModel.postCreated.observe(viewLifecycleOwner) { created ->
            if (created) {
                val message = if (args.postId.isNotEmpty()) {
                    getString(R.string.post_updated)
                } else {
                    getString(R.string.post_created)
                }
                activity?.findViewById<View>(android.R.id.content)?.let { root ->
                    BrandedSnackbar.showSuccess(root, message)
                }
                findNavController().navigateUp()
            }
        }
    }

    /**
     * Render both existing cloud images and newly picked local images
     * in the preview container. Toggle the empty-state placeholder.
     */
    private fun refreshAllImagePreviews() {
        binding.imageContainer.removeAllViews()
        val size = (120 * resources.displayMetrics.density).toInt()
        val margin = (6 * resources.displayMetrics.density).toInt()

        // Existing cloud images (edit mode)
        val existingUrls = viewModel.existingImageUrls.value.orEmpty()
        existingUrls.forEachIndexed { index, url ->
            val iv = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = margin }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setOnClickListener { viewModel.removeExistingImage(index) }
            }
            Picasso.get().load(url).resize(size, size).centerCrop().into(iv)
            binding.imageContainer.addView(iv)
        }

        // Newly picked local images
        val newUris = viewModel.selectedImages.value.orEmpty()
        newUris.forEachIndexed { index, uri ->
            val iv = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = margin }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setOnClickListener { viewModel.removeImage(index) }
            }
            Picasso.get().load(uri).resize(size, size).centerCrop().into(iv)
            binding.imageContainer.addView(iv)
        }

        // Toggle empty upload placeholder
        val hasImages = existingUrls.isNotEmpty() || newUris.isNotEmpty()
        binding.emptyUploadState.visibility = if (hasImages) View.GONE else View.VISIBLE
    }

    private fun copyToLocalFile(uri: Uri): Uri? {
        val file = File(requireContext().cacheDir, "img_${System.currentTimeMillis()}.jpg")
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            Uri.fromFile(file)
        } catch (_: Exception) { null }
    }

    private fun launchCamera() {
        val dir = File(requireContext().cacheDir, "images").also { it.mkdirs() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        cameraLauncher.launch(currentPhotoUri!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
