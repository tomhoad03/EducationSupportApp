package com.example.educationsupport.ui.explore

import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.educationsupport.R
import com.example.educationsupport.databinding.FragmentExploreBinding
import com.example.educationsupport.db.objects.Activity
import com.example.educationsupport.db.objects.Course
import com.example.educationsupport.db.objects.User
import com.example.educationsupport.ui.theme.EducationSupportComposeTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.example.educationsupport.db.Firebase as CustomFirebase

class ExploreFragment : Fragment() {
    private var _binding: FragmentExploreBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // UI
    private val displayMetrics = DisplayMetrics()
    private lateinit var search: MenuItem

    // Firebase
    private var realtimeFirebase = CustomFirebase()
    private lateinit var auth: FirebaseAuth

    // Users
    private lateinit var usersTable: DatabaseReference
    private lateinit var user: User

    // Courses
    private lateinit var coursesTable: DatabaseReference
    private var coursesMap = mutableMapOf<String, Course>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()
        usersTable = realtimeFirebase.getUsersTable()
        coursesTable = realtimeFirebase.getCoursesTable()
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    // this function is automatically called after onCreateView
    // here execute all bindings for buttons or any backend logic
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inflateTopMenu()

        // TODO Potentially deprecated, revisit later
        this.requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)

        // Get current user
        getUserDataFromDB()
    }

    /**
     * Used for manipulating the top bar buttons(menu) etc. on the current fragment
     */
    private fun inflateTopMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.findItem(R.id.action_sign_out).isVisible = true
                menu.findItem(R.id.action_profile).isVisible = true
                search = menu.findItem(R.id.search_bar)
                search.isVisible = true

                (search.actionView as SearchView).setOnQueryTextListener(object :
                    SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(p0: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(p0: String?): Boolean {
                        if (p0 != null) {
                            //filter courses
                            updateComposeView(coursesMap.filter { (_, course) ->
                                (course.name).contains(
                                    p0,
                                    ignoreCase = true
                                )
                            } as MutableMap<String, Course>)
                        }
                        return true
                    }
                })
            }

            //setup navigation on selection of option/ fragment specific
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_profile -> {
                        findNavController().navigate(R.id.action_navigation_exploreFragment_to_profileFragment)
                        true
                    }

                    R.id.action_sign_out -> {
                        signOut()
                        true
                    }

                    R.id.search_bar -> {
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * Gets the user from the database
     */
    private fun getUserDataFromDB() {
        usersTable.child(auth.uid!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                user = dataSnapshot.getValue(User::class.java)!! // gets user object from db
                getUserCoursesFromDB(user.courseIDs.keys) //uses course keys from user object
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    /**
     * Gets the courses from the database
     */
    private fun getUserCoursesFromDB(courseIDs: MutableSet<String>) {
        coursesMap= mutableMapOf()

        coursesTable.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataSnap in dataSnapshot.children) {
                    if (dataSnap.key in courseIDs) {
                        continue
                    } else {
                        coursesMap[dataSnap.key!!] = dataSnap.getValue(Course::class.java)!!
                    }
                }
                updateComposeView(coursesMap)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    /**
     * Column
     */
    @Composable
    private fun LazyColumnClickable(
        courses: MutableMap<String, Course>,
        selectedCourse: (Pair<String, Course>) -> Unit
    ) {
        if (courses.isEmpty()) EmptyList() // first empty check
        else {
            LazyColumn {
                items(
                    items = courses.toList(),
                    itemContent = {
                        ListItem(course = it, selectedCourse = selectedCourse)
                    }
                )
            }
        }
    }

    /**
     * Updates the explore view
     */
    private fun updateComposeView(coursesMap: MutableMap<String, Course>) {
        try {
            binding.composeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    EducationSupportComposeTheme {
                        Surface(color = MaterialTheme.colors.background) {
                            LazyColumnClickable(coursesMap) {
                                // ask user to join
                                joinConfirmationDialog(it.first, it.second)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun joinConfirmationDialog(courseId: String, course: Course) {
        if (!user.educator) {
            val dialog = Dialog(this.requireContext())
            dialog.setContentView(R.layout.dialog_join_course)
            dialog.findViewById<Button>(R.id.confirm_join_button).setOnClickListener {
                addUserToCourse(courseId, course)
                dialog.dismiss()
            }
            dialog.findViewById<Button>(R.id.cancel_join_button).setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    private fun addUserToCourse(courseId: String, course: Course) {
        // Gets the user with the matching email from the db
        usersTable.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataSnap in dataSnapshot.children) {
                    val dbUser = dataSnap.getValue(User::class.java)!!
                    if (dbUser.email == user.email) {
                        course.learnerIds[dataSnap.key.toString()] = true
                        dbUser.courseIDs[courseId] = true

                        // Updates the course with the new learner id
                        coursesTable.child(courseId).setValue(course)

                        // Updates the user with the new course id and refreshes list
                        usersTable.child(dataSnap.key.toString()).setValue(dbUser)
                            .addOnSuccessListener {
                                coursesMap = mutableMapOf<String, Course>()
                                getUserDataFromDB()
                                showConfirmationToast()
                            }
                        break
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun showConfirmationToast() {
        Toast.makeText(
            this.requireContext(), "Course joined succesfully.",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Course button
     */
    @Composable
    private fun ListItem(
        course: Pair<String, Course>,
        selectedCourse: (Pair<String, Course>) -> Unit
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clickable { selectedCourse(course) },
            elevation = 4.dp,
        ) {
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 8.dp,
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = Color.White
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp)
                            .height(50.dp)
                    ) {
                        Column {
                            Text(
                                course.second.name,
                                style = MaterialTheme.typography.subtitle1,
                                modifier = Modifier
                                    .padding(start = 15.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!user.educator) {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_add_24),
                                    contentDescription = "Open course icon",
                                    Modifier
                                        .align(Alignment.End)
                                        .padding(top = 12.dp, bottom = 12.dp, end = 10.dp)
                                )
                            }
                        }

                    }

                }
                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.description),
                        contentDescription = "Description icon",
                        modifier = Modifier
                            .padding(start = 15.dp, end = 5.dp, top = 8.dp, bottom = 8.dp)
                            .size(20.dp)
                    )
                    Text(
                        "Description: ${course.second.description}",
                        style = MaterialTheme.typography.subtitle2,
                        modifier = Modifier
                            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                            .fillMaxSize()
                    )
                }
                Row(
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_learners),
                        contentDescription = "Learners icon",
                        modifier = Modifier
                            .padding(start = 15.dp, end = 5.dp, top = 8.dp, bottom = 8.dp)
                            .fillMaxHeight(),
                    )
                    Text(
                        "Activities: " + course.second.activityIds.size,
                        style = MaterialTheme.typography.subtitle2,
                        modifier = Modifier
                            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }

    /**
     * List item
     */
    @Composable
    private fun EmptyList() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No Courses Found",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    /**
     * Signs out the user
     */
    private fun signOut() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Do you want to Sign Out?")
            .setNegativeButton("Cancel") { _, _ -> }
            .setPositiveButton("Sign Out") { _, _ ->
                auth.signOut()
                findNavController().navigate(R.id.action_navigation_exploreFragment_to_navigation_loginFragment)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}