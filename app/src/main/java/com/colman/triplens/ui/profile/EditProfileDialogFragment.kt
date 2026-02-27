package com.colman.triplens.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.colman.triplens.R
import com.colman.triplens.databinding.DialogEditProfileBinding
import com.colman.triplens.util.BrandedSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import java.io.File

class EditProfileDialogFragment : DialogFragment() {

    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileViewModel: ProfileViewModel
    private var selectedImageUri: Uri? = null
    private var currentPhotoUri: Uri? = null

    // Camera permission
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else activity?.findViewById<View>(android.R.id.content)?.let { root ->
            BrandedSnackbar.showError(root, getString(R.string.camera_permission_required))
        }
    }

    // Camera capture
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                selectedImageUri = uri
                Picasso.get().load(uri)
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .fit().centerCrop()
                    .into(binding.ivDialogProfileImage)
            }
        }
    }

    // Gallery pick
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val localUri = copyToLocalFile(it)
            selectedImageUri = localUri ?: it
            Picasso.get().load(selectedImageUri)
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .fit().centerCrop()
                .into(binding.ivDialogProfileImage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get ViewModel from parent fragment scope
        profileViewModel = ViewModelProvider(requireParentFragment())[ProfileViewModel::class.java]

        populateCurrentData()
        setupClickListeners()
        setupObservers()
        setupTextWatchers()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun populateCurrentData() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        // Display name
        binding.etDialogDisplayName.setText(user.displayName ?: "")

        // Profile image
        val photoUrl = user.photoUrl?.toString()
        if (!photoUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(photoUrl)
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .fit().centerCrop()
                .into(binding.ivDialogProfileImage)
        }
    }

    private fun setupClickListeners() {
        binding.btnChangePhoto.setOnClickListener {
            showImageSourceChooser()
        }

        binding.btnDialogCancel.setOnClickListener {
            dismiss()
        }

        binding.btnDialogSave.setOnClickListener {
            // Clear previous error
            binding.tilDialogDisplayName.error = null

            val displayName = binding.etDialogDisplayName.text.toString().trim()
            if (displayName.isBlank()) {
                binding.tilDialogDisplayName.error = "Display name cannot be empty"
                return@setOnClickListener
            }
            profileViewModel.updateProfile(displayName, selectedImageUri)
        }
    }

    private fun setupObservers() {
        profileViewModel.isUpdatingProfile.observe(viewLifecycleOwner) { updating ->
            binding.progressBar.visibility = if (updating) View.VISIBLE else View.GONE
            binding.btnDialogSave.isEnabled = !updating
            binding.btnDialogCancel.isEnabled = !updating
            binding.btnChangePhoto.isEnabled = !updating
        }

        profileViewModel.profileUpdateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                // Show branded Snackbar on the activity's root so it
                // persists after the dialog is dismissed.
                activity?.findViewById<View>(android.R.id.content)?.let { root ->
                    BrandedSnackbar.showSuccess(root, getString(R.string.profile_updated))
                }
                profileViewModel.clearProfileUpdateSuccess()
                dismiss()
            }
        }

        profileViewModel.profileUpdateError.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.tilDialogDisplayName.error = it
                profileViewModel.clearProfileUpdateError()
            }
        }
    }

    /**
     * Clear the inline error when the user starts editing.
     */
    private fun setupTextWatchers() {
        binding.etDialogDisplayName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilDialogDisplayName.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun showImageSourceChooser() {
        val options = arrayOf("Camera", "Gallery")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(
                                requireContext(), Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) launchCamera()
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun launchCamera() {
        val dir = File(requireContext().cacheDir, "images").also { it.mkdirs() }
        val file = File(dir, "profile_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        cameraLauncher.launch(currentPhotoUri!!)
    }

    private fun copyToLocalFile(uri: Uri): Uri? {
        val file = File(requireContext().cacheDir, "profile_copy_${System.currentTimeMillis()}.jpg")
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            Uri.fromFile(file)
        } catch (_: Exception) { null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
