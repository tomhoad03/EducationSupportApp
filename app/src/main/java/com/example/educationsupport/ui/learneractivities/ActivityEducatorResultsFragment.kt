package com.example.educationsupport.ui.learneractivities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.example.educationsupport.MainActivity
import com.example.educationsupport.R
import com.example.educationsupport.databinding.FragmentActivityEducatorResultsBinding
import com.example.educationsupport.db.Firebase
import com.example.educationsupport.db.objects.Course
import com.example.educationsupport.db.objects.Result
import com.example.educationsupport.db.objects.User
import com.example.educationsupport.ui.theme.EducationSupportComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class ActivityEducatorResultsFragment : Fragment() {

    private var _binding: FragmentActivityEducatorResultsBinding? = null
    private val binding get() = _binding!!

    // UI
    private lateinit var search: MenuItem

    // Firebase
    private var realtimeFirebase = Firebase()
    private lateinit var auth: FirebaseAuth

    // Users
    private lateinit var usersTable: DatabaseReference

    // Courses
    private var course = Course()

    // Activities
    private lateinit var activityId: String
    private var learnersMap = mutableMapOf<String, User>()

    // Results
    private lateinit var resultsTable: DatabaseReference
    private var resultsMap = mutableMapOf<String, Result>()

    // Data
    private var userResultInfoMap = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()
        usersTable = realtimeFirebase.getUsersTable()
        resultsTable = realtimeFirebase.getResultsTable()
        _binding = FragmentActivityEducatorResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // this function is automatically called after onCreateView
    // here execute all bindings for buttons or any backend logic
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeData()
        inflateTopMenu()
        getLearnersDataFromDB()
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

                (search.actionView as SearchView).setOnQueryTextListener(object :
                    SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(p0: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(p0: String?): Boolean {
                        if (p0 != null) {
                            //filter users by both email and name at the same time
                            updateComposeView(learnersMap.filter { (_, user) ->
                                (user.email).contains(
                                    p0,
                                    ignoreCase = true
                                ).or(
                                    (user.name).contains(
                                        p0,
                                        ignoreCase = true
                                    )
                                )
                            } as MutableMap<String, User>)
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
        activityId = arguments?.getString("activityID").toString()
        course = arguments?.getSerializable("course") as Course
    }

    private fun setupLearnerQuestionText() {
        resultsMap.forEach {
            if (learnersMap.containsKey(it.value.userID)) {
                userResultInfoMap[it.value.userID] =
                    "Correct answers: ${it.value.numberOfCorrectQuestions}"
            }
        }
    }

    /**
     * Gets the user from the database
     */
    private fun getLearnersDataFromDB() {
        learnersMap = mutableMapOf() //resets learners map

        usersTable.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataSnap in dataSnapshot.children) {
                    if (dataSnap.key in course.learnerIds) {
                        learnersMap[dataSnap.key!!] = dataSnap.getValue(User::class.java)!!
                    }
                }
                getActivityResultsDataFromDB()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    /**
     * Gets the user from the database
     */
    private fun getActivityResultsDataFromDB() {
        resultsMap = mutableMapOf() //resets learners map

        resultsTable.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataSnap in dataSnapshot.children) {
                    if (dataSnap.getValue(Result::class.java)!!.activityID == activityId) {
                        resultsMap[dataSnap.key!!] = dataSnap.getValue(Result::class.java)!!
                    }
                }
                setupLearnerQuestionText()
                updateComposeView(learnersMap)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    /**
     * Updates the courses view
     */
    private fun updateComposeView(learnersMap: MutableMap<String, User>) {
        try {
            binding.composeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    EducationSupportComposeTheme {
                        Surface(color = MaterialTheme.colors.background) {
                            LazyColumn(learnersMap)
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
    private fun LazyColumn(
        learners: MutableMap<String, User>,
    ) {
        if (learners.isEmpty()) EmptyList() // first empty check
        else {
            LazyColumn {
                items(
                    items = learners.toList(),
                    itemContent = {
                        ListItem(learner = it)
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
        learner: Pair<String, User>,
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
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
                                learner.second.name,
                                style = MaterialTheme.typography.subtitle1,
                                modifier = Modifier
                                    .padding(start = 15.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_email_24),
                        contentDescription = "Email icon",
                        modifier = Modifier
                            .padding(start = 15.dp, end = 5.dp, top = 8.dp, bottom = 8.dp)
                            .size(20.dp)
                    )
                    Text(
                        learner.second.email,
                        style = MaterialTheme.typography.subtitle2,
                        modifier = Modifier
                            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                            .fillMaxSize()
                    )
                }

                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.description),
                        contentDescription = "Description",
                        modifier = Modifier
                            .padding(start = 15.dp, end = 5.dp, top = 8.dp, bottom = 8.dp)
                            .size(20.dp)
                    )
                    Text(
                        if (userResultInfoMap[learner.first].isNullOrEmpty()) {
                            "No Results"
                        } else {
                            userResultInfoMap[learner.first].toString()
                        },
                        style = MaterialTheme.typography.subtitle2,
                        modifier = Modifier
                            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                            .fillMaxSize()
                    )
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
                "No Learners for ${course.name}",
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