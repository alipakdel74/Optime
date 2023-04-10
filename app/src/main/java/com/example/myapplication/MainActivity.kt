package com.example.myapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting()
                }
            }
        }
    }
}

@Composable
fun Greeting() {
    val context = LocalContext.current
    val handler = Handler(Looper.getMainLooper())

    var myLocation by remember { mutableStateOf("") }
    var latLong by remember { mutableStateOf("") }

    if (myLocation.isEmpty())
        if (checkGPSIsOn(context))
            CurrentLocation(context) { latitude, longitude, locality, countryName ->
                myLocation = "$locality / $countryName"
                latLong = "$latitude - $longitude"
            }
        else handler.removeCallbacksAndMessages(null)
    Box {
        Button(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(end = 24.dp, start = 24.dp),
            onClick = {
                myLocation = ""
                latLong = ""
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({
                    myLocation = "please check internet (VPN)"
                }, 5000)
            }
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                color = Color.White,
                text = "Find location"
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = myLocation)
            Spacer(modifier = Modifier.padding(8.dp))
            Text(text = latLong)
        }
    }

}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        Greeting()
    }
}