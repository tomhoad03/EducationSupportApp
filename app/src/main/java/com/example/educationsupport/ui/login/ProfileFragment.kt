package com.example.educationsupport.ui.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.educationsupport.R
import com.example.educationsupport.databinding.FragmentProfileBinding
import com.example.educationsupport.db.Firebase
import com.example.educationsupport.db.objects.User
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var startForProfileImageResult: ActivityResultLauncher<Intent>

    private val firebaseInst: Firebase = Firebase()
    private lateinit var auth: FirebaseAuth
    private lateinit var user: User
    private val usersTable = firebaseInst.getUsersTable()

    private var photoUri: Uri = Uri.EMPTY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        startForProfileImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                when (resultCode) {
                    Activity.RESULT_OK -> {
                        photoUri = data?.data!!
                        binding.photoView.setImageURI(photoUri)
                    }

                    ImagePicker.RESULT_ERROR -> Toast.makeText(
                        requireContext(),
                        ImagePicker.getError(data),
                        Toast.LENGTH_SHORT
                    ).show()

                    else -> Toast.makeText(requireContext(), "Upload Cancelled", Toast.LENGTH_LONG)
                        .show()
                }
            }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getUserDataFromDB()
        setupButtons()
    }

    /**
     * Gets the user from the database
     */
    private fun getUserDataFromDB() {
        usersTable.child(auth.uid!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                user = dataSnapshot.getValue(User::class.java)!! // gets user object from db
                addDataToFields()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun setupButtons() {
        binding.fabCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.fabSave.setOnClickListener {
            binding.fabSave.isEnabled = false
            updateUserObject()
            tryUpdateUser()
        }

        binding.buttonSelectPhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop(1f, 1f)
                .compress(1024)
                .maxResultSize(
                    360,
                    360
                )
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
        }
    }

    private fun updateUserObject() {
        user.name = binding.nameInput.text.toString()
        user.bio = binding.bioInput.text.toString()
    }
    

    private fun tryUpdateUser() {
        if (checkFields()) {
            binding.fabSave.text = "Saving..."
            if (photoUri != Uri.EMPTY) {
                removeUserPicture()
            } else {
                editUser()
            }
        } else {
            binding.fabSave.isEnabled = true
        }
    }

    private fun removeUserPicture() {
        if (user.pictureURL == "") {
            editUserWithPicture(photoUri)
        } else {
            val pictureRef = firebaseInst.getFirebaseStorage()
                .getReferenceFromUrl(user.pictureURL)
            pictureRef.delete()
                .addOnSuccessListener {
                    editUserWithPicture(photoUri)
                }.addOnFailureListener {
                    failEdit()
                }
        }
    }

    private fun editUserWithPicture(pictureLocalUri: Uri) {
        val pictureRef = firebaseInst.getProfilePicturesFolder().child(
            "${pictureLocalUri.lastPathSegment}"
        )

        pictureRef.putFile(pictureLocalUri).addOnSuccessListener {
            pictureRef.downloadUrl.addOnSuccessListener {
                user.pictureURL = it.toString()
                usersTable.child(auth.uid!!).setValue(user)
                    .addOnSuccessListener {
                        successEdit()
                    }.addOnFailureListener {
                        failEdit()
                    }
            }
        }
    }

    private fun editUser() {
        usersTable.child(auth.uid!!).setValue(user)
            .addOnSuccessListener {
                successEdit()
            }.addOnFailureListener {
                failEdit()
            }
    }

    private fun addDataToFields() {
        binding.nameInput.setText(user.name)
        binding.bioInput.setText(user.bio)
        binding.emailInput.setText(user.email)
        if (user.educator) {
            binding.roleText.text = "Educator"
        } else {
            binding.roleText.text = "Learner"
        }

        if (user.pictureURL != "") {
            Glide.with(this).load(user.pictureURL).into(binding.photoView)
        } else {
            binding.photoView.setImageResource(R.drawable.ic_person_24)
        }
    }

    private fun checkFields(): Boolean {
        var check = true
        if (binding.nameInput.text.toString() == "") {
            binding.nameDescription.error = "You need to add Your Name"
            check = false
        } else {
            binding.nameDescription.error = null
        }
        return check
    }

    private fun successEdit() {
        findNavController().navigateUp()
    }

    private fun failEdit() {
        Toast.makeText(
            requireContext(),
            "Error editing user " + user.name,
            Toast.LENGTH_LONG
        ).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}