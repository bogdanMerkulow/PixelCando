package pixel.cando.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pixel.cando.R
import pixel.cando.databinding.ActivityRootBinding
import pixel.cando.ui._base.fragment.OnBackPressedListener

class RootActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Pixelcando)
        super.onCreate(savedInstanceState)
        val binding = ActivityRootBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(
                    binding.container.id,
                    RootFragment()
                )
                .commit()
        }

    }

    override fun onBackPressed() {
        val backPressedListener = supportFragmentManager.fragments.lastOrNull {
            it is OnBackPressedListener
        } as? OnBackPressedListener

        if (backPressedListener != null) {
            backPressedListener.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }

}