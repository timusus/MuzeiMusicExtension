package com.simplecity.muzei.music.activity

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceFragmentCompat
import com.simplecity.muzei.music.R

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val KEY_PREF_WIFI_ONLY = "pref_key_wifi_only"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The theme is light, so ask for dark status and navigation bar icons on a transparent bar
        enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // The action bar handles the top inset; keep the preferences clear of the other bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(R.id.settingsContainer)) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, 0, bars.right, bars.bottom)
            windowInsets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                    .replace(R.id.settingsContainer, PrefsFragment())
                    .commit()
        }
    }

    class PrefsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
