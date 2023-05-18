package com.example.educationsupport.ui.learneractivities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.educationsupport.MainActivity
import com.example.educationsupport.R
import com.example.educationsupport.databinding.FragmentManageQuestionsBinding
import com.example.educationsupport.db.Firebase
import com.example.educationsupport.db.objects.Activity
import com.example.educationsupport.db.objects.Course
import com.example.educationsupport.db.objects.Question
import com.example.educationsupport.ui.theme.EducationSupportComposeTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


class ManageQuestionsFragment : Fragment() {
    private var _binding: FragmentManageQuestionsBinding? = null
    private val binding get() = _binding!!

    private var realtimeFirebase = Firebase()

    private lateinit var activity: Activity
    private lateinit var activityID: String
    private lateinit var course: Course

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageQuestionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialiseData()
        (requireActivity() as MainActivity).supportActionBar?.title = "Manage ${activity.name}"
        getQuestionsDB()
        setupButtons()
    }

    private fun initialiseData() {
        activity = requireArguments().getSerializable("activity") as Activity
        course = requireArguments().getSerializable("course") as Course
        activityID = requireArguments().getString("activityID")!!
    }

    private fun setupButtons() {
        binding.newQuestionButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putSerializable(
                "question", Question(
                    activityID = activityID,
                    questionText = "Question Title",
                    answers = mutableMapOf(
                        "Answer 1" to true,
                        "Answer 2" to false
                    )
                )
            )
            bundle.putSerializable("activity", activity)
            bundle.putString("questionID", null)
            findNavController().navigate(
                R.id.action_navigation_manageQuestionsFragment_to_navigation_editQuestionFragment,
                args = bundle
            )
        }

        binding.showLearnersButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putSerializable("course", course)
            bundle.putString("activityID", activityID)
            findNavController().navigate(
                R.id.action_navigation_manageQuestionsFragment_to_activityEducatorResultsFragment,
                args = bundle
            )
        }
    }

    private fun getQuestionsDB() {
        realtimeFirebase.getQuestionsTable()
            .addListenerForSingleValueEvent(object : ValueEventListener {
                val map: MutableMap<String, Question> = mutableMapOf()
                override fun onDataChange(questionsTable: DataSnapshot) {
                    for (question in questionsTable.children) {
                        val `object`: Question? = question.getValue(Question::class.java)
                        if (`object` != null) {
                            if (`object`.activityID == activityID) {
                                map[question.key!!] = `object`
                            }
                        }
                    }
                    updateComposeView(map)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("Firebase", "Couldn't load the questions from the database")
                }
            })
    }

    /**
     * Updates the courses view
     */
    private fun updateComposeView(questionMap: MutableMap<String, Question>) {
        try {
            binding.editQuestionsComposeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    EducationSupportComposeTheme {
                        Surface(color = MaterialTheme.colors.background) {
                            QuestionsList(questionMap = questionMap)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    @Composable
    private fun QuestionsList(questionMap: MutableMap<String, Question>) {
        if (questionMap.isEmpty()) {
            EmptyList()
        } else {
            LazyColumn {
                item {
                    questionMap.onEachIndexed { index, entry ->
                        QuestionItem(
                            questionNumber = index + 1,
                            questionId = entry.key,
                            question = entry.value
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyList() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No questions found",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
    }


    @Composable
    private fun QuestionItem(
        questionNumber: Number,
        questionId: String,
        question: Question
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clickable {
                        val bundle = Bundle()
                        bundle.putSerializable("question", question)
                        bundle.putString("questionID", questionId)
                        findNavController().navigate(
                            R.id.action_navigation_manageQuestionsFragment_to_navigation_editQuestionFragment,
                            args = bundle
                        )
                    }
            ) {
                Column {
                    Text(
                        text = "Question $questionNumber",
                        style = MaterialTheme.typography.subtitle1
                    )
                    Text(text = question.questionText, style = MaterialTheme.typography.subtitle2)
                }
                Column {
                    Row {
                        Text(
                            text = "Edit >"
                        )
                    }

                }
            }


        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}