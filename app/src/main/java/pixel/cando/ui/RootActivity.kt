package pixel.cando.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pixel.cando.R

class RootActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)
    }
}