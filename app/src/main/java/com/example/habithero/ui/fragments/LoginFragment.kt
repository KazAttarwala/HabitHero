package com.example.habithero.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.habithero.R
import com.example.habithero.databinding.FragmentLoginBinding
import com.example.habithero.repository.UserRepository
import com.example.habithero.ui.viewmodels.QuoteViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private val quoteViewModel: QuoteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase Auth and UserRepository
        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()
        
        // Check if user is already signed in
        if (auth.currentUser != null) {
            // User already signed in, navigate to home
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        // Set up observers for the quote
        setupQuoteObservers()
        
        // Set up swipe refresh layout
        setupSwipeRefresh()
        
        // Fetch a quote initially
        quoteViewModel.fetchQuote()

        // Handle login button click
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (validateForm(email, password)) {
                showLoading(true)
                loginUser(email, password)
            }
        }

        // Handle register button click
        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (validateForm(email, password)) {
                showLoading(true)
                registerUser(email, password)
            }
        }
        
        // Handle forgot password click
        binding.forgotPasswordTextView.setOnClickListener {
            showPasswordResetDialog()
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            quoteViewModel.fetchQuote()
        }
        
        // Colors for the refresh indicator
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.purple_500,
            R.color.teal_200,
            R.color.purple_700
        )
    }
    
    private fun setupQuoteObservers() {
        // Observe quote
        quoteViewModel.quote.observe(viewLifecycleOwner) { quoteResponse ->
            binding.quoteTextView.text = "\"${quoteResponse.quote}\""
            binding.quoteAuthorTextView.text = "- ${quoteResponse.author}"
        }
        
        // Observe loading state
        quoteViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            if (isLoading) {
                binding.quoteTextView.text = "Loading quote..."
            }
        }
        
        // Observe errors
        quoteViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                // Only log the error, don't show to user as we'll display a fallback quote
                // We also don't want to annoy the user with quote fetch errors when they're trying to log in
                // Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    // Login successful
                    Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                } else {
                    // Login failed
                    handleAuthError(task.exception)
                }
            }
    }
    
    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Registration successful, store user data in Firestore
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        userRepository.createUserAfterRegistration(firebaseUser)
                            .addOnSuccessListener {
                                showLoading(false)
                                Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                            }
                            .addOnFailureListener { e ->
                                showLoading(false)
                                Toast.makeText(requireContext(), "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // Registration failed
                    showLoading(false)
                    handleAuthError(task.exception)
                }
            }
    }
    
    private fun handleAuthError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Must be at least 6 characters."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
            is FirebaseAuthInvalidUserException -> "No account found with this email."
            is FirebaseAuthUserCollisionException -> "Account already exists with this email."
            else -> "Authentication failed: ${exception?.message}"
        }
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
    }
    
    private fun validateForm(email: String, password: String): Boolean {
        var isValid = true
        
        if (email.isEmpty()) {
            binding.emailEditText.error = "Required"
            isValid = false
        } else {
            binding.emailEditText.error = null
        }
        
        if (password.isEmpty()) {
            binding.passwordEditText.error = "Required"
            isValid = false
        } else if (password.length < 6) {
            binding.passwordEditText.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.passwordEditText.error = null
        }
        
        return isValid
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.loginButton.isEnabled = !isLoading
        binding.registerButton.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showPasswordResetDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Reset Password")
        
        // Set up the input
        val inputLayout = layoutInflater.inflate(R.layout.dialog_reset_password, null)
        val emailInput = inputLayout.findViewById<TextInputEditText>(R.id.emailInputEditText)
        
        // Pre-fill with email if already entered in login form
        if (binding.emailEditText.text.isNotEmpty()) {
            emailInput.setText(binding.emailEditText.text.toString())
        }
        
        builder.setView(inputLayout)
        
        // Set up the buttons
        builder.setPositiveButton("Reset") { dialog, _ ->
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your email address", Toast.LENGTH_SHORT).show()
            } else {
                resetPassword(email)
                dialog.dismiss()
            }
        }
        
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        
        builder.show()
    }

    private fun resetPassword(email: String) {
        showLoading(true)
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Password reset instructions sent to your email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 