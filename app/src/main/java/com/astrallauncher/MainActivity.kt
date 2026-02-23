package com.astrallauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.astrallauncher.ui.screens.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val vm: MainViewModel = viewModel()
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF080808)
            ) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(vm = vm, navController = navController)
                    }
                }
            }
        }
    }
}
