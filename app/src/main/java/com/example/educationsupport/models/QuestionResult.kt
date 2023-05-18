package com.example.educationsupport.models

import com.example.educationsupport.db.objects.Question

data class QuestionResult(val question: Question, val incorrectAnswers: ArrayList<String>, val missedAnswers: ArrayList<String>) {
    public fun isCorrect(): Boolean {
        return incorrectAnswers.isEmpty() && missedAnswers.isEmpty()
    }
}
