package com.example.educationsupport.ui.learneractivities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.educationsupport.MainActivity
import com.example.educationsupport.R
import com.example.educationsupport.databinding.FragmentActivityBinding
import com.example.educationsupport.db.Firebase
import com.example.educationsupport.db.objects.Activity
import com.example.educationsupport.db.objects.Question
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ActivityFragment : Fragment() {
    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!

    private var realtimeFirebase = Firebase()

    private var activity = Activity()
    private var activityID: String? = null
    private var questions = ArrayList<Question>()

    // key is id of the question, value is the question object
    private var questionMap: MutableMap<String?, Question?> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        activity = requireArguments().getSerializable("activity") as Activity
        activityID = requireArguments().getString("activityID")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).supportActionBar?.title = activity.name
        setUpInitialActivityInfo()
        fetchQuestions()
        setUpStartActivityButton()
        setUpReturnToCourseFragmentButton()
    }

    private fun setUpStartActivityButton() {
        if (activity.questionIDs.isEmpty()) {
            binding.startActivityButton.isEnabled = false
        }
        binding.startActivityButton.setOnClickListener {
            showQuestion()
        }
    }

    private fun setUpReturnToCourseFragmentButton() {
        binding.returnToCorseFragmentButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun showQuestion() {
        val bundle = Bundle()
        bundle.putSerializable("questions", questions)
        bundle.putString("activityID", activityID)
        findNavController().navigate(
            R.id.action_navigation_activityFragment_to_navigation_questionView, args = bundle
        )
    }

    private fun setUpInitialActivityInfo() {
        binding.activityDescriptionText.text =
            activity.description + "\n" + "\n" + "\n" +
            "This activity has ${questions.size} multiple choice questions"
        binding.activityTitleTextView.text = activity.name
    }

    private fun fetchQuestions() {
        questionMap= mutableMapOf()
        questions.clear()

        realtimeFirebase.getQuestionsTable()
            .addListenerForSingleValueEvent(object : ValueEventListener {
                val map: MutableMap<String?, Question?> = mutableMapOf()
                override fun onDataChange(questionsTable: DataSnapshot) {
                    for (question in questionsTable.children) {
                        val `object`: Question? = question.getValue(Question::class.java)
                        if (`object` != null) {
                            if (`object`.activityID == activityID) {
                                map[question.key] = `object`
                                questions.add(`object`)
                            }
                        }
                    }
                    questionMap = map
                    setUpInitialActivityInfo()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("Firebase", "Couldn't load the questions from the database")
                }
            })
    }

}