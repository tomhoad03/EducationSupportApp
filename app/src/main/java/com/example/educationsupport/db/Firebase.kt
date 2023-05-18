package com.example.educationsupport.db

import android.net.Uri
import android.util.Log
import com.example.educationsupport.db.objects.Activity
import com.example.educationsupport.db.objects.Course
import com.example.educationsupport.db.objects.Question
import com.example.educationsupport.db.objects.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage

/**
 * This class contains all the functions (or template functions) useful to interact with the Google Firebase
 * Realtime Database and Storage
 */
class Firebase() {
    // determine url of realtime firebase DB
    private val url = "https://educationsupportapp-28209-default-rtdb.europe-west1.firebasedatabase.app/"

    // Setting Google Firebase and Storage references
    private val realtimeDatabase = Firebase.database(url)
    private val googleStorage = Firebase.storage

    // Database tables references
    private val usersTable = realtimeDatabase.getReference("users")
    private val coursesTable = realtimeDatabase.getReference("courses")
    private val activitiesTable = realtimeDatabase.getReference("activities")
    private val questionsTable = realtimeDatabase.getReference("questions")
    private val resultsTable = realtimeDatabase.getReference("results")

    // Pictures folders
    private val profilePicturesFolder = googleStorage.getReference("profile_pictures")
    private val activitiesPicturesFolder = googleStorage.getReference("activities_pictures") // will look at later for advanced extension if time


    // Bellow are functions with examples on adding, editing and requesting elements from the database
    // For functions requiring data to be returned from the database, those should be implemented in the code as the listener functions cant be accessed here

    /**
     * Given a user, this function adds it to the users table on the Firebase Realtime Database
     *
     * @param user                  the user object containing all data about the user
     */
    private fun addUser(user: User, userID: String? = usersTable.push().key) {

        // Adding new user to the database
        if (userID != null) {
            usersTable.child(userID).setValue(user)
                .addOnSuccessListener {
                    Log.d("Firebase Realtime Database", "User $userID was added!")
                }.addOnFailureListener {
                    Log.e("Firebase Realtime Database", "Unable to add user $userID!", it)
                }
        } else {
            Log.e("Firebase Realtime Database", "Unable to generate a new userID!")
        }
    }

    /**
     * Given a user and a picture, this function adds it to the users table on the Firebase Realtime
     * Database, uploading the picture to Google Storage
     *
     * @param user                  the user object containing all data about the user
     * @param userPictureUri        the local uri of the picture to upload
     */
    fun addUser(user: User, userPictureUri: Uri) {
        // Generating a new userID
        val userID = usersTable.push().key
        if (userID != null) {

            // If user picture uri is provided upload picture to Storage and add user to DB
            if (userPictureUri != Uri.EMPTY) {

                // Specifying upload location on google Storage
                val pictureRef = profilePicturesFolder.child("${userPictureUri.lastPathSegment}")

                // Uploading picture to google Storage
                pictureRef.putFile(userPictureUri).addOnSuccessListener {
                    pictureRef.downloadUrl.addOnSuccessListener { it1 ->
                        // Adding pictureURL to user's data
                        user.pictureURL = it1.toString()

                        // Adding new user to the database
                        usersTable.child(userID).setValue(user)
                            .addOnSuccessListener {
                                Log.d("Firebase Realtime Database", "User $userID was added!")
                            }.addOnFailureListener {
                                Log.e(
                                    "Firebase Realtime Database",
                                    "Unable to add user $userID!",
                                    it
                                )
                            }
                    }
                }

                // If user picture uri is empty just add user to Firebase Realtime DB
            } else {
                addUser(user)
            }
        } else {
            Log.e("Firebase Realtime Database", "Unable to generate a new userID!")
        }
    }

    /**
     * Given a course, this function adds it to the course table on the Firebase Realtime Database
     *
     * @param course                  the course object containing all data about the course
     */
    fun addCourse(course: Course, userID: String, user: User): String? {
        // Generating a new courseId
        val courseID = coursesTable.push().key
        if (courseID != null) {
            // Adding new course to the database
            coursesTable.child(courseID).setValue(course)
                .addOnSuccessListener {
                    Log.d(
                        "Firebase Realtime Database",
                        "Course $courseID was added!"
                    )
                }.addOnFailureListener {
                    Log.e(
                        "Firebase Realtime Database",
                        "Unable to add course $courseID!", it
                    )
                }

            user.courseIDs[courseID] = true
            editUser(userID, user)
            return courseID
        } else {
            Log.e("Firebase Realtime Database", "Unable to generate a new courseID!")
            return null
        }
    }

    /**
     * Given an activity, this function adds it to the activity table on the Firebase Realtime Database
     *
     * @param activity                  the activity object containing all data about the activity
     */
    fun addActivity(activity: Activity, courseId: String, course: Course) {
        // Generating a new activityID
        val activityID = activitiesTable.push().key
        if (activityID != null) {

            // Adding new activity to the database
            activitiesTable.child(activityID).setValue(activity)
                .addOnSuccessListener {
                    Log.d(
                        "Firebase Realtime Database",
                        "Activity $activityID was added!"
                    )
                }.addOnFailureListener {
                    Log.e(
                        "Firebase Realtime Database",
                        "Unable to add activity $activityID!", it
                    )
                }

            course.activityIds[activityID] = true
            editCourse(courseId, course)
        } else {
            Log.e("Firebase Realtime Database", "Unable to generate a new activityID!")
        }
    }

    /**
     * Given the activityID of an activity to be edited and an activity object representing the
     * updated version of that activity, this function substitutes the old activity with the new one
     *
     * @param activityID        the activityID of the activity to be edited
     * @param editedActivity    the new activity object representing the updated activity
     */
    private fun editActivity(activityID: String, editedActivity: Activity) {
        activitiesTable.child(activityID).setValue(editedActivity)
            .addOnSuccessListener {
                Log.d(
                    "Firebase Realtime Database",
                    "Activity $activityID was edited!"
                )
            }.addOnFailureListener {
                Log.e(
                    "Firebase Realtime Database",
                    "Unable to edit activity $activityID!", it
                )
            }
    }

    /**
     * Adds a new question to an existing activity.
     *
     * @param activity        the activity object to add the question to
     * @param activityID    the id of the activity to add the question to
     * @param question    the question object to be added to activity
     */
    fun addQuestionTo(activity: Activity, activityID: String, question: Question): String? {
        // Generating a new questionID
        val questionID = questionsTable.push().key
        if (questionID != null) {

            // Adding new question to the database
            questionsTable.child(questionID).setValue(question)
                .addOnSuccessListener {
                    Log.d(
                        "Firebase Realtime Database",
                        "Question $questionID was added!"
                    )
                }.addOnFailureListener {
                    Log.e(
                        "Firebase Realtime Database",
                        "Unable to add question $questionID!", it
                    )
                }

            // linking this question to activity
            activity.questionIDs[questionID] = true
            editActivity(activityID, activity)
            return questionID
        } else {
            Log.e("Firebase Realtime Database", "Unable to generate a new questionID!")
            return null
        }
    }

    fun updateQuestion(questionId: String, question: Question) {
        questionsTable.child(questionId).setValue(question).addOnSuccessListener {
            Log.d(
                "Firebase Realtime Database",
                "Question $questionId was updated!"
            )
        }.addOnFailureListener{
            Log.e(
                "Firebase Realtime Database",
                "Unable to update question $questionId!", it
            )
        }
    }

    fun recordActivityResult(result: com.example.educationsupport.db.objects.Result): String? {
        // Generating a new resultID
        val resultID = resultsTable.push().key
        if (resultID != null) {

            // Adding new result to the database
            resultsTable.child(resultID).setValue(result)
                .addOnSuccessListener {
                    activitiesTable.child(result.activityID).child("resultIDs").child(resultID).setValue(true)
                    Log.d(
                        "Firebase Realtime Database",
                        "Result $resultID was added!"
                    )
                }.addOnFailureListener {
                    Log.e(
                        "Firebase Realtime Database",
                        "Unable to add result $resultID!", it
                    )
                }
            return resultID
        } else {
            Log.e("Firebase Realtime Database", "Unable to generate a new resultID!")
            return null
        }
    }

    /**
     * Gets questions of a specific activity as a map of question IDs and question objects
     *
     * @param activityID    the id of the activity
     */
    fun getQuestionsOfActivity(activityID: String): MutableMap<String, Question>? {
        var result: MutableMap<String, Question>? = null
        questionsTable.get().addOnCompleteListener {
            if (it.isSuccessful) {
                result = it.result.value as MutableMap<String, Question>
//                result!!.filter { (key, value) ->
//                    value.activityID == activityID
//                }
                Log.d("Firebase", result.toString())
            } else {
                Log.w("Firebase", "Could not load questions")
            }
        }
        Log.d("result:", result.toString())
        return result
    }


    /**
     * Given the userID of a user to be edited and a user object representing the updated version of
     * that user, this function substitutes the old user with the new one
     *
     * @param userID        the userID of the activity to be edited
     * @param editedUser    the new user object representing the updated user
     */
    private fun editUser(userID: String, editedUser: User) {
        usersTable.child(userID).setValue(editedUser)
            .addOnSuccessListener {
                Log.d(
                    "Firebase Realtime Database",
                    "User $userID was edited!"
                )
            }.addOnFailureListener {
                Log.e(
                    "Firebase Realtime Database",
                    "Unable to edit user $userID!", it
                )
            }
    }

    /**
     * Given the courseID of a course to be edited and a course object representing the updated version of
     * that course, this function substitutes the old course with the new one
     *
     * @param courseID        the courseID of the activity to be edited
     * @param editedCourse    the new course object representing the updated course
     */
    private fun editCourse(courseID: String, editedCourse: Course) {
        coursesTable.child(courseID).setValue(editedCourse)
            .addOnSuccessListener {
                Log.d(
                    "Firebase Realtime Database",
                    "Course $courseID was edited!"
                )
            }.addOnFailureListener {
                Log.e(
                    "Firebase Realtime Database",
                    "Unable to edit course $courseID!", it
                )
            }
    }


    /**
     * Given a userID, this function:
     *
     * 1. Removes the user from the participants list of all their planned activities
     * 2. Removes the user from the users table on the Firebase Realtime Database
     *
     * @param userID      the userID string associated to the user that will be removed
     */
    fun removeUser(userID: String) {
        usersTable.child(userID).child("plannedActivities")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(plannedActivities: DataSnapshot) {

                    // Remove user from the users table
                    usersTable.child(userID).removeValue()
                        .addOnSuccessListener {
                            Log.d("Firebase Realtime Database", "User $userID was removed!")
                        }.addOnFailureListener {
                            Log.e(
                                "Firebase Realtime Database",
                                "Unable to remove user $userID!",
                                it
                            )
                        }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }


    /**
     * This function returns all the users in the database as an array of Users objects
     * (This function is just a template for when we need to access all users)
     */
    fun readAllUsers() {
        val usersMap: MutableMap<String?, User?> = mutableMapOf()

        usersTable.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(users: DataSnapshot) {
                for (user in users.children) {
                    val `object`: User? = user.getValue(User::class.java)
                    if (`object` != null) {
                        usersMap[user.key] = `object`
                    }
                }
                // DO SOMETHING WITH THE FULL MAP OF USERS
                print("Map of Users:")
                println(usersMap)
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    /**
     * This function returns all the courses in the database as an array of Course objects
     * (This function is just a template for when we need to access all courses)
     */
    fun readAllCourses() {
        val coursesMap: MutableMap<String?, Course?> = mutableMapOf()

        coursesTable.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(courses: DataSnapshot) {
                for (course in courses.children) {
                    val `object`: Course? = course.getValue(Course::class.java)
                    if (`object` != null) {
                        coursesMap[course.key] = `object`
                    }
                }
                // DO SOMETHING WITH THE FULL MAP OF ACTIVITIES
                print("Map of Courses:")
                println(coursesMap)
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    /**
     * This function returns all the activities in the database as an array of Activity objects
     * (This function is just a template for when we need to access all activities)
     */
    fun readAllActivities() {
        val activitiesMap: MutableMap<String?, Activity?> = mutableMapOf()

        activitiesTable.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(activities: DataSnapshot) {
                for (activity in activities.children) {
                    val `object`: Activity? = activity.getValue(Activity::class.java)
                    if (`object` != null) {
                        activitiesMap[activity.key] = `object`
                    }
                }
                // DO SOMETHING WITH THE FULL MAP OF ACTIVITIES
                print("Map of Activities:")
                println(activitiesMap)
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    //Getter functions bellow

    /**
     * Getter method for the usersTable in the Firebase Realtime Database
     */
    fun getUsersTable(): DatabaseReference {
        return usersTable
    }

    /**
     * Getter method for the activitiesTable in the Firebase Realtime Database
     */
    fun getActivitiesTable(): DatabaseReference {
        return activitiesTable
    }

    fun getCoursesTable(): DatabaseReference {
        return coursesTable
    }

    fun getQuestionsTable(): DatabaseReference {
        return questionsTable
    }

    fun getResultsTable(): DatabaseReference {
        return resultsTable
    }

    /**
     * Getter method for the profilePicturesFolder in the Firebase Storage
     */
    fun getProfilePicturesFolder(): StorageReference {
        return profilePicturesFolder
    }

    /**
     * Getter method for the activitiesPicturesFolder in the Firebase Storage
     */
    fun getActivitiesPicturesFolder(): StorageReference {
        return activitiesPicturesFolder
    }

    /**
     * Getter method for Firebase Storage reference
     */
    fun getFirebaseStorage(): FirebaseStorage {
        return googleStorage
    }
}