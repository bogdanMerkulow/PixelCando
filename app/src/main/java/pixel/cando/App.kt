package pixel.cando

import android.app.Application
import com.elvishew.xlog.XLog
import pixel.cando.di.DependencyManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        XLog.init()
        DependencyManager(this)

    }

}