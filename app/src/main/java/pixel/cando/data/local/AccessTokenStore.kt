package pixel.cando.data.local

import android.content.SharedPreferences

interface AccessTokenProvider {
    val accessToken: String?
}

interface AccessTokenStore : AccessTokenProvider {
    override var accessToken: String?
}

class RealAccessTokenStore(
    private val sharedPreferences: SharedPreferences
) : AccessTokenStore {

    private val accessTokenKey = "KEY_ACCESS_TOKEN"

    override var accessToken: String?
        get() = sharedPreferences.getString(
            accessTokenKey,
            null
        )
        set(value) {
            sharedPreferences.edit()
                .putString(
                    accessTokenKey,
                    value
                )
                .apply()
        }
}