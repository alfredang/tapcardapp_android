package com.tertiaryinfotech.tapcard.net

import android.content.Context

/** Persists the signed-in user's bearer token and identity in SharedPreferences. */
class AuthStore(context: Context) {

    private val prefs = context.getSharedPreferences("TapcardAuth", Context.MODE_PRIVATE)

    val token: String? get() = prefs.getString("token", null)
    val userName: String? get() = prefs.getString("name", null)
    val userEmail: String? get() = prefs.getString("email", null)

    fun save(token: String, name: String?, email: String?) {
        prefs.edit()
            .putString("token", token)
            .putString("name", name)
            .putString("email", email)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()
}