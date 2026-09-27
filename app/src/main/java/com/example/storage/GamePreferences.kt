package com.example.storage

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("domino_rami_prefs", Context.MODE_PRIVATE)

    var playerId: String
        get() {
            var id = prefs.getString("player_id", null)
            if (id == null) {
                id = "player_" + UUID.randomUUID().toString().substring(0, 8)
                prefs.edit().putString("player_id", id).apply()
            }
            return id
        }
        set(value) = prefs.edit().putString("player_id", value).apply()

    var playerName: String
        get() = prefs.getString("player_name", "رامي") ?: "رامي"
        set(value) = prefs.edit().putString("player_name", value).apply()

    var isSfxEnabled: Boolean
        get() = prefs.getBoolean("sfx_enabled", true)
        set(value) = prefs.edit().putBoolean("sfx_enabled", value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

    var targetScore: Int
        get() = prefs.getInt("target_score", 100)
        set(value) = prefs.edit().putInt("target_score", value).apply()

    var hasCompletedTutorial: Boolean
        get() = prefs.getBoolean("tutorial_completed", false)
        set(value) = prefs.edit().putBoolean("tutorial_completed", value).apply()
}
