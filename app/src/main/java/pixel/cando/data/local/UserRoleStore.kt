package pixel.cando.data.local

import android.content.SharedPreferences
import pixel.cando.ui._models.UserRole

interface UserRoleStore {
    var userRole: UserRole?
}

class RealUserRoleStore(
    private val sharedPreferences: SharedPreferences
) : UserRoleStore {

    private val userRoleKey = "KEY_USER_ROLE"

    override var userRole: UserRole?
        get() = sharedPreferences.getString(
            userRoleKey,
            null
        )?.let {
            getUserRoleFromStoreValue(it)
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(
                        userRoleKey,
                        value.storeValue
                    )
                    .apply()
            } else {
                sharedPreferences.edit()
                    .remove(userRoleKey)
                    .apply()
            }
        }


    private val UserRole.storeValue: String
        get() = when (this) {
            UserRole.DOCTOR -> "5u6hsarxfu"
            UserRole.PATIENT -> "E7Bj8bByOR"
        }

    private fun getUserRoleFromStoreValue(
        storeValue: String
    ) = UserRole.values().firstOrNull {
        it.storeValue == storeValue
    }

}