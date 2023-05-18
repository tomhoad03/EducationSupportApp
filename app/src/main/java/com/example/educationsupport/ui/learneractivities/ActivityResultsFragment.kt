package com.example.educationsupport.ui.learneractivities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.educationsupport.databinding.FragmentActivityResultsBinding
import com.example.educationsupport.db.Firebase
import com.example.educationsupport.db.objects.Question
import com.example.educationsupport.db.objects.Result
import com.example.educationsupport.models.QuestionResult
import com.example.educationsupport.ui.theme.EducationSupportComposeTheme
import com.example.educationsupport.ui.theme.Shapes
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.round

class ActivityResultsFragment : Fragment() {
    private var _binding: FragmentActivityResultsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var activityResults: ArrayList<QuestionResult> = arrayListOf()
    private var correctResults: ArrayList<QuestionResult> = arrayListOf()

    private lateinit var result: Result

    private var auth = FirebaseAuth.getInstance()
    private var realtimeFirebase = Firebase()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activityResults = requireArguments().getSerializable("results") as ArrayList<QuestionResult>
        correctResults = activityResults.filter { it.isCorrect() } as ArrayList<QuestionResult>
        // register user's result in the database
        result = Result(
            activityID = requireArguments().getString("activityID")!!,
            userID = auth.uid!!,
            numberOfCorrectQuestions = correctResults.size
        )
        realtimeFirebase.recordActivityResult(result = result)
        _binding = FragmentActivityResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // this function is automatically called after onCreateView
    // here execute all bindings for buttons or any backend logic
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateComposeView()
    }


    /**
     * Updates the courses view
     */
    private fun updateComposeView() {
        try {
            binding.resultsComposeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    EducationSupportComposeTheme {
                        Surface(color = MaterialTheme.colors.background) {
                            Column {
                                ResultSummary()
                                Button(onClick = {
                                    findNavController().navigateUp()
                                }, modifier = Modifier
                                    .height(60.dp)
                                    .width(260.dp)
                                    .align(Alignment.CenterHorizontally)) {
                                    Text(text = "Close Results",style = MaterialTheme.typography.h6)
                                }
                                Text(text = "Detailed results", modifier = Modifier.padding(10.dp))
                                ResultsList()


                            }


                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }


    @Composable
    private fun ResultSummary() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        )
        {
            Column {
                Text(
                    text = "You've answered ${correctResults.size} question(s) correctly out of ${activityResults.size}. That's a score of:",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Card(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "${round((correctResults.size.toDouble() / activityResults.size) * 100)}%"
                    )
                }

            }

        }

    }

    @Composable
    private fun ResultsList() {
        LazyColumn {
            item {
                activityResults.onEachIndexed { index, entry ->
                    ResultItem(
                        questionNumber = index + 1,
                        question = entry.question,
                        correct = entry.isCorrect()
                    )
                }
            }

        }
    }

    @Composable
    private fun ResultItem(
        questionNumber: Int,
        question: Question,
        correct: Boolean
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Column {
                        Text(
                            text = "Question $questionNumber",
                            style = MaterialTheme.typography.subtitle1
                        )
                        Text(
                            text = question.questionText,
                            style = MaterialTheme.typography.subtitle2
                        )
                    }
                    Column {
                        Text(
                            text = if (correct) {
                                "Correct"
                            } else {
                                "Wrong"
                            },
                            color = if (correct) {
                                Color(45, 150, 73, 255)
                            } else {
                                Color.Red
                            }
                        )
                    }
                }
                Row {
                    if (!correct) {
                        DetailedResultInfo(indexOfQuestion = questionNumber - 1)
                    } else {
                        Column {
                            activityResults[questionNumber - 1].question.answers.forEach { (answer, correct) ->
                                if (correct) {
                                    Row {
                                        Text(text = "✅ $answer", style = MaterialTheme.typography.subtitle1)
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }


    }


    @Composable
    private fun DetailedResultInfo(indexOfQuestion: Int) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            elevation = 4.dp,
            shape = Shapes.medium
        ) {
            Column {
                if (activityResults[indexOfQuestion].incorrectAnswers.isNotEmpty()) {
                    Text(
                        text = "You chose these incorrect answers:",
                        style = MaterialTheme.typography.subtitle1
                    )
                    activityResults[indexOfQuestion].incorrectAnswers.forEach {
                        Text(text = "❌ $it")
                    }
                }
                if (activityResults[indexOfQuestion].missedAnswers.isNotEmpty()) {
                    Text(
                        text = "You missed these correct answers:",
                        style = MaterialTheme.typography.subtitle1
                    )
                    activityResults[indexOfQuestion].missedAnswers.forEach {
                        Text(text = "✅ $it", color = Color.Red)
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