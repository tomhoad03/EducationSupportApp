package com.example.educationsupport.db.objects

import kotlinx.serialization.Serializable

/**
 * This class defines an Activity.
 *
 * @param courseID              The String ID of the of the course of the Activity
 * @param name                  The name of the activity, as a String
 * @param description           The description of the activity, as a String
 * @param questionIDs           The list of questions as a Map of (questionID, True)
 * @param resultIDs             The list of results as a Map of (resultID, True)
 *
 */

@Serializable
class Activity(
    var courseID: String = "",
    var name: String = "",
    var description: String = "",
    var questionIDs: MutableMap<String, Boolean> = mutableMapOf(),
    var resultIDs: MutableMap<String, Boolean> = mutableMapOf(),
) : java.io.Serializable