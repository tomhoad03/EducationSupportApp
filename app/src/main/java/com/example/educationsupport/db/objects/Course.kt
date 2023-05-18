package com.example.educationsupport.db.objects

import kotlinx.serialization.Serializable

/**
 * This class defines a Course.
 *
 * @param name                  The name of the course, as a String
 * @param description           The description of the course, as a String
 * @param educatorId            The id of the educator, as a String
 * @param activityIds           The list of activities as a Map of (activityId, True)
 * @param learnerIds            The list of learners as a Map of (learnerId, True)
 * @param learnersFinished      The number of finished learners
 *
 */


@Serializable
class Course(
    var name: String = "",
    var description: String = "",
    var educatorId: String = "",
    var activityIds: MutableMap<String, Boolean> = mutableMapOf(),
    var learnerIds: MutableMap<String, Boolean> = mutableMapOf(),
    var learnersFinished: Int = 0,
) : java.io.Serializable