package com.example.educationsupport.ui.learneractivities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.educationsupport.R
import com.example.educationsupport.databinding.FragmentQuestionViewBinding
import com.example.educationsupport.db.objects.Question
import com.example.educationsupport.db.objects.User
import com.example.educationsupport.db.objects.getDetailedActivityResults
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class QuestionViewFragment : Fragment() {
    // this fragment will display all questions to the user

    private var _binding: FragmentQuestionViewBinding? = null
    private val binding get() = _binding!!

    private var questions: ArrayList<Question> = arrayListOf()
    private var currentQuestionNumber: Int = -1
    private var currentSelectedAnswers = mutableListOf<String>()
    private var userAnswers: HashMap<Question, ArrayList<String>> = hashMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentQuestionViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchQuestions()
        nextQuestion()

        binding.buttonConfirmAnswer.setOnClickListener {
            handleConfirmAnswer()
        }

        binding.buttonSubmit.setOnClickListener {
            handleSubmit()
        }

        binding.buttonNext.setOnClickListener {
            if (currentQuestionNumber + 2 >= questions.size) {
                binding.buttonNext.isEnabled = false
                binding.buttonBack.isEnabled = true
                nextQuestion()
            } else{
                binding.buttonBack.isEnabled = true
                nextQuestion()
            }
        }

        binding.buttonBack.setOnClickListener {
            if(currentQuestionNumber<=1){
                binding.buttonBack.isEnabled = false
                binding.buttonNext.isEnabled = true
                previousQuestion()
            } else{
                previousQuestion()
                binding.buttonNext.isEnabled = true
            }
        }


        if(currentQuestionNumber + 1 >= questions.size){
            binding.buttonNext.isEnabled = false
        }
    }

    private fun handleConfirmAnswer() {
        val currentQuestion = questions[currentQuestionNumber]
        userAnswers[currentQuestion] = ArrayList(currentSelectedAnswers)

        if (currentQuestionNumber + 1 < questions.size) {
            binding.buttonNext.callOnClick()
        } else {
            binding.buttonConfirmAnswer.isEnabled=false
        }
    }

    private fun handleSubmit(){
        if(userAnswers.size == questions.size){
            confirmSubmitDialog()
        } else{
            Toast.makeText(requireContext(),"You need to answer all the Questions!",Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResults() {
        //fix ordering of questions for next fragment
        val orderedAnswers : HashMap<Question, ArrayList<String>> = hashMapOf()
        questions.forEach {
            orderedAnswers[it] = userAnswers[it]!!
        }
        val bundle = Bundle()
        bundle.putString("activityID", requireArguments().getString("activityID"))
        bundle.putSerializable("results", getDetailedActivityResults(orderedAnswers))
        findNavController().navigate(
            R.id.action_navigation_questionView_to_navigation_activityResultsFragment,
            args = bundle
        )
    }

    private fun setQuestion(question: Question, currentSelectedAnswers: MutableList<String>) {
        setQuestionText(question.questionText)
        // add the new answers
        for (answer: String in question.answers.keys) {
            addAnswerOption(answer,currentSelectedAnswers.contains(answer))
        }
    }

    private fun nextQuestion() {
        // remove old answers from previous question
        binding.answerOptionsLayout.removeAllViewsInLayout()
        binding.buttonConfirmAnswer.isEnabled = false
        currentQuestionNumber += 1
        currentSelectedAnswers = if(userAnswers[questions[currentQuestionNumber]].isNullOrEmpty()){
            mutableListOf()
        } else {
            userAnswers[questions[currentQuestionNumber]]!!
        }
        setQuestion(questions[currentQuestionNumber],currentSelectedAnswers)
    }

    private fun previousQuestion() {
        // remove old answers from previous question
        binding.answerOptionsLayout.removeAllViewsInLayout()
        binding.buttonConfirmAnswer.isEnabled = false
        currentQuestionNumber -= 1
        currentSelectedAnswers = if(userAnswers[questions[currentQuestionNumber]].isNullOrEmpty()){
            mutableListOf()
        } else {
            userAnswers[questions[currentQuestionNumber]]!!
        }
        setQuestion(questions[currentQuestionNumber], currentSelectedAnswers)
    }

    private fun fetchQuestions() {
        this.questions = requireArguments().getSerializable("questions")!! as ArrayList<Question>


    }

    private fun setQuestionText(text: String) {
        binding.questionTextLearner.text = text
    }

    private fun handleSelectAnswer(answer: String) {
        currentSelectedAnswers.add(answer)
    }

    private fun handleDeSelectAnswer(answer: String) {
        currentSelectedAnswers.remove(answer)
    }

    private fun setAnswerOptionAppearance(answerOption: CheckBox): CheckBox {
        answerOption.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        val marginParams = answerOption.layoutParams as ViewGroup.MarginLayoutParams
        marginParams.setMargins(20, 5, 5, 5)
        answerOption.layoutParams = marginParams
        answerOption.textSize = 25F
        return answerOption
    }

    private fun confirmSubmitDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Submit this activity?")
            .setNegativeButton("Cancel") { _, _ -> }
            .setPositiveButton("Submit") { _, _ ->
                showResults()
            }
            .show()
    }

    private fun addAnswerOption(text: String, checked: Boolean) {
        val answerButtonOption = CheckBox(requireContext())
        setAnswerOptionAppearance(answerButtonOption)
        answerButtonOption.text = text
        answerButtonOption.isChecked = checked
        answerButtonOption.setOnClickListener {
            val checkbox = it as CheckBox
            if (checkbox.isChecked) {
                handleSelectAnswer(checkbox.text.toString())
            } else {
                handleDeSelectAnswer(checkbox.text.toString())
            }
            Log.w(this.tag, this.currentSelectedAnswers.toString())
            binding.buttonConfirmAnswer.isEnabled = this.currentSelectedAnswers.isNotEmpty()
        }
        binding.answerOptionsLayout.addView(answerButtonOption)
    }
}