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
import platform.CoreFoundation.CFTypeRefVar
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
 * store). `kSecClass`/etc. are `CFStringRef` constants; cast `as NSString` to use as
 * `NSMutableDictionary` keys/values, and the dictionary itself `as CFDictionaryRef` to call the
 * `Security` framework's plain-C API — both casts are the standard Kotlin/Native idiom for
 * Foundation/CoreFoundation's toll-free-bridged types (`NSString`<->`CFStringRef`,
 * `NSDictionary`<->`CFDictionaryRef`), verified by actually compiling this file against the
 * `iosSimulatorArm64` target, not from memory.
 */
@Suppress("CAST_NEVER_SUCCEEDS")
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual class SecureSessionManager actual constructor(context: PlatformContext) : SessionManager {

    actual override suspend fun saveSession(session: UserSession) {
        deleteFromKeychain()
        val query = baseQuery()
        query.setObject(stringToData(encodeSession(session)), forKey = kSecValueData as NSString)
        SecItemAdd(query as CFDictionaryRef, null)
    }

    actual override suspend fun loadSession(): UserSession? {
        val query = baseQuery()
        query.setObject(NSNumber(bool = true), forKey = kSecReturnData as NSString)
        query.setObject(kSecMatchLimitOne as NSString, forKey = kSecMatchLimit as NSString)
        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
            if (status != errSecSuccess) return@memScoped null
            val data = result.value as? NSData ?: return@memScoped null
            decodeSessionOrNull(dataToString(data))
        }
    }

    actual override suspend fun deleteSession() {
        deleteFromKeychain()
    }

    private fun deleteFromKeychain() {
        SecItemDelete(baseQuery() as CFDictionaryRef)
    }

    private fun baseQuery(): NSMutableDictionary {
        val dict = NSMutableDictionary()
        dict.setObject(kSecClassGenericPassword as NSString, forKey = kSecClass as NSString)
        dict.setObject(SERVICE, forKey = kSecAttrService as NSString)
        dict.setObject(ACCOUNT, forKey = kSecAttrAccount as NSString)
        return dict
    }

    private fun stringToData(value: String): NSData =
        (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!

    private fun dataToString(data: NSData): String =
        NSString.create(data, NSUTF8StringEncoding) as String
}
