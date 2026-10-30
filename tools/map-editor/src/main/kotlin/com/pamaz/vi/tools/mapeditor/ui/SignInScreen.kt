package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.design.components.IconWarningTriangle
import com.pamaz.vi.design.components.IloveViButton
import com.pamaz.vi.design.components.IloveViButtonVariant
import com.pamaz.vi.design.components.IloveViTextField
import com.pamaz.vi.tools.mapeditor.data.fake.FakeAuthService
import com.pamaz.vi.tools.mapeditor.model.SignInResult
import kotlinx.coroutines.launch

/**
 * EDT-01: the sign-in dialog — matches the Login screenshot. [isFakeBackend] gates the "Running
 * on fake data" hint (EDT-01 doesn't call for it against a real project — it'd be actively
 * misleading there, not just unnecessary).
 */
@Composable
fun SignInScreen(
    onSignIn: suspend (String, String) -> SignInResult,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    isFakeBackend: Boolean = true,
) {
    var email by remember { mutableStateOf("admin@example.com") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier.fillMaxSize().background(IloveViColors.bg900),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .width(380.dp)
                .clip(IloveViShapes.radiusLg)
                .background(IloveViColors.bg800, IloveViShapes.radiusLg)
                .border(1.dp, IloveViColors.border, IloveViShapes.radiusLg)
                .padding(IloveViSpacing.space6),
        ) {
            Row {
                Text(
                    "ILOVEVI",
                    color = IloveViColors.brandPink,
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    " MAP EDITOR",
                    color = IloveViColors.ink,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Text(
                "Sign in with the admin account to save pins and entries. You'll stay signed in the next time you open the tool, until you sign out.",
                color = IloveViColors.inkMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(
                    top = IloveViSpacing.space2,
                    bottom = IloveViSpacing.space5
                ),
            )

            IloveViTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                modifier = Modifier.testTag("sign-in-email"),
            )
            Box(Modifier.padding(top = IloveViSpacing.space3)) {
                IloveViTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    isPassword = true,
                    modifier = Modifier.testTag("sign-in-password"),
                )
            }
            error?.let {
                Text(
                    it,
                    color = IloveViColors.warningYellow,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = IloveViSpacing.space2),
                )
            }

            IloveViButton(
                text = "SIGN IN",
                enabled = !isSigningIn,
                useDisplayFont = true,
                onClick = {
                    isSigningIn = true
                    error = null
                    scope.launch {
                        when (val result = onSignIn(email, password)) {
                            is SignInResult.Success -> onSuccess()
                            is SignInResult.InvalidCredentials -> error = result.message
                            is SignInResult.NetworkError -> error = result.message
                        }
                        isSigningIn = false
                    }
                },
                modifier = Modifier.padding(top = IloveViSpacing.space5),
            )

            if (isFakeBackend) {
                Text(
                    "Running on fake data (no live Supabase project). Sign in with any password — admin@example.com is the admin account.",
                    color = IloveViColors.inkFaint,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = IloveViSpacing.space4),
                )
            }
        }
    }
}

/** EDT-02: shown after a successful-but-not-admin sign-in, matching the "not an admin" screenshot. */
@Composable
fun NotAdminScreen(
    email: String,
    onSignOut: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.fillMaxSize().background(IloveViColors.bg900),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .width(420.dp)
                .clip(IloveViShapes.radiusLg)
                .background(IloveViColors.bg800, IloveViShapes.radiusLg)
                .border(1.dp, IloveViColors.border, IloveViShapes.radiusLg)
                .padding(IloveViSpacing.space6),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconWarningTriangle(size = 28.dp)
                Text(
                    "THIS ACCOUNT IS NOT AN ADMIN",
                    color = IloveViColors.ink,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(start = IloveViSpacing.space2),
                )
            }
            Text(
                "You are signed in as $email, but the token has no admin role. You can look at the map and prepare a draft, but Commit stays disabled because the database would refuse the write.",
                color = IloveViColors.inkMuted,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = IloveViSpacing.space4),
            )
            Box(
                Modifier
                    .padding(top = IloveViSpacing.space4)
                    .clip(IloveViShapes.radiusSm)
                    .background(IloveViColors.bg900, IloveViShapes.radiusSm)
                    .padding(IloveViSpacing.space3),
            ) {
                Text(
                    "To fix it: give the user app_metadata.role = \"admin\" in Supabase, then sign out and sign in again so the token is refreshed.",
                    color = IloveViColors.inkMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                Modifier.padding(top = IloveViSpacing.space5),
                horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space3),
            ) {
                IloveViButton(
                    text = "Sign out",
                    onClick = onSignOut,
                    variant = IloveViButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                IloveViButton(
                    text = "Continue without committing",
                    onClick = onContinue,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SignInScreenPreview() {
    val authService = remember { FakeAuthService() }
    IloveViTheme {
        SignInScreen(onSignIn = authService::signIn, onSuccess = {})
    }
}

@Preview
@Composable
private fun SignInScreenRealBackendPreview() {
    val authService = remember { FakeAuthService() }
    IloveViTheme {
        SignInScreen(onSignIn = authService::signIn, onSuccess = {}, isFakeBackend = false)
    }
}

@Preview
@Composable
private fun NotAdminScreenPreview() {
    IloveViTheme {
        NotAdminScreen(email = "player@example.com", onSignOut = {}, onContinue = {})
    }
}
