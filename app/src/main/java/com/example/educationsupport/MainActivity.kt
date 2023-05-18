package com.example.educationsupport

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.educationsupport.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_mycoursesFragment, R.id.navigation_exploreFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // hide and show bottom bar for fragments
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_mycoursesFragment
                || destination.id == R.id.navigation_exploreFragment
            ) {
                navView.visibility = View.VISIBLE
            } else {
                navView.visibility = View.GONE
            }

            //used to hide top bar on login screen
            when (destination.id) {
                R.id.navigation_loginFragment -> {
                    supportActionBar?.hide()
                }

                R.id.navigation_mycoursesFragment, R.id.navigation_exploreFragment -> {
                    supportActionBar?.setDisplayHomeAsUpEnabled(false) //disable back on top level fragments
                    supportActionBar?.show()
                }

                else -> {
                    supportActionBar?.show()
                }
            }
        }

        navView.setupWithNavController(navController)
    }

    // Inflate the menu; this adds items to the action bar if it is present.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }


    // this is boilerplate code to make the back button(navigate up) work in deeper level fragments
    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}