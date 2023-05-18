package com.example.educationsupport.db.objects

import kotlinx.serialization.Serializable

/**
 * This class defines a Result.
 *
 * @param activityID         The String ID of the of the Activity linked to the result
 * @param userID             The String ID of the User
 * @param numberOfCorrectQuestions         Integer of how many questions were correct
 *
 */

@Serializable
class Result(
    var activityID: String = "",
    var userID: String = "",
    var numberOfCorrectQuestions: Int = 0
) : java.io.Serializable