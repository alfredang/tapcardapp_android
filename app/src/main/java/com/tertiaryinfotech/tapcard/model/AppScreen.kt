package com.tertiaryinfotech.tapcard.model

/** Top-level screens the app routes between (mirrors the iOS app's flow). */
enum class AppScreen {
    HOME,       // "My Cards" list
    SCAN,       // Camera + OCR capture
    REVIEW,     // Confirm / edit the parsed fields
    CARD,       // The finished digital card with QR
    CONTACTS,   // Captured leads + saved contacts (people you meet)
    ANALYTICS,  // Card performance (views, taps, leads)
    SETTINGS,   // Account, sign in/out, app info
}
