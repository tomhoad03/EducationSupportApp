package com.example.educationsupport.db.objects

import com.example.educationsupport.models.QuestionResult
import kotlinx.serialization.Serializable

/**
 * This class defines a Question.
 *
 * @param activityID                 The String activity's ID
 * @param questionText               The String text for the question
 * @param answers                  The list of answers as a Map of (Answer Text String, True/False)
 *
 */

@Serializable
class Question(
    var activityID: String = "",
    var questionText: String = "",
    var answers: MutableMap<String, Boolean> = mutableMapOf(),
) : java.io.Serializable

public fun checkAnswer(question: Question, selectedAnswer: String): Boolean {
    return question.answers[selectedAnswer]!!
}

private fun checkIndividualUnselectedAnswer(question: Question, unselectedAnswer: String): Boolean {
    return !question.answers[unselectedAnswer]!!
}

public fun getIncorrectSelected(
    question: Question,
    selectedAnswers: ArrayList<String>
): ArrayList<String> {
    val result = arrayListOf<String>()
    for (answer in selectedAnswers) {
        if (!checkAnswer(question, answer)) {
            result.add(answer)
        }
    }
    return result
}

public fun getMissedAnswers(
    question: Question,
    selectedAnswers: ArrayList<String>
): ArrayList<String> {
    val result = arrayListOf<String>()
    for (answer in question.answers.keys) {
        if (question.answers[answer] == true) {
            result.add(answer)
        }
    }
    for (answer in selectedAnswers) {
        result.remove(answer)
    }
    return result
}

public fun getQuestionResult(
    question: Question,
    selectedAnswers: ArrayList<String>
): QuestionResult {
    return QuestionResult(
        question,
        getIncorrectSelected(question, selectedAnswers),
        getMissedAnswers(question, selectedAnswers)
    )
}

public fun getDetailedActivityResults(userAnswers: HashMap<Question, ArrayList<String>>): ArrayList<QuestionResult> {
    val results = arrayListOf<QuestionResult>()
    userAnswers.forEach { (question, answers) ->
        results.add(getQuestionResult(question, answers))
    }
    return results
}