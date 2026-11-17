package com.pamaz.vi.core.data.auth

import com.pamaz.vi.core.data.repository.AccountErrorReason

/** Thrown by [GoogleIdTokenRequester]/[AppleIdTokenRequester]'s platform `actual`s — [AccountServiceImpl] catches this and nothing else needs to know the real platform exception type. */
class NativeSignInException(val reason: AccountErrorReason) : Exception()
