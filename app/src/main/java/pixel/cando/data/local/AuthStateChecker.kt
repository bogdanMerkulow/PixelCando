package pixel.cando.data.local

interface AuthStateChecker {
    val isAuthorized: Boolean
}

class RealAuthStateChecker(
    private val accessTokenStore: AccessTokenStore,
    private val userRoleStore: UserRoleStore
) : AuthStateChecker {

    override val isAuthorized: Boolean
        get() = accessTokenStore.accessToken != null
                && userRoleStore.userRole != null

}