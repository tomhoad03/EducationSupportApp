package com.example.educationsupport.ui.learneractivities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.educationsupport.databinding.FragmentEditQuestionBinding
import com.example.educationsupport.db.Firebase
import com.example.educationsupport.db.objects.Activity
import com.example.educationsupport.db.objects.Question
import com.example.educationsupport.ui.theme.EducationSupportComposeTheme

class EditQuestionFragment : Fragment() {
    private var _binding: FragmentEditQuestionBinding? = null
    private val binding get() = _binding!!

    private var realtimeFirebase = Firebase()

    private lateinit var question: Question
    private var questionId: String? = null


    private var answerOptionsValues: MutableMap<Int, Pair<String, Boolean>?> = mutableMapOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        question = requireArguments().getSerializable("question") as Question
        questionId = requireArguments().getString("questionID")

        _binding = FragmentEditQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setQuestionTitle()
        question.answers.onEachIndexed { index, entry ->
            answerOptionsValues[index] = Pair(entry.key, entry.value)
        }
        setupButtons()
        updateComposeView()
    }

    private fun setQuestionTitle() {
        binding.questionInput.setText(question.questionText)
    }

    private fun setupButtons() {
        //set delete visible if id present
        if (questionId != null) {
            binding.buttonDelete.visibility = View.VISIBLE
        }

        binding.buttonDeleteOption.setOnClickListener {
            if (answerOptionsValues.isNotEmpty()) {
                answerOptionsValues.remove(answerOptionsValues.keys.max())
                updateComposeView()
            }
        }

        binding.buttonAddOption.setOnClickListener {
            if (answerOptionsValues.isEmpty()) {
                answerOptionsValues[0] = Pair("New", true)
            } else {
                answerOptionsValues[answerOptionsValues.keys.max() + 1] = Pair("New", true)
            }
            updateComposeView()
        }

        binding.buttonSave.setOnClickListener {
            question.answers.clear()
            question.questionText =
                binding.questionInput.text.toString()//sets the questionText to box upon saving

            answerOptionsValues.forEach { (_, answer) ->
                if (answer != null) {
                    question.answers[answer.first] = answer.second
                }
            }
            if (questionId != null) {
                realtimeFirebase.updateQuestion(questionId!!, question)
            } else {
                realtimeFirebase.addQuestionTo(
                    activity = requireArguments().getSerializable("activity") as Activity,
                    activityID = question.activityID,
                    question = question
                )
            }
            findNavController().navigateUp()
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.buttonDelete.setOnClickListener {
            // remove question itself
            realtimeFirebase.getQuestionsTable().child(questionId!!).removeValue()
            // remove question id from activity
            realtimeFirebase.getActivitiesTable().child(question.activityID).child("questionIDs").get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val result = it.result.value as MutableMap<String, Boolean>
                    result.remove(questionId)
                    realtimeFirebase.getActivitiesTable().child(question.activityID).child("questionIDs").setValue(result)
                    findNavController().navigateUp()
                } else {
                    findNavController().navigateUp()
                }
            }
        }

    }

    private fun updateComposeView() {
        try {
            binding.answersComposeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    EducationSupportComposeTheme {
                        Surface(
                            color = MaterialTheme.colors.background,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            AnswerOptionsList()
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    @Composable
    private fun AnswerOptionsList() {
        if (answerOptionsValues.isEmpty()) {
            EmptyList()
        } else {
            Column {
                answerOptionsValues.onEach { entry ->
                    if (entry.value != null) {
                        AnswerOption(
                            mapID = entry.key
                        )
                    }

                }
            }
        }
    }

    @Composable
    private fun AnswerOption(mapID: Int) {
        Row(modifier = Modifier.padding(5.dp)) {
            val checkboxState = remember {
                mutableStateOf(answerOptionsValues[mapID]!!.second)
            }
            val textFieldState = remember {
                mutableStateOf(answerOptionsValues[mapID]!!.first)
            }

            TextField(
                modifier = Modifier.weight(1f),
                value = textFieldState.value,
                onValueChange = {
                    textFieldState.value = it
                    answerOptionsValues[mapID] = Pair(it, checkboxState.value)
                })
            Checkbox(
                checked = checkboxState.value, onCheckedChange = {
                    checkboxState.value = it
                    answerOptionsValues[mapID] = Pair(textFieldState.value, it)
                }, colors = CheckboxDefaults.colors(
                    checkedColor = Color(43, 124, 20, 255),
                    uncheckedColor = Color.Red
                )
            )
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
                "No Answers Set",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
    }

}