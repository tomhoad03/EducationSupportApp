package com.example.educationsupport.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.educationsupport.R
import com.example.educationsupport.databinding.FragmentLoginBinding
import com.example.educationsupport.db.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DatabaseReference
import com.example.educationsupport.db.Firebase as CustomFirebase

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private var realtimeFirebase = CustomFirebase()
    private lateinit var auth: FirebaseAuth

    // Users
    private lateinit var usersTable: DatabaseReference

    // standard onCreateView, executed upon entering of fragment
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance() // we need that here
        usersTable = realtimeFirebase.getUsersTable()
        return binding.root
    }

    // this function is automatically called after onCreateView
    // here execute all bindings for buttons or any backend logic
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons() // setup supporter button
    }

    /**
     * Adds listeners to buttons
     */
    private fun setupButtons() {
        binding.buttonEducator.setOnClickListener {
            signUp(
                binding.emailField.editText?.text.toString(),
                binding.passwordField.editText?.text.toString(),
                true
            )
        }

        binding.buttonLearner.setOnClickListener {
            signUp(
                binding.emailField.editText?.text.toString(),
                binding.passwordField.editText?.text.toString(),
                false
            )
        }
    }

    /**
     * Registers the user
     */
    private fun signUp(email: String, password: String, educator: Boolean) {
        // Check email and password were entered
        if (email == "" || password == "") {
            Toast.makeText(
                this.requireContext(), "Logging in with test account",
                Toast.LENGTH_SHORT
            ).show()
            if (educator) {
                auth.signInWithEmailAndPassword("educator@email.com", "educator")
                    .addOnSuccessListener {
                        findNavController().navigate(R.id.action_navigation_loginFragment_to_navigation_mycoursesFragment)
                    }
            } else {
                auth.signInWithEmailAndPassword("learner@email.com", "learner")
                    .addOnSuccessListener {
                        findNavController().navigate(R.id.action_navigation_loginFragment_to_navigation_mycoursesFragment)
                    }
            }
            return
        }

        // Check password is long enough
        if (password.length < 6) {
            Toast.makeText(
                this.requireContext(), "Password must be at least 6 characters",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this.requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    // Add user to Realtime db
                    addUser(User(email, educator = educator, name = "New Name"), auth.uid,password)

                    findNavController().navigate(R.id.action_navigation_loginFragment_to_navigation_mycoursesFragment)
                } else {
                    // If sign in fails, display a message to the user.
                    // If it's because email already exists then log in
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        logIn(email, password)
                    } else {
                        Toast.makeText(
                            this.requireContext(), "Registration failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    /**
     * Logs in the user
     */
    private fun logIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                findNavController().navigate(R.id.action_navigation_loginFragment_to_navigation_mycoursesFragment)
            } else {
                // If sign in fails, display a message to the user.
                Toast.makeText(
                    this.requireContext(), "Login failed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Adds a user to the database
     */
    private fun addUser(user: User, userID: String? = usersTable.push().key,loginPassword: String) {
        var registered = false

        // Adding new user to the database
        if (userID != null) {
            usersTable.child(userID).setValue(user)
                .addOnSuccessListener {
                    Toast.makeText(
                        this.requireContext(), "Register complete.",
                        Toast.LENGTH_SHORT
                    ).show()
                    registered = true
                }.addOnFailureListener {
                    Toast.makeText(
                        this.requireContext(), "Register Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            if (registered) {
                logIn(user.email, loginPassword)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}