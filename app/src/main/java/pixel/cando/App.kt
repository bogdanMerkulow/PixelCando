package pixel.cando

import android.app.Application
import com.elvishew.xlog.XLog
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.onesignal.OneSignal
import pixel.cando.di.DependencyManager


class App : Application() {

    override fun onCreate() {
        super.onCreate()

        XLog.init()
        DependencyManager(this)

        OneSignal.initWithContext(this)
        OneSignal.setAppId("174511d2-c1a5-4fb8-808b-9688fd12f497")

        if (BuildConfig.DEBUG.not()) {
            AppCenter.start(
                this,
                "f84038f7-cc38-4bbb-a6f7-007147d429de",
                Analytics::class.java,
                Crashes::class.java
            )
        }

    }

}