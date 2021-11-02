package pixel.cando.utils

import com.onesignal.OSSubscriptionObserver
import com.onesignal.OneSignal
import kotlinx.coroutines.withTimeoutOrNull
import pixel.cando.data.remote.RemoteRepository
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface PushNotificationsSubscriber {

    suspend fun subscribe(): Either<Unit, Throwable>

}

class OneSignalPushNotificationsSubscriber(
    private val remoteRepository: RemoteRepository,
) : PushNotificationsSubscriber {

    override suspend fun subscribe(
    ): Either<Unit, Throwable> {

        val userId = OneSignal.getDeviceState()?.userId
            ?: withTimeoutOrNull(
                timeMillis = 10_000L
            ) {
                suspendCoroutine { continuation ->
                    var observer: OSSubscriptionObserver? = null
                    observer = OSSubscriptionObserver { change ->
                        change.to.userId?.let { userId ->
                            observer?.let {
                                OneSignal.removeSubscriptionObserver(it)
                            }
                            continuation.resume(userId)
                        }
                    }
                    OneSignal.addSubscriptionObserver(observer)
                }
            }
        return if (userId != null) {
            remoteRepository.subscribeForPushNotifications(
                identifier = userId
            )
        } else Either.Left(Unit)
    }
}