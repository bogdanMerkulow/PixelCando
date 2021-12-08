package pixel.cando.di

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import pixel.cando.data.local.AccessTokenStore
import pixel.cando.data.local.AuthStateChecker
import pixel.cando.data.local.LoggedInUserIdStore
import pixel.cando.data.local.RealAccessTokenStore
import pixel.cando.data.local.RealAuthStateChecker
import pixel.cando.data.local.RealLoggedInUserIdStore
import pixel.cando.data.local.RealSessionWiper
import pixel.cando.data.local.RealUserRoleStore
import pixel.cando.data.local.SessionWiper
import pixel.cando.data.local.UserRoleStore
import pixel.cando.data.remote.AuthRepository
import pixel.cando.data.remote.RealAuthRepository
import pixel.cando.data.remote.RealRemoteRepository
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.DelegatingFragment
import pixel.cando.ui._base.fragment.FragmentDelegate
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.fragment.findImplementationOrThrow
import pixel.cando.ui.auth.password_recovery.PasswordRecoveryFragment
import pixel.cando.ui.auth.sign_in.SignInFragment
import pixel.cando.ui.createUnauthorizedResultEventSource
import pixel.cando.ui.main.chat_list.ChatListFragment
import pixel.cando.ui.main.chat_messaging.ChatMessagingFragment
import pixel.cando.ui.main.chat_with_doctor.ChatWithDoctorFragment
import pixel.cando.ui.main.chat_with_patient.ChatWithPatientFragment
import pixel.cando.ui.main.exam_details.ExamDetailsFragment
import pixel.cando.ui.main.home.HomeFragment
import pixel.cando.ui.main.patient_details.PatientDetailsFragment
import pixel.cando.ui.main.patient_info.PatientInfoFragment
import pixel.cando.ui.main.patient_list.PatientListFragment
import pixel.cando.ui.main.photo_list.PhotoListFragment
import pixel.cando.ui.main.photo_preview.PhotoPreviewFragment
import pixel.cando.ui.main.profile.ProfileFragment
import pixel.cando.ui.root.RootEvent
import pixel.cando.ui.root.RootFragment
import pixel.cando.utils.OneSignalPushNotificationsSubscriber
import pixel.cando.utils.PushNotificationsSubscriber
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

    private val loggedInUserIdStore: LoggedInUserIdStore by lazy {
        RealLoggedInUserIdStore(
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
            context = app,
        )
    }

    private val authApi by lazy {
        assembleAuthApi(
            resourceProvider = resourceProvider,
            moshi = moshi,
            context = app,
        )
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

    private val authRepository: AuthRepository by lazy {
        RealAuthRepository(
            authApi = authApi,
            moshi = moshi,
        )
    }

    private val pushNotificationsSubscriber: PushNotificationsSubscriber by lazy {
        OneSignalPushNotificationsSubscriber(
            remoteRepository = remoteRepository,
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
                                    fragment.setup(
                                        rootRouter = requireRootRouter(),
                                        flowRouter = fragment.findImplementationOrThrow(),
                                        authRepository = authRepository,
                                        accessTokenStore = accessTokenStore,
                                        userRoleStore = userRoleStore,
                                        loggedInUserIdStore = loggedInUserIdStore,
                                        pushNotificationsSubscriber = pushNotificationsSubscriber,
                                        resourceProvider = resourceProvider,
                                        context = context,
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
                                    fragment.setup(
                                        userRoleStore = userRoleStore,
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
                                    fragment.setup(
                                        remoteRepository = remoteRepository,
                                        resourceProvider = resourceProvider,
                                        context = app,
                                        flowRouter = fragment.findImplementationOrThrow(),
                                    )
                                }
                                is PatientInfoFragment -> {
                                    fragment.setup(
                                        resourceProvider = resourceProvider,
                                        remoteRepository = remoteRepository,
                                        flowRouter = fragment.findImplementationOrThrow(),
                                    )
                                }
                                is ExamDetailsFragment -> {
                                    fragment.setup(
                                        resourceProvider = resourceProvider,
                                        remoteRepository = remoteRepository,
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
                                is PhotoPreviewFragment -> {
                                    fragment.setup()
                                }
                                is ProfileFragment -> {
                                    fragment.setup(
                                        sessionWiper = sessionWiper,
                                        rootRouter = fragment.findImplementationOrThrow(),
                                        resourceProvider = resourceProvider,
                                        remoteRepository = remoteRepository,
                                    )
                                }
                                is ChatListFragment -> {
                                    fragment.setup(
                                        remoteRepository = remoteRepository,
                                        resourceProvider = resourceProvider,
                                        loggedInUserIdProvider = loggedInUserIdStore,
                                        flowRouter = fragment.findImplementationOrThrow(),
                                    )
                                }
                                is ChatMessagingFragment -> {
                                    fragment.setup(
                                        remoteRepository = remoteRepository,
                                        resourceProvider = resourceProvider,
                                        loggedInUserIdProvider = loggedInUserIdStore,
                                    )
                                }
                                is ChatWithPatientFragment -> {
                                    fragment.setup(
                                        remoteRepository = remoteRepository,
                                        flowRouter = fragment.findImplementationOrThrow(),
                                    )
                                }
                                is ChatWithDoctorFragment -> {
                                    fragment.setup()
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

inline fun <reified D : FragmentDelegate> Fragment.findDelegateOrThrow(): D {
    return findDelegate(D::class.java)
        ?: throw IllegalStateException("Delegate with class ${D::class.java.name} was not found")
}

fun <D : FragmentDelegate> Fragment.findDelegate(klass: Class<D>): D? {
    if (this is DelegatingFragment) {
        val delegate = delegates.firstOrNull {
            klass.isInstance(it)
        } as? D
        if (delegate != null) {
            return delegate
        }
    }
    return parentFragment?.findDelegate(klass)
}