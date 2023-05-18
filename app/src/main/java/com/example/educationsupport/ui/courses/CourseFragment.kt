package com.example.educationsupport.ui.courses

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.educationsupport.MainActivity
import com.example.educationsupport.R
import com.example.educationsupport.databinding.FragmentCourseBinding
import com.example.educationsupport.db.Firebase
import com.example.educationsupport.db.objects.Activity
import com.example.educationsupport.db.objects.Course
import com.example.educationsupport.db.objects.User
import com.example.educationsupport.ui.theme.EducationSupportComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class CourseFragment : Fragment() {

    private var _binding: FragmentCourseBinding? = null
    private val binding get() = _binding!!

    // UI
    private lateinit var search: MenuItem

    // Firebase
    private var realtimeFirebase = Firebase()
    private lateinit var auth: FirebaseAuth

    // Users
    private lateinit var usersTable: DatabaseReference
    private var educator = true

    // Courses
    private lateinit var coursesTable: DatabaseReference
    private lateinit var courseId: String
    private var course = Course()

    // Activities
    private lateinit var activitiesTable: DatabaseReference
    private var activitiesMap = mutableMapOf<String, Activity>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()
        usersTable = realtimeFirebase.getUsersTable()
        coursesTable = realtimeFirebase.getCoursesTable()
        activitiesTable = realtimeFirebase.getActivitiesTable()
        _binding = FragmentCourseBinding.inflate(inflater, container, false)
        return binding.root
    }

    // this function is automatically called after onCreateView
    // here execute all bindings for buttons or any backend logic
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeData()
        inflateTopMenu()
        createAddNewActivityButton()
        getCourseActivitiesFromDB(course.activityIds.keys)
    }

    /**
     * Used for manipulating the top bar buttons(menu) etc. on the current fragment
     */
    private fun inflateTopMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                search = menu.findItem(R.id.search_bar)
                search.isVisible = true

                (requireActivity() as MainActivity).supportActionBar?.title = course.name

                (search.actionView as SearchView).setOnQueryTextListener(object :
                    SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(p0: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(p0: String?): Boolean {
                        if (p0 != null) {
                            //filter courses
                            updateComposeView(activitiesMap.filter { (_, activity) ->
                                (activity.name).contains(
                                    p0,
                                    ignoreCase = true
                                )
                            } as MutableMap<String, Activity>)
                        }
                        return true
                    }
                })
            }

            //setup navigation on selection of option/ fragment specific
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.search_bar -> {
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initializeData() {
        // Gets the arguments
        courseId = arguments?.getString("courseId").toString()
        course = arguments?.getSerializable("course") as Course
        educator = arguments?.getBoolean("educator") == true
    }

    /**
     * Gets the activities from the database
     */
    private fun getCourseActivitiesFromDB(activityIds: MutableSet<String>) {
        activitiesMap = mutableMapOf()

        activitiesTable.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataSnap in dataSnapshot.children) {
                    if (dataSnap.key in activityIds) {
                        activitiesMap[dataSnap.key!!] = dataSnap.getValue(Activity::class.java)!!
                    }
                }
                updateComposeView(activitiesMap)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    /**
     * Applies listeners to the expandable FAB
     */
    private fun createAddNewActivityButton() {
        if (educator) {
            binding.openFabsButton.visibility = View.VISIBLE

            binding.openFabsButton.setOnClickListener {
                if (binding.newActivityButton.visibility == View.INVISIBLE) {
                    binding.newActivityButton.visibility = View.VISIBLE
                    binding.showLearnersButton.visibility = View.VISIBLE
                } else {
                    binding.newActivityButton.visibility = View.INVISIBLE
                    binding.showLearnersButton.visibility = View.INVISIBLE
                }
            }

            // Add new activities button
            binding.newActivityButton.setOnClickListener {
                val dialog = Dialog(this.requireContext())
                dialog.setContentView(R.layout.dialog_new_activity)

                dialog.findViewById<Button>(R.id.add_activity_button).setOnClickListener {
                    val activityName = dialog.findViewById<EditText>(R.id.activityName).text
                    val activityDescription =
                        dialog.findViewById<EditText>(R.id.activityDescription).text

                    if (activityName.toString().isNotEmpty() && activityDescription.toString()
                            .isNotEmpty()
                    ) {
                        // adds activity and refreshes data again after successful add
                        addActivity(
                            Activity(
                                courseId,
                                activityName.toString(),
                                activityDescription.toString()
                            ), courseId, course
                        )
                        dialog.dismiss()
                    }
                }
                dialog.findViewById<Button>(R.id.cancel_activity_button).setOnClickListener {
                    dialog.dismiss()
                }
                dialog.show()
            }

            // Adds a learner to the course
            binding.showLearnersButton.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("courseId", courseId)
                bundle.putSerializable("course", course)
                findNavController().navigate(
                    R.id.action_navigation_courseFragment_to_courseLearnersFragment,
                    bundle
                )
            }
        }
    }

    /**
     * Adds an activity to the database
     *
     * @param activity The new activity
     * @param courseId The current course id
     * @param course The current course
     */
    private fun addActivity(activity: Activity, courseId: String, course: Course) {
        // Generating a new activityId
        val activityId = activitiesTable.push().key
        if (activityId != null) {
            // Adding new activity to the database
            activitiesTable.child(activityId).setValue(activity)
                .addOnSuccessListener {
                    course.activityIds[activityId] = true
                    // updating course activity ids
                    coursesTable.child(courseId).setValue(course)
                        .addOnSuccessListener {
                            getCourseActivitiesFromDB(course.activityIds.keys)
                        }
                }
        }
    }

    /**
     * Updates the courses view
     */
    private fun updateComposeView(activitiesMap: MutableMap<String, Activity>) {
        try {
            binding.composeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    EducationSupportComposeTheme {
                        Surface(color = MaterialTheme.colors.background) {
                            LazyColumnClickable(activitiesMap) {
                                val bundle = Bundle()
                                bundle.putString("activityID", it.first)
                                bundle.putSerializable("activity", it.second)
                                bundle.putSerializable("course", course)
                                if (educator) {
                                    findNavController().navigate(
                                        R.id.action_navigation_courseFragment_to_navigation_manageQuestionsFragment,
                                        args = bundle
                                    )
                                } else {
                                    // user is learner, show them activity
                                    findNavController().navigate(
                                        R.id.action_navigation_courseFragment_to_navigation_activityFragment,
                                        args = bundle
                                    )
                                }

                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Column
     */
    @Composable
    private fun LazyColumnClickable(
        activities: MutableMap<String, Activity>,
        selectedActivity: (Pair<String, Activity>) -> Unit
    ) {
        if (activities.isEmpty()) EmptyList() // first empty check
        else {
            LazyColumn {
                items(
                    items = activities.toList(),
                    itemContent = {
                        ListItem(activity = it, selectedActivity = selectedActivity)
                    }
                )
            }
        }
    }

    /**
     * Course button
     */
    @Composable
    private fun ListItem(
        activity: Pair<String, Activity>,
        selectedActivity: (Pair<String, Activity>) -> Unit
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clickable { selectedActivity(activity) },
            elevation = 4.dp,
        ) {
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 8.dp,
                    backgroundColor = MaterialTheme.colors.secondary
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
                                activity.second.name,
                                style = MaterialTheme.typography.subtitle1,
                                modifier = Modifier
                                    .padding(start = 15.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_remove_red_eye_24),
                                contentDescription = "Open course icon",
                                Modifier
                                    .align(Alignment.End)
                                    .padding(top = 12.dp, bottom = 12.dp, end = 10.dp)
                            )
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
                        "Description: ${activity.second.description}",
                        style = MaterialTheme.typography.subtitle2,
                        modifier = Modifier
                            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                            .fillMaxSize()
                    )
                }
                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_question_mark_24),
                        contentDescription = "Questions icon",
                        modifier = Modifier
                            .padding(start = 15.dp, end = 5.dp, top = 8.dp, bottom = 8.dp)
                            .size(20.dp)
                    )
                    Text(
                        "Questions: ${activity.second.questionIDs.size}",
                        style = MaterialTheme.typography.subtitle2,
                        modifier = Modifier
                            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                            .fillMaxSize()
                    )
                }
                if (educator) {
                    Row(
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_learners),
                            contentDescription = "Results icon",
                            modifier = Modifier
                                .padding(start = 15.dp, end = 5.dp, top = 8.dp, bottom = 8.dp)
                                .fillMaxHeight(),
                        )
                        Text(
                            "Results: " + activity.second.resultIDs.size,
                            style = MaterialTheme.typography.subtitle2,
                            modifier = Modifier
                                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }

    /**
     * What to show when list is empty
     */
    @Composable
    private fun EmptyList() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No Activities Found",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}