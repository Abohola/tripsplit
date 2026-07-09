package com.tripsplit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tripsplit.data.TripStore
import com.tripsplit.ui.TripSplitApp
import com.tripsplit.ui.TripSplitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = TripStore(applicationContext)
        enableEdgeToEdge()
        setContent {
            TripSplitTheme {
                TripSplitApp(store = store)
            }
        }
    }
}
