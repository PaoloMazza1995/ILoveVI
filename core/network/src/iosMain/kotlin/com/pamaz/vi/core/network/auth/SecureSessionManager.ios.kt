package com.pamaz.vi.core.network.auth

import com.pamaz.vi.core.database.PlatformContext
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private const val SERVICE = "com.pamaz.vi.supabase.session"
private const val ACCOUNT = "session"

/**
 * iOS Keychain (`kSecClassGenericPassword`, Apple's own secure, per-app-sandboxed credential
 * store). Crossing the Foundation (`NSString`/`NSDictionary`) <-> CoreFoundation
 * (`CFStringRef`/`CFDictionaryRef`) boundary needs the explicit `CFBridging*` bridge functions on
 * both sides — Kotlin/Native wraps CF's `CPointer`s and Foundation's `ObjCObject`s as genuinely
 * different Kotlin types, so a plain `as` cast between them compiles (it's unchecked, hence
 * `@Suppress("CAST_NEVER_SUCCEEDS")`) but throws at runtime despite the two being toll-free
 * bridged at the ABI level. [toNSString] converts the `kSec*` constants (safe without a matching
 * retain — they're process-lifetime constant strings); [useAsCFDictionary] converts the query
 * dictionary the other direction, balancing the bridge's implied retain with [CFRelease].
 */
@Suppress("CAST_NEVER_SUCCEEDS")
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual class SecureSessionManager actual constructor(context: PlatformContext) : SessionManager {

    actual override suspend fun saveSession(session: UserSession) {
        deleteFromKeychain()
        val query = baseQuery()
        query.setObject(stringToData(encodeSession(session)), forKey = kSecValueData.toNSString())
        query.useAsCFDictionary { SecItemAdd(it, null) }
    }

    actual override suspend fun loadSession(): UserSession? {
        val query = baseQuery()
        query.setObject(NSNumber(bool = true), forKey = kSecReturnData.toNSString())
        query.setObject(kSecMatchLimitOne.toNSString(), forKey = kSecMatchLimit.toNSString())
        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = query.useAsCFDictionary { SecItemCopyMatching(it, result.ptr) }
            if (status != errSecSuccess) return@memScoped null
            val data = result.value as? NSData ?: return@memScoped null
            decodeSessionOrNull(dataToString(data))
        }
    }

    actual override suspend fun deleteSession() {
        deleteFromKeychain()
    }

    private fun deleteFromKeychain() {
        baseQuery().useAsCFDictionary { SecItemDelete(it) }
    }

    private fun baseQuery(): NSMutableDictionary {
        val dict = NSMutableDictionary()
        dict.setObject(kSecClassGenericPassword.toNSString(), forKey = kSecClass.toNSString())
        dict.setObject(SERVICE, forKey = kSecAttrService.toNSString())
        dict.setObject(ACCOUNT, forKey = kSecAttrAccount.toNSString())
        return dict
    }

    private fun CFStringRef?.toNSString(): NSString = CFBridgingRelease(this) as NSString

    private inline fun <T> NSMutableDictionary.useAsCFDictionary(block: (CFDictionaryRef) -> T): T {
        val ref = CFBridgingRetain(this) as CFDictionaryRef
        try {
            return block(ref)
        } finally {
            CFRelease(ref)
        }
    }

    private fun stringToData(value: String): NSData =
        (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!

    private fun dataToString(data: NSData): String =
        NSString.create(data, NSUTF8StringEncoding) as String
}
