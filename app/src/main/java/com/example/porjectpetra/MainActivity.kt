package com.example.porjectpetra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.porjectpetra.ui.navigation.AppNavigation
import com.example.porjectpetra.ui.program.ProgramViewModel
import com.example.porjectpetra.ui.theme.PetraTheme

class MainActivity : ComponentActivity() {

    private val app by lazy { application as FitnessApplication }

    private val programViewModel: ProgramViewModel by viewModels {
        ProgramViewModel.provideFactory(app.programRepository, app.exerciseCatalog)
    }

    override fun onResume() {
        super.onResume()
        // Le jour a pu changer pendant que l'application était en arrière-plan.
        app.todayProvider.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PetraTheme {
                AppNavigation(
                    app = app,
                    programViewModel = programViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
