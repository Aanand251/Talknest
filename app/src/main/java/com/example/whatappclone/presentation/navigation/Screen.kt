package com.example.whatappclone.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object EmailAuth : Screen("email_auth")
    object PhoneAuth : Screen("phone_auth")
    object OtpVerification : Screen("otp_verification/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "otp_verification/$phoneNumber"
    }
    object ProfileSetup : Screen("profile_setup")
    object Home : Screen("home")
    object ChatList : Screen("chat_list")
    object UserSelection : Screen("user_selection")
    object Chat : Screen("chat/{chatId}/{otherUserId}") {
        fun createRoute(chatId: String, otherUserId: String) = "chat/$chatId/$otherUserId"
    }
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object EditProfile : Screen("edit_profile")
    object AddContact : Screen("add_contact")
    object ContactRequests : Screen("contact_requests")
    object FriendsList : Screen("friends_list")
    object ProfileQR : Screen("profile_qr")
    object QRScanner : Screen("qr_scanner")
    object Call : Screen("call/{callId}/{isIncoming}") {
        fun createRoute(callId: String, isIncoming: Boolean = false) = "call/$callId/$isIncoming"
    }
}
