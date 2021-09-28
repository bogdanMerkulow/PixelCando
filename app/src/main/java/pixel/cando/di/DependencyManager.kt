package pixel.cando.di

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import pixel.cando.data.local.*
import pixel.cando.data.remote.AuthRepository
import pixel.cando.data.remote.RealAuthRepository
import pixel.cando.data.remote.RealRemoteRepository
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.fragment.findImplementationOrThrow
import pixel.cando.ui.auth.password_recovery.PasswordRecoveryFragment
import pixel.cando.ui.auth.sign_in.SignInFragment
import pixel.cando.ui.createUnauthorizedResultEventSource
import pixel.cando.ui.main.home.HomeFragment
import pixel.cando.ui.main.patient_details.PatientDetailsFragment
import pixel.cando.ui.main.patient_list.PatientListFragment
import pixel.cando.ui.main.photo_list.PhotoListFragment
import pixel.cando.ui.root.RootEvent
import pixel.cando.ui.root.RootFragment
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

    private val moshi by lazy {
        assembleMoshi()
    }

    private val restApi by lazy {
        assembleRestApi(
            resourceProvider = resourceProvider,
            accessTokenProvider = accessTokenStore,
            moshi = moshi,
        )
    }

    private val authApi by lazy {
        assembleAuthApi(
            resourceProvider = resourceProvider,
            moshi = moshi,
        )
    }

    private val remoteRepository: RemoteRepository by lazy {
        RealRemoteRepository(
            restApi = restApi,
            unauthorizedHandler = {
                unauthorizedResultEventSource.emit(
                    RootEvent.UserAuthorizationGotInvalid
                )
            }
        )
    }

    private val authRepository: AuthRepository by lazy {
        RealAuthRepository(
            authApi = authApi,
            moshi = moshi,
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
                                        flowRouter = fragment.findImplementationOrThrow(),
                                        authRepository = authRepository,
                                        accessTokenStore = accessTokenStore,
                                        userRoleStore = userRoleStore,
                                    )
                                }
                                is PasswordRecoveryFragment -> {
                                    setup(
                                        fragment = fragment,
                                        authRepository = authRepository,
                                        resourceProvider = resourceProvider,
                                        flowRouter = fragment.findImplementationOrThrow(),
                                    )
                                }
                                is HomeFragment -> {
                                    setup(
                                        fragment = fragment,
                                        userRoleStore = userRoleStore,
                                        resourceProvider = resourceProvider,
                                    )
                                }
                                is PatientListFragment -> {
                                    setup(
                                        fragment = fragment,
                                        remoteRepository = remoteRepository,
                                        resourceProvider = resourceProvider,
                                        flowRouter = fragment.findImplementationOrThrow(),
                                    )
                                }
                                is PatientDetailsFragment -> {
                                    setup(
                                        fragment = fragment,
                                        remoteRepository = remoteRepository,
                                        resourceProvider = resourceProvider,
                                        context = app,
                                        flowRouter = fragment.findImplementationOrThrow(),
                                    )
                                }
                                is PhotoListFragment -> {
                                    setup(
                                        fragment = fragment,
                                        remoteRepository = remoteRepository,
                                        resourceProvider = resourceProvider,
                                        context = app,
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
