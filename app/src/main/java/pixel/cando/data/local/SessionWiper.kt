package pixel.cando.data.local

interface SessionWiper {
    fun wipe()
}

class RealSessionWiper(
    private val accessTokenStore: AccessTokenStore,
    private val userRoleStore: UserRoleStore
) : SessionWiper {

    override fun wipe() {
        accessTokenStore.accessToken = null
        userRoleStore.userRole = null
    }
}