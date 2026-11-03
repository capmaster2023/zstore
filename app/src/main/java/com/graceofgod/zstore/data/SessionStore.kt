package com.graceofgod.zstore.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Garde le token et l'utilisateur connecté. */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("zstore_session", Context.MODE_PRIVATE)

    private val _user = MutableStateFlow(readUser())
    val user: StateFlow<User?> = _user.asStateFlow()

    val token: String? get() = prefs.getString("token", null)

    fun save(auth: AuthResponse) {
        prefs.edit()
            .putString("token", auth.token)
            .putString("user", json.encodeToString(User.serializer(), auth.user))
            .apply()
        _user.value = auth.user
    }

    fun updateUser(user: User) {
        prefs.edit().putString("user", json.encodeToString(User.serializer(), user)).apply()
        _user.value = user
    }

    fun clear() {
        prefs.edit().clear().apply()
        _user.value = null
    }

    private fun readUser(): User? {
        if (prefs.getString("token", null) == null) return null
        val raw = prefs.getString("user", null) ?: return null
        return runCatching { json.decodeFromString(User.serializer(), raw) }.getOrNull()
    }
}
