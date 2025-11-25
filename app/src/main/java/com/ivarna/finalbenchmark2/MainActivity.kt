package com.ivarna.finalbenchmark2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ivarna.finalbenchmark2.navigation.BenchmarkNavigation
import com.ivarna.finalbenchmark2.ui.theme.FinalBenchmark2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinalBenchmark2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BenchmarkNavigation(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}