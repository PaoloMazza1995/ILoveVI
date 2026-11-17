import SwiftUI
import Shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                // Step 13B: Google sign-in's web-OAuth redirect (com.pamaz.vi://login-callback)
                // opens the app here — forwarded straight into Kotlin, which owns the actual
                // Supabase session completion (AuthDeepLinks.ios.kt). No auth logic in Swift.
                .onOpenURL { url in
                    AuthDeepLinks_iosKt.handleIosAuthDeepLink(url: url)
                }
        }
    }
}