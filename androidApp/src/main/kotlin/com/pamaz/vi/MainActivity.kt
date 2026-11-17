package com.pamaz.vi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }

    /** Step 13B: the Google/Apple sign-in web-OAuth redirect (`com.pamaz.vi://login-callback`) lands here — see `handleAndroidAuthDeepLink`'s own KDoc. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAndroidAuthDeepLink(intent)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}