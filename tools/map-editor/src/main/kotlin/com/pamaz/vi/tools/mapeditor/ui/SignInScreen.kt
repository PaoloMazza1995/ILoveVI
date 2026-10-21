package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pamaz.vi.tools.mapeditor.auth.AdminSignInResult
import kotlinx.coroutines.launch

/** PLT-01: email/password sign-in, with a specific message for "wrong credentials" vs "not an admin" vs "can't reach the backend". */
@Composable
fun SignInScreen(
    onSignIn: suspend (email: String, password: String) -> AdminSignInResult,
    onSignedIn: () -> Unit,
    onContinueOffline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("IloveVI Map Editor", style = MaterialTheme.typography.headlineSmall)
        Text("Administrator sign-in", style = MaterialTheme.typography.bodyMedium)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.width(320.dp),
            singleLine = true,
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.width(320.dp).padding(top = 8.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        if (errorText != null) {
            Text(
                errorText!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp).width(320.dp),
            )
        }
        Button(
            enabled = !isSigningIn && email.isNotBlank() && password.isNotBlank(),
            onClick = {
                isSigningIn = true
                scope.launch {
                    when (val result = onSignIn(email.trim(), password)) {
                        is AdminSignInResult.Success -> onSignedIn()
                        is AdminSignInResult.InvalidCredentials -> errorText = result.message
                        is AdminSignInResult.NotAnAdmin -> errorText =
                            "${result.email} signed in but does not have the admin role."

                        is AdminSignInResult.NetworkError -> errorText =
                            "Could not reach the backend: ${result.message}"
                    }
                    isSigningIn = false
                }
            },
            modifier = Modifier.padding(top = 16.dp).width(320.dp),
        ) {
            Text(if (isSigningIn) "Signing in..." else "Sign in")
        }
        androidx.compose.material3.TextButton(
            onClick = onContinueOffline,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Work offline from a local image instead")
        }
    }
}
