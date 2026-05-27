package com.appenheimer.dailyflow.data

class SpotifyAuthManager {
    val configured: Boolean = false

    fun connectionMessage(): String {
        return "Spotify and music linking are planned for a future version. That step will need a public client ID and redirect URI, but no secrets should be hardcoded in the app."
    }
}
