package pixel.cando.data.local

import android.content.SharedPreferences

interface LoggedInUserIdProvider {
    val loggedInUserId: Long?
}

interface LoggedInUserIdStore : LoggedInUserIdProvider {
    override var loggedInUserId: Long?
}

class RealLoggedInUserIdStore(
    private val sharedPreferences: SharedPreferences
) : LoggedInUserIdStore {

    private val loggedInUserIdKey = "KEY_LOGGED_USER_ID"

    override var loggedInUserId: Long?
        get() = sharedPreferences.getLong(
            loggedInUserIdKey,
            -1
        ).takeIf { it > 0 }
        set(value) {
            if (value != null)
                sharedPreferences.edit()
                    .putLong(
                        loggedInUserIdKey,
                        value
                    )
                    .apply()
            else
                sharedPreferences.edit()
                    .remove(loggedInUserIdKey)
                    .apply()
        }
}