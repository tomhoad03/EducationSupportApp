package com.example.educationsupport.db.objects

import kotlinx.serialization.Serializable

/**
 * This class defines a User.
 *
 * @param email                 The email address of the user
 * @param name                  The String display name of the profile
 * @param bio                   The String bio information of the profile
 * @param pictureURL            The profile pic String link
 * @param courseIDs             The list of courses as a Map of (courseID, True)
 * @param educator            The Boolean notes if a user is an Educator
 *
 */

@Serializable
class User(
    var email: String = "",
    var name: String = "",
    var bio: String = "",
    var pictureURL: String = "",
    var courseIDs: MutableMap<String, Boolean> = mutableMapOf(),
    var educator: Boolean = false,
) : java.io.Serializable