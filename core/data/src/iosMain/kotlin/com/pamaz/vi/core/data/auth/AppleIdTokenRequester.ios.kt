package com.pamaz.vi.core.data.auth

import com.pamaz.vi.core.data.repository.AccountErrorReason
import com.pamaz.vi.core.database.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASAuthorizationRequest
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Real implementation — `ASAuthorizationAppleIDProvider`, a system framework (no CocoaPods/SPM),
 * written directly in Kotlin per this repo's established iOS convention (see this class's
 * commonMain KDoc). A delegate/presentation-anchor pair (`NSObject` subclasses implementing the
 * two `AuthenticationServices` protocols) bridges the SDK's callback API into a suspend function
 * via [suspendCancellableCoroutine] — there is no `async`/`await`-shaped Kotlin/Native API for
 * this framework to call directly.
 */
@Suppress("CAST_NEVER_SUCCEEDS")
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual class AppleIdTokenRequester actual constructor(context: PlatformContext) {

    actual suspend fun requestIdToken(): Result<AppleIdTokenResult> {
        val rawNonce = randomNonce()
        val hashedNonce = sha256Hex(rawNonce)

        val provider = ASAuthorizationAppleIDProvider()
        val request = provider.createRequest()
        request.requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
        request.nonce = hashedNonce

        return try {
            val credential = performRequest(request)
            val tokenData = credential.identityToken
            val identityToken = tokenData?.let {
                NSString.create(it, NSUTF8StringEncoding) as String
            }
            if (identityToken.isNullOrBlank()) {
                Result.failure(NativeSignInException(AccountErrorReason.UNKNOWN))
            } else {
                Result.success(
                    AppleIdTokenResult(
                        identityToken = identityToken,
                        rawNonce = rawNonce
                    )
                )
            }
        } catch (e: AppleSignInCancelledException) {
            Result.failure(NativeSignInException(AccountErrorReason.CANCELLED))
        } catch (e: Exception) {
            Result.failure(NativeSignInException(AccountErrorReason.UNKNOWN))
        }
    }

    private suspend fun performRequest(
        request: ASAuthorizationRequest,
    ): ASAuthorizationAppleIDCredential = suspendCancellableCoroutine { continuation ->
        val controller = ASAuthorizationController(authorizationRequests = listOf(request))

        val delegate = object : NSObject(), ASAuthorizationControllerDelegateProtocol {
            override fun authorizationController(
                controller: ASAuthorizationController,
                didCompleteWithAuthorization: ASAuthorization,
            ) {
                val credential =
                    didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
                if (credential == null) {
                    continuation.resumeWithException(IllegalStateException("Unexpected credential type"))
                } else {
                    continuation.resume(credential)
                }
            }

            override fun authorizationController(
                controller: ASAuthorizationController,
                didCompleteWithError: NSError,
            ) {
                if (didCompleteWithError.code == ASAuthorizationErrorCanceled) {
                    continuation.resumeWithException(AppleSignInCancelledException())
                } else {
                    continuation.resumeWithException(IllegalStateException(didCompleteWithError.domain))
                }
            }
        }

        val presentationProvider =
            object : NSObject(), ASAuthorizationControllerPresentationContextProvidingProtocol {
                override fun presentationAnchorForAuthorizationController(
                    controller: ASAuthorizationController,
                ): ASPresentationAnchor = UIApplication.sharedApplication.keyWindow
            }

        // Kept alive for the duration of the (async, user-driven) request via these local vals
        // captured in the closures above — ARC keeps the controller/delegate/provider alive as
        // long as `controller.delegate`/`controller.presentationContextProvider` hold them.
        controller.delegate = delegate
        controller.presentationContextProvider = presentationProvider
        controller.performRequests()

        continuation.invokeOnCancellation { }
    }
}

private class AppleSignInCancelledException : Exception()

private fun randomNonce(): String {
    val allowed = "0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._"
    return (1..32).map { allowed.random() }.joinToString("")
}
