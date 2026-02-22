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
import com.colman.triplens.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Skip login screen if session is still valid
        if (authViewModel.shouldAutoLogin()) {
            navigateToFeed()
            return binding.root
        }

        setupObservers()
        setupClickListeners()
        setupTextWatchers()
        return binding.root
    }

    private fun setupObservers() {
        authViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.loginButton.isEnabled = !loading
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
        binding.loginButton.setOnClickListener {
            if (validateForm()) {
                clearAllErrors()
                val email = binding.email.text.toString().trim()
                val password = binding.password.text.toString().trim()
                authViewModel.login(email, password, binding.stayLoggedInCheckbox.isChecked)
            }
        }

        binding.registerText.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            findNavController().navigate(action)
        }
    }

    /**
     * Clear field errors when the user starts typing.
     */
    private fun setupTextWatchers() {
        binding.email.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) { binding.tilEmail.error = null; binding.tvError.visibility = View.GONE }
        }
        binding.password.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) { binding.tilPassword.error = null; binding.tvError.visibility = View.GONE }
        }
    }

    /**
     * Validate form fields and show inline errors.
     */
    private fun validateForm(): Boolean {
        clearAllErrors()
        var isValid = true

        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()

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
        }

        return isValid
    }

    /**
     * Map Firebase server errors to the correct field whenever possible,
     * otherwise show in the general error label.
     */
    private fun showServerError(message: String) {
        val lower = message.lowercase()
        when {
            lower.contains("no user") || lower.contains("user not found") ||
            lower.contains("no account") -> {
                binding.tilEmail.error = "No account found with this email"
            }
            lower.contains("password") && (lower.contains("wrong") || lower.contains("invalid")) -> {
                binding.tilPassword.error = "Incorrect password"
            }
            lower.contains("email") && (lower.contains("invalid") || lower.contains("badly formatted")) -> {
                binding.tilEmail.error = "Please enter a valid email address"
            }
            lower.contains("credential") || lower.contains("invalid") -> {
                binding.tvError.text = "Invalid email or password"
                binding.tvError.visibility = View.VISIBLE
            }
            lower.contains("network") || lower.contains("connection") -> {
                binding.tvError.text = "Network error — please check your connection"
                binding.tvError.visibility = View.VISIBLE
            }
            else -> {
                binding.tvError.text = message
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun clearAllErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tvError.visibility = View.GONE
    }

    private fun navigateToFeed() {
        val action = LoginFragmentDirections.actionLoginFragmentToHomeFragment()
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
