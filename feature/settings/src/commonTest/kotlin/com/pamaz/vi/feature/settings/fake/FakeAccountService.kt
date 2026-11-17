package com.pamaz.vi.feature.settings.fake

import com.pamaz.vi.core.data.repository.AccountErrorException
import com.pamaz.vi.core.data.repository.AccountErrorReason
import com.pamaz.vi.core.data.repository.AccountService
import com.pamaz.vi.core.data.repository.AccountState
import com.pamaz.vi.core.data.repository.AuthProviderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A tiny in-memory stand-in for the real [AccountService], for [com.pamaz.vi.feature.settings.settings.SettingsViewModel] tests. */
internal class FakeAccountService(
    initial: AccountState = AccountState.SignedOut,
    override var availableProviders: List<AuthProviderId> = listOf(AuthProviderId.GOOGLE),
) : AccountService {
    private val stateFlow = MutableStateFlow(initial)
    override val state: StateFlow<AccountState> = stateFlow

    var signInResult: Result<Unit> = Result.success(Unit)
    var signOutCallCount: Int = 0
    var deleteAccountResult: Result<Unit> = Result.success(Unit)
    var lastSignInProvider: AuthProviderId? = null

    /** When set, [signIn] applies this to [state] right before returning — the shape a real sign-in success/failure takes. */
    var onSignInEffect: ((AuthProviderId) -> Unit)? = null

    fun setState(newState: AccountState) {
        stateFlow.value = newState
    }

    override suspend fun signIn(provider: AuthProviderId): Result<Unit> {
        lastSignInProvider = provider
        onSignInEffect?.invoke(provider)
        return signInResult
    }

    override suspend fun signOut() {
        signOutCallCount++
        stateFlow.value = AccountState.SignedOut
    }

    override suspend fun deleteAccount(): Result<Unit> = deleteAccountResult
}

internal fun accountErrorFailure(reason: AccountErrorReason): Result<Unit> =
    Result.failure(AccountErrorException(reason))
