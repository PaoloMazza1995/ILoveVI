package com.pamaz.vi.feature.settings.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pamaz.vi.core.data.FeatureFlags
import com.pamaz.vi.core.data.repository.AccountErrorReason
import com.pamaz.vi.core.data.repository.AuthProviderId
import com.pamaz.vi.core.data.spotify.SpotifyConnectionState
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.design.IloveViThemeMode
import com.pamaz.vi.design.components.IconRefresh
import com.pamaz.vi.design.components.IconWarningTriangle
import com.pamaz.vi.design.components.IloveViButton
import com.pamaz.vi.design.components.IloveViButtonVariant
import com.pamaz.vi.design.components.IloveViChip
import com.pamaz.vi.design.components.IloveViChipGroup
import com.pamaz.vi.design.components.IloveViIconButton
import com.pamaz.vi.design.components.IloveViSwitch
import ilovevi.feature.settings.generated.resources.Res
import ilovevi.feature.settings.generated.resources.settings_about_disclaimer
import ilovevi.feature.settings.generated.resources.settings_about_licenses
import ilovevi.feature.settings.generated.resources.settings_about_licenses_body
import ilovevi.feature.settings.generated.resources.settings_about_version
import ilovevi.feature.settings.generated.resources.settings_account_continue_with_apple
import ilovevi.feature.settings.generated.resources.settings_account_continue_with_google
import ilovevi.feature.settings.generated.resources.settings_account_error_cancelled
import ilovevi.feature.settings.generated.resources.settings_account_error_developer_error
import ilovevi.feature.settings.generated.resources.settings_account_error_no_account
import ilovevi.feature.settings.generated.resources.settings_account_error_not_allowed
import ilovevi.feature.settings.generated.resources.settings_account_error_offline
import ilovevi.feature.settings.generated.resources.settings_account_error_provider_disabled
import ilovevi.feature.settings.generated.resources.settings_account_error_redirect_mismatch
import ilovevi.feature.settings.generated.resources.settings_account_error_unknown
import ilovevi.feature.settings.generated.resources.settings_account_retry
import ilovevi.feature.settings.generated.resources.settings_account_sign_out
import ilovevi.feature.settings.generated.resources.settings_account_signed_out_message
import ilovevi.feature.settings.generated.resources.settings_account_signing_in
import ilovevi.feature.settings.generated.resources.settings_appearance_language
import ilovevi.feature.settings.generated.resources.settings_appearance_theme
import ilovevi.feature.settings.generated.resources.settings_danger_delete_account
import ilovevi.feature.settings.generated.resources.settings_delete_account_dialog_cancel
import ilovevi.feature.settings.generated.resources.settings_delete_account_dialog_confirm
import ilovevi.feature.settings.generated.resources.settings_delete_account_dialog_message
import ilovevi.feature.settings.generated.resources.settings_delete_account_dialog_title
import ilovevi.feature.settings.generated.resources.settings_language_english
import ilovevi.feature.settings.generated.resources.settings_language_italian
import ilovevi.feature.settings.generated.resources.settings_map_data_update
import ilovevi.feature.settings.generated.resources.settings_map_data_update_days_ago
import ilovevi.feature.settings.generated.resources.settings_map_data_update_error
import ilovevi.feature.settings.generated.resources.settings_map_data_update_hours_ago
import ilovevi.feature.settings.generated.resources.settings_map_data_update_just_now
import ilovevi.feature.settings.generated.resources.settings_map_data_update_minutes_ago
import ilovevi.feature.settings.generated.resources.settings_map_data_update_never
import ilovevi.feature.settings.generated.resources.settings_map_refresh_content_description
import ilovevi.feature.settings.generated.resources.settings_map_show_labels
import ilovevi.feature.settings.generated.resources.settings_map_show_labels_subtitle
import ilovevi.feature.settings.generated.resources.settings_section_about
import ilovevi.feature.settings.generated.resources.settings_section_account
import ilovevi.feature.settings.generated.resources.settings_section_appearance
import ilovevi.feature.settings.generated.resources.settings_section_danger_zone
import ilovevi.feature.settings.generated.resources.settings_section_map
import ilovevi.feature.settings.generated.resources.settings_section_spotify
import ilovevi.feature.settings.generated.resources.settings_spotify_connect
import ilovevi.feature.settings.generated.resources.settings_spotify_connected
import ilovevi.feature.settings.generated.resources.settings_spotify_disconnect
import ilovevi.feature.settings.generated.resources.settings_spotify_disconnected_subtitle
import ilovevi.feature.settings.generated.resources.settings_theme_dark
import ilovevi.feature.settings.generated.resources.settings_theme_light
import ilovevi.feature.settings.generated.resources.settings_theme_system
import ilovevi.feature.settings.generated.resources.settings_title
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Settings tab (step 13B). Wires [SettingsViewModel] into [SettingsScreenContent] — the real
 * app's only caller of that stateless composable; `@Preview`s below call it directly with a fixed
 * [SettingsUiState] instead (this repo has no screenshot-testing tooling, per `core:design`'s
 * step-I precedent — `@Preview`s in both themes are what substitutes for it here).
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel(), modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreenContent(
        state = state,
        onSignIn = viewModel::onSignIn,
        onSignOut = viewModel::onSignOut,
        onThemeModeSelect = viewModel::onThemeModeSelect,
        onLanguageSelect = viewModel::onLanguageSelect,
        onShowLabelsToggle = viewModel::onShowLabelsToggle,
        onRefreshData = viewModel::onRefreshData,
        onSpotifyConnect = viewModel::onSpotifyConnect,
        onSpotifyDisconnect = viewModel::onSpotifyDisconnect,
        onDeleteAccountConfirm = viewModel::onDeleteAccountConfirm,
        modifier = modifier,
    )
}

/**
 * The screen's whole section layout — every section in the step's required order: Account (only
 * when [FeatureFlags.accountLogin] is on — see that flag's own KDoc), Appearance, Map, Spotify
 * (only when [FeatureFlags.spotify] is on), About, Danger zone (only when signed in, which can't
 * happen when Account is hidden anyway). Stateless (no `ViewModel`/Koin dependency) so it's
 * directly usable from a `@Preview`.
 */
@Composable
internal fun SettingsScreenContent(
    state: SettingsUiState,
    onSignIn: (AuthProviderId) -> Unit,
    onSignOut: () -> Unit,
    onThemeModeSelect: (ThemeModeOption) -> Unit,
    onLanguageSelect: (LanguageOption) -> Unit,
    onShowLabelsToggle: (Boolean) -> Unit,
    onRefreshData: () -> Unit,
    onSpotifyConnect: () -> Unit,
    onSpotifyDisconnect: () -> Unit,
    onDeleteAccountConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IloveViColors.bg900)
            .verticalScroll(rememberScrollState())
            .padding(IloveViSpacing.space6),
        verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space7),
    ) {
        Text(
            text = stringResource(Res.string.settings_title),
            color = IloveViColors.ink,
            style = MaterialTheme.typography.displayMedium,
        )

        if (FeatureFlags.accountLogin) {
            AccountSection(state = state, onSignIn = onSignIn, onSignOut = onSignOut)
        }

        AppearanceSection(
            themeMode = state.themeMode,
            onThemeModeSelect = onThemeModeSelect,
            language = state.language,
            onLanguageSelect = onLanguageSelect,
        )

        MapSection(
            showLabels = state.showLabels,
            onShowLabelsToggle = onShowLabelsToggle,
            lastUpdateAt = state.lastUpdateAt,
            isRefreshing = state.isRefreshingData,
            refreshError = state.dataRefreshError,
            onRefresh = onRefreshData,
        )

        if (FeatureFlags.spotify) {
            SpotifySection(
                spotifyState = state.spotifyState,
                onConnect = onSpotifyConnect,
                onDisconnect = onSpotifyDisconnect,
            )
        }

        AboutSection(appVersion = state.appVersion)

        if (FeatureFlags.accountLogin && state.account is AccountUiState.SignedIn) {
            DangerZoneSection(onDeleteAccountClick = { showDeleteConfirm = true })
        }
    }

    if (showDeleteConfirm) {
        DeleteAccountDialog(
            isDeleting = state.isDeletingAccount,
            onConfirm = {
                showDeleteConfirm = false
                onDeleteAccountConfirm()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space3)) {
        Text(
            text = title.uppercase(),
            color = IloveViColors.brandPink,
            style = MaterialTheme.typography.labelMedium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space4), content = content)
    }
}

// ---- Account ----

@Composable
private fun AccountSection(
    state: SettingsUiState,
    onSignIn: (AuthProviderId) -> Unit,
    onSignOut: () -> Unit,
) {
    SettingsSection(title = stringResource(Res.string.settings_section_account)) {
        when (val account = state.account) {
            is AccountUiState.SignedOut -> SignedOutAccountContent(
                availableProviders = state.availableProviders,
                onSignIn = onSignIn,
            )

            is AccountUiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        color = IloveViColors.brandPink,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(Res.string.settings_account_signing_in),
                        color = IloveViColors.inkMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            is AccountUiState.SignedIn -> SignedInAccountContent(
                account = account,
                onSignOut = onSignOut
            )

            is AccountUiState.Error -> Column(
                verticalArrangement = Arrangement.spacedBy(
                    IloveViSpacing.space3
                )
            ) {
                Text(
                    text = accountErrorMessage(account.reason),
                    color = IloveViColors.warningYellow,
                    style = MaterialTheme.typography.bodyMedium,
                )
                IloveViButton(
                    text = stringResource(Res.string.settings_account_retry),
                    onClick = { state.availableProviders.firstOrNull()?.let(onSignIn) },
                    variant = IloveViButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
private fun SignedOutAccountContent(
    availableProviders: List<AuthProviderId>,
    onSignIn: (AuthProviderId) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space3)) {
        if (AuthProviderId.GOOGLE in availableProviders) {
            IloveViButton(
                text = stringResource(Res.string.settings_account_continue_with_google),
                onClick = { onSignIn(AuthProviderId.GOOGLE) },
                variant = IloveViButtonVariant.Primary,
            )
        }
        if (AuthProviderId.APPLE in availableProviders) {
            IloveViButton(
                text = stringResource(Res.string.settings_account_continue_with_apple),
                onClick = { onSignIn(AuthProviderId.APPLE) },
                variant = IloveViButtonVariant.Secondary,
            )
        }
        Text(
            text = stringResource(Res.string.settings_account_signed_out_message),
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SignedInAccountContent(account: AccountUiState.SignedIn, onSignOut: () -> Unit) {
    val displayLabel = account.displayName ?: account.email ?: ""
    val initial = displayLabel.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(IloveViColors.brandPink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                color = IloveViColors.inkOnBrand,
                style = MaterialTheme.typography.titleLarge
            )
        }
        Column(
            modifier = Modifier.weight(1f).padding(start = IloveViSpacing.space3),
        ) {
            if (account.displayName != null) {
                Text(
                    text = account.displayName,
                    color = IloveViColors.ink,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (account.email != null) {
                Text(
                    text = account.email,
                    color = IloveViColors.inkMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    IloveViButton(
        text = stringResource(Res.string.settings_account_sign_out),
        onClick = onSignOut,
        variant = IloveViButtonVariant.Secondary,
    )
}

@Composable
private fun accountErrorMessage(reason: AccountErrorReason): String = stringResource(
    when (reason) {
        AccountErrorReason.CANCELLED -> Res.string.settings_account_error_cancelled
        AccountErrorReason.NO_ACCOUNT -> Res.string.settings_account_error_no_account
        AccountErrorReason.DEVELOPER_ERROR -> Res.string.settings_account_error_developer_error
        AccountErrorReason.NOT_ALLOWED -> Res.string.settings_account_error_not_allowed
        AccountErrorReason.REDIRECT_MISMATCH -> Res.string.settings_account_error_redirect_mismatch
        AccountErrorReason.PROVIDER_DISABLED -> Res.string.settings_account_error_provider_disabled
        AccountErrorReason.OFFLINE -> Res.string.settings_account_error_offline
        AccountErrorReason.UNKNOWN -> Res.string.settings_account_error_unknown
    },
)

// ---- Appearance ----

@Composable
private fun AppearanceSection(
    themeMode: ThemeModeOption,
    onThemeModeSelect: (ThemeModeOption) -> Unit,
    language: LanguageOption,
    onLanguageSelect: (LanguageOption) -> Unit,
) {
    SettingsSection(title = stringResource(Res.string.settings_section_appearance)) {
        Column(verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space2)) {
            Text(
                text = stringResource(Res.string.settings_appearance_theme),
                color = IloveViColors.ink,
                style = MaterialTheme.typography.bodyLarge,
            )
            RadioChipGroup(
                options = ThemeModeOption.entries,
                selectedOption = themeMode,
                labelFor = { themeModeLabel(it) },
                onSelect = onThemeModeSelect,
                groupContentDescription = stringResource(Res.string.settings_appearance_theme),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space2)) {
            Text(
                text = stringResource(Res.string.settings_appearance_language),
                color = IloveViColors.ink,
                style = MaterialTheme.typography.bodyLarge,
            )
            RadioChipGroup(
                options = LanguageOption.entries,
                selectedOption = language,
                labelFor = { languageLabel(it) },
                onSelect = onLanguageSelect,
                groupContentDescription = stringResource(Res.string.settings_appearance_language),
            )
        }
    }
}

@Composable
private fun themeModeLabel(mode: ThemeModeOption): String = stringResource(
    when (mode) {
        ThemeModeOption.SYSTEM -> Res.string.settings_theme_system
        ThemeModeOption.LIGHT -> Res.string.settings_theme_light
        ThemeModeOption.DARK -> Res.string.settings_theme_dark
    },
)

@Composable
private fun languageLabel(language: LanguageOption): String = stringResource(
    when (language) {
        LanguageOption.EN -> Res.string.settings_language_english
        LanguageOption.IT -> Res.string.settings_language_italian
    },
)

/**
 * A row of [IloveViChip]s behaving as a single-choice radio group — `Role.RadioButton` + `selected`
 * semantics on each one, `selectableGroup`-equivalent single-selection enforced by [onSelect]
 * always replacing the whole selection. Local to this screen: [core.design]'s `IloveViChip` is a
 * generic filter/selection pill, not semantically a radio button by itself.
 */
@Composable
private fun <T> RadioChipGroup(
    options: List<T>,
    selectedOption: T,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    groupContentDescription: String,
) {
    IloveViChipGroup(modifier = Modifier.semantics {
        contentDescription = groupContentDescription
    }) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            IloveViChip(
                label = labelFor(option),
                active = isSelected,
                onClick = { onSelect(option) },
                modifier = Modifier.semantics {
                    role = Role.RadioButton
                    this.selected = isSelected
                },
            )
        }
    }
}

// ---- Map ----

@Composable
private fun MapSection(
    showLabels: Boolean,
    onShowLabelsToggle: (Boolean) -> Unit,
    lastUpdateAt: Instant?,
    isRefreshing: Boolean,
    refreshError: String?,
    onRefresh: () -> Unit,
) {
    SettingsSection(title = stringResource(Res.string.settings_section_map)) {
        Row(
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                role = Role.Switch
                selected = showLabels
            }.clickable(role = Role.Switch) { onShowLabelsToggle(!showLabels) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_map_show_labels),
                    color = IloveViColors.ink,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(Res.string.settings_map_show_labels_subtitle),
                    color = IloveViColors.inkMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IloveViSwitch(checked = showLabels, onCheckedChange = null)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_map_data_update),
                    color = IloveViColors.ink,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = if (refreshError != null) {
                        stringResource(Res.string.settings_map_data_update_error)
                    } else {
                        dataUpdateText(lastUpdateAt)
                    },
                    color = if (refreshError != null) IloveViColors.warningYellow else IloveViColors.inkMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (isRefreshing) {
                CircularProgressIndicator(
                    color = IloveViColors.brandPink,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                IloveViIconButton(
                    contentDescription = stringResource(Res.string.settings_map_refresh_content_description),
                    onClick = onRefresh,
                ) { tint -> IconRefresh(tint = tint) }
            }
        }
    }
}

@Composable
private fun dataUpdateText(lastUpdateAt: Instant?): String {
    if (lastUpdateAt == null) return stringResource(Res.string.settings_map_data_update_never)
    val elapsed = Clock.System.now() - lastUpdateAt
    return when {
        elapsed < 1.minutes -> stringResource(Res.string.settings_map_data_update_just_now)
        elapsed < 1.hours -> stringResource(
            Res.string.settings_map_data_update_minutes_ago,
            elapsed.inWholeMinutes.toInt()
        )

        elapsed < 1.days -> stringResource(
            Res.string.settings_map_data_update_hours_ago,
            elapsed.inWholeHours.toInt()
        )

        else -> stringResource(
            Res.string.settings_map_data_update_days_ago,
            elapsed.inWholeDays.toInt()
        )
    }
}

// ---- Spotify ----

@Composable
private fun SpotifySection(
    spotifyState: SpotifyConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    SettingsSection(title = stringResource(Res.string.settings_section_spotify)) {
        when (spotifyState) {
            is SpotifyConnectionState.Connected -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        Res.string.settings_spotify_connected,
                        spotifyState.displayName
                    ),
                    color = IloveViColors.ink,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                IloveViButton(
                    text = stringResource(Res.string.settings_spotify_disconnect),
                    onClick = onDisconnect,
                    variant = IloveViButtonVariant.Secondary,
                )
            }

            is SpotifyConnectionState.Disconnected, SpotifyConnectionState.Unavailable -> Column(
                verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space3),
            ) {
                Text(
                    text = stringResource(Res.string.settings_spotify_disconnected_subtitle),
                    color = IloveViColors.inkMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                IloveViButton(
                    text = stringResource(Res.string.settings_spotify_connect),
                    onClick = onConnect,
                    variant = IloveViButtonVariant.Primary,
                )
            }
        }
    }
}

// ---- About ----

@Composable
private fun AboutSection(appVersion: String) {
    SettingsSection(title = stringResource(Res.string.settings_section_about)) {
        Text(
            text = stringResource(Res.string.settings_about_disclaimer),
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Column(verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space1)) {
            Text(
                text = stringResource(Res.string.settings_about_licenses),
                color = IloveViColors.ink,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(Res.string.settings_about_licenses_body),
                color = IloveViColors.inkMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = stringResource(Res.string.settings_about_version, appVersion),
            color = IloveViColors.inkFaint,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

// ---- Danger zone ----

@Composable
private fun DangerZoneSection(onDeleteAccountClick: () -> Unit) {
    SettingsSection(title = stringResource(Res.string.settings_section_danger_zone)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                .clickable(onClick = onDeleteAccountClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space2),
        ) {
            IconWarningTriangle(tint = IloveViColors.warningYellow)
            Text(
                text = stringResource(Res.string.settings_danger_delete_account),
                color = IloveViColors.warningYellow,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun DeleteAccountDialog(isDeleting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = {
            Text(
                text = stringResource(Res.string.settings_delete_account_dialog_title),
                color = IloveViColors.ink,
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.settings_delete_account_dialog_message),
                color = IloveViColors.inkMuted,
            )
        },
        confirmButton = {
            IloveViButton(
                text = stringResource(Res.string.settings_delete_account_dialog_confirm),
                onClick = onConfirm,
                variant = IloveViButtonVariant.Primary,
            )
        },
        dismissButton = {
            IloveViButton(
                text = stringResource(Res.string.settings_delete_account_dialog_cancel),
                onClick = onDismiss,
                variant = IloveViButtonVariant.Secondary,
            )
        },
        containerColor = IloveViColors.bg800,
    )
}

// ---- Previews (no screenshot-testing tooling in this repo — see core:design's step-I precedent) ----

private val PREVIEW_SIGNED_OUT_STATE = SettingsUiState(
    account = AccountUiState.SignedOut,
    availableProviders = listOf(AuthProviderId.GOOGLE, AuthProviderId.APPLE),
    themeMode = ThemeModeOption.SYSTEM,
    language = LanguageOption.EN,
    showLabels = true,
    appVersion = "1.0",
)

private val PREVIEW_SIGNED_IN_STATE = SettingsUiState(
    account = AccountUiState.SignedIn(
        displayName = "Paolo",
        email = "paolo@example.com",
        avatarUrl = null
    ),
    availableProviders = listOf(AuthProviderId.GOOGLE, AuthProviderId.APPLE),
    themeMode = ThemeModeOption.DARK,
    language = LanguageOption.IT,
    showLabels = false,
    dataRefreshError = "weapons: offline",
    appVersion = "1.0",
)

@Composable
private fun PreviewScaffold(themeMode: IloveViThemeMode, state: SettingsUiState) {
    IloveViTheme(themeMode = themeMode) {
        SettingsScreenContent(
            state = state,
            onSignIn = {},
            onSignOut = {},
            onThemeModeSelect = {},
            onLanguageSelect = {},
            onShowLabelsToggle = {},
            onRefreshData = {},
            onSpotifyConnect = {},
            onSpotifyDisconnect = {},
            onDeleteAccountConfirm = {},
        )
    }
}

@Preview
@Composable
private fun SettingsScreenSignedOutDarkPreview() {
    PreviewScaffold(IloveViThemeMode.Dark, PREVIEW_SIGNED_OUT_STATE)
}

@Preview
@Composable
private fun SettingsScreenSignedOutLightPreview() {
    PreviewScaffold(IloveViThemeMode.Light, PREVIEW_SIGNED_OUT_STATE)
}

@Preview
@Composable
private fun SettingsScreenSignedInDarkPreview() {
    PreviewScaffold(IloveViThemeMode.Dark, PREVIEW_SIGNED_IN_STATE)
}

@Preview
@Composable
private fun SettingsScreenSignedInLightPreview() {
    PreviewScaffold(IloveViThemeMode.Light, PREVIEW_SIGNED_IN_STATE)
}
