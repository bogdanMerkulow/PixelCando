package pixel.cando

import android.app.Application
import com.elvishew.xlog.XLog
import com.onesignal.OneSignal
import pixel.cando.di.DependencyManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        XLog.init()
        DependencyManager(this)

        OneSignal.initWithContext(this)
        OneSignal.setAppId("174511d2-c1a5-4fb8-808b-9688fd12f497")

    }

}