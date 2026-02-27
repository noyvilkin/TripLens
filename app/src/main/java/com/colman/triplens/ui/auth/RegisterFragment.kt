package com.colman.triplens.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.colman.triplens.auth.AuthViewModel
import com.colman.triplens.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        setupObservers()
        setupClickListeners()
        setupTextWatchers()
        return binding.root
    }

    private fun setupObservers() {
        authViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.registerButton.isEnabled = !loading
        }

        authViewModel.authError.observe(viewLifecycleOwner) { error ->
            error?.let {
                showServerError(it)
                authViewModel.clearError()
            }
        }

        authViewModel.authSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                authViewModel.clearAuthSuccess()
                navigateToFeed()
            }
        }
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            if (validateForm()) {
                val displayName = binding.displayName.text.toString().trim()
                val email = binding.email.text.toString().trim()
                val password = binding.password.text.toString().trim()
                clearAllErrors()
                authViewModel.register(email, password, displayName)
            }
        }

        binding.loginText.setOnClickListener {
            val action = RegisterFragmentDirections.actionRegisterFragmentToLoginFragment()
            findNavController().navigate(action)
        }
    }

    /**
     * Clear field errors when the user starts typing.
     */
    private fun setupTextWatchers() {
        binding.displayName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilDisplayName.error = null
        }
        binding.email.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) { binding.tilEmail.error = null; binding.tvError.visibility = View.GONE }
        }
        binding.password.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilPassword.error = null
        }
        binding.confirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilConfirmPassword.error = null
        }
    }

    /**
     * Validate all form fields and show inline errors.
     * Returns true only if everything is valid.
     */
    private fun validateForm(): Boolean {
        clearAllErrors()
        var isValid = true

        val displayName = binding.displayName.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val confirmPassword = binding.confirmPassword.text.toString().trim()

        if (displayName.isEmpty()) {
            binding.tilDisplayName.error = "Display name is required"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email address"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }

    /**
     * Show a server-side error (e.g. "email already in use", "display name taken")
     * mapped to the correct field when possible, otherwise shown in the general error label.
     */
    private fun showServerError(message: String) {
        val lower = message.lowercase()
        when {
            lower.contains("display name") -> {
                binding.tilDisplayName.error = message
            }
            lower.contains("email") && (lower.contains("already") || lower.contains("in use")) -> {
                binding.tilEmail.error = "This email is already in use"
            }
            lower.contains("email") && (lower.contains("invalid") || lower.contains("badly formatted")) -> {
                binding.tilEmail.error = "Please enter a valid email address"
            }
            lower.contains("password") && lower.contains("weak") -> {
                binding.tilPassword.error = "Password is too weak — use at least 6 characters"
            }
            else -> {
                // Generic fallback — show in the general error label
                binding.tvError.text = message
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun clearAllErrors() {
        binding.tilDisplayName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.tvError.visibility = View.GONE
    }

    private fun navigateToFeed() {
        val action = RegisterFragmentDirections.actionRegisterFragmentToHomeFragment()
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
