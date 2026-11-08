package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.alarm.DoseScheduler
import com.example.data.db.DoseLockDatabase
import com.example.data.repository.MedicationRepository
import com.example.ui.DoseLockMainScreen
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.DoseLockTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = DoseLockDatabase.getDatabase(applicationContext)
        val repository = MedicationRepository(db.medicationDao())
        val scheduler = DoseScheduler(applicationContext)

        val viewModelFactory = MainViewModelFactory(repository, scheduler)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        setContent {
            DoseLockTheme {
                DoseLockMainScreen(viewModel = viewModel)
            }
        }
    }
}
