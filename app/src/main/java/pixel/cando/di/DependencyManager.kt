package pixel.cando.di

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import pixel.cando.data.local.*
import pixel.cando.data.remote.RealRemoteRepository
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui.RootEvent
import pixel.cando.ui.RootFragment
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui.auth.sign_in.SignInFragment
import pixel.cando.ui.createUnauthorizedResultEventSource
import pixel.cando.ui.main.home.HomeFragment
import pixel.cando.utils.RealResourceProvider
import pixel.cando.utils.ResourceProvider
import java.lang.ref.WeakReference


class DependencyManager(
    private val app: Application
) : Application.ActivityLifecycleCallbacks {

    private var rootRouter: WeakReference<RootRouter>? = null

    private fun requireRootRouter() =
        rootRouter?.get() ?: throw IllegalStateException("RootRouter was not set")

    private val sharedPreferences = app.getSharedPreferences(
        "Session",
        Context.MODE_PRIVATE
    )

    private val resourceProvider: ResourceProvider by lazy {
        RealResourceProvider(app)
    }

    private val accessTokenStore: AccessTokenStore by lazy {
        RealAccessTokenStore(
            sharedPreferences = sharedPreferences
        )
    }

    private val userRoleStore: UserRoleStore by lazy {
        RealUserRoleStore(
            sharedPreferences = sharedPreferences
        )
    }

    private val authStateChecker: AuthStateChecker by lazy {
        RealAuthStateChecker(
            accessTokenStore = accessTokenStore,
            userRoleStore = userRoleStore
        )
    }

    private val sessionWiper: SessionWiper by lazy {
        RealSessionWiper(
            accessTokenStore = accessTokenStore,
            userRoleStore = userRoleStore,
        )
    }

    private val restApi by lazy {
        assembleWebApi(
            resourceProvider = resourceProvider
        )
    }

    private val moshi by lazy {
        assembleMoshi()
    }

    private val remoteRepository: RemoteRepository by lazy {
        RealRemoteRepository(
            restApi = restApi,
            moshi = moshi,
            unauthorizedHandler = {
                unauthorizedResultEventSource.emit(
                    RootEvent.UserAuthorizationGotInvalid
                )
            }
        )
    }

    private val unauthorizedResultEventSource by lazy {
        createUnauthorizedResultEventSource()
    }

    init {
        app.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
        if (activity is FragmentActivity) {
            activity.supportFragmentManager
                .registerFragmentLifecycleCallbacks(
                    object : FragmentManager.FragmentLifecycleCallbacks() {
                        override fun onFragmentPreAttached(
                            fragmentManager: FragmentManager,
                            fragment: Fragment,
                            context: Context
                        ) {
                            when (fragment) {
                                is RootFragment -> {
                                    rootRouter = WeakReference(fragment)
                                    setup(
                                        fragment = fragment,
                                        rootRouter = requireRootRouter(),
                                        authStateChecker = authStateChecker,
                                        sessionWiper = sessionWiper,
                                        eventSources = arrayOf(
                                            unauthorizedResultEventSource
                                        )
                                    )
                                }
                                is SignInFragment -> {
                                    setup(
                                        fragment = fragment,
                                        rootRouter = requireRootRouter(),
                                        remoteRepository = remoteRepository,
                                        accessTokenStore = accessTokenStore,
                                        userRoleStore = userRoleStore,
                                    )
                                }
                                is HomeFragment -> {
                                    setup(
                                        fragment = fragment,
                                        userRoleStore = userRoleStore,
                                        resourceProvider = resourceProvider,
                                    )
                                }
                            }
                        }
                    },
                    true
                )
        }
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(
        p0: Activity,
        p1: Bundle
    ) {
    }

}
