package com.colman.triplens.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.colman.triplens.NavGraphDirections
import com.colman.triplens.auth.AuthViewModel
import com.colman.triplens.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        setupObservers()
        setupClickListeners()
        return binding.root
    }

    private fun setupObservers() {
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            binding.userEmail.text = user?.email ?: ""
        }
    }

    private fun setupClickListeners() {
        binding.logoutButton.setOnClickListener {
            authViewModel.logout()
            val action = NavGraphDirections.actionGlobalLoginFragment()
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
