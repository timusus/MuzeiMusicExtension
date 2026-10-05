package com.simplecity.muzei.music.activity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.simplecity.muzei.music.R

class SetupActivity : AppCompatActivity() {

    private var hasSeenDialog = false

    private var isFirstPresentation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val notificationListenerEnabled = notificationListenerEnabled()

        if (notificationListenerEnabled) {
            setResultAndFinish()
            return
        }

        setContentView(R.layout.activity_setup)

        val root: View = findViewById(R.id.setupRoot)
        val padding = root.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val settingsButton: Button = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            openNotificationListenerSettings()
        }
    }

    override fun onResume() {
        super.onResume()

        if (notificationListenerEnabled()) {
            setResultAndFinish()
            return
        }

        if (!isFirstPresentation) {
            if (hasSeenDialog) {
                setResultAndFinish()
            } else {
                showDialog()
            }
        }

        isFirstPresentation = false
    }

    private fun notificationListenerEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(applicationContext).contains(applicationContext.packageName)
    }

    companion object {
        private const val TAG = "SetupActivity"
    }

    private fun openNotificationListenerSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Notification listener settings not available")
        }
    }

    private fun showDialog() {
        hasSeenDialog = true

        AlertDialog.Builder(this)
                .setTitle(getString(R.string.notificationSettingsDialogTitle))
                .setMessage(getString(R.string.notificationSettingsDialogMessage))
                .setPositiveButton(R.string.settingsButton) { _, _ ->
                    openNotificationListenerSettings()
                }
                .setNegativeButton(getString(R.string.closeButton)) { _, _ -> setResultAndFinish() }
                .show()
    }

    private fun setResultAndFinish() {
        setResult(if (!notificationListenerEnabled()) Activity.RESULT_CANCELED else Activity.RESULT_OK)
        finish()
    }
}