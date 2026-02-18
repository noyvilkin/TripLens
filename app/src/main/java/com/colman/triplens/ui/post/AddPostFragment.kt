package com.colman.triplens.ui.post

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.colman.triplens.databinding.FragmentAddPostBinding
import com.squareup.picasso.Picasso
import java.io.File

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AddPostViewModel
    private var currentPhotoUri: Uri? = null

    // Camera permission
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
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
        val remaining = AddPostViewModel.MAX_IMAGES - (viewModel.selectedImages.value?.size ?: 0)
        uris.take(remaining).forEach { uri ->
            val localUri = copyToLocalFile(uri)
            viewModel.addImage(localUri ?: uri)
        }
        if (uris.size > remaining) {
            Toast.makeText(context, "Max ${AddPostViewModel.MAX_IMAGES} images", Toast.LENGTH_SHORT).show()
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
        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
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

        // Fetch weather/country when destination field loses focus
        binding.etDestination.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val dest = binding.etDestination.text.toString().trim()
                if (dest.isNotEmpty()) viewModel.fetchDestinationData(dest)
            }
        }

        binding.btnSubmit.setOnClickListener {
            // Trigger destination fetch if not already done
            val dest = binding.etDestination.text.toString().trim()
            viewModel.submitPost(
                title = binding.etTitle.text.toString().trim(),
                description = binding.etDescription.text.toString().trim(),
                destination = dest
            )
        }
    }

    private fun setupObservers() {
        viewModel.selectedImages.observe(viewLifecycleOwner) { uris -> refreshImagePreviews(uris) }

        viewModel.weatherText.observe(viewLifecycleOwner) { text ->
            text?.let {
                binding.cardWeather.visibility = View.VISIBLE
                binding.tvWeatherInfo.text = it
            }
        }

        viewModel.weatherIconUrl.observe(viewLifecycleOwner) { url ->
            url?.let { Picasso.get().load(it).into(binding.ivWeatherIcon) }
        }

        viewModel.countryText.observe(viewLifecycleOwner) { text ->
            text?.let {
                binding.cardCountry.visibility = View.VISIBLE
                binding.tvCountryInfo.text = it
            }
        }

        viewModel.countryFlagUrl.observe(viewLifecycleOwner) { url ->
            url?.let { Picasso.get().load(it).into(binding.ivCountryFlag) }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSubmit.isEnabled = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.postCreated.observe(viewLifecycleOwner) { created ->
            if (created) findNavController().navigateUp()
        }
    }

    private fun refreshImagePreviews(uris: List<Uri>) {
        binding.imageContainer.removeAllViews()
        val size = (120 * resources.displayMetrics.density).toInt()
        val margin = (6 * resources.displayMetrics.density).toInt()

        uris.forEachIndexed { index, uri ->
            val iv = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = margin }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setOnClickListener { viewModel.removeImage(index) }
            }
            Picasso.get().load(uri).resize(size, size).centerCrop().into(iv)
            binding.imageContainer.addView(iv)
        }
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
