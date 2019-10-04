package com.simplecity.muzei.music.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.simplecity.muzei.music.R

class SetupActivity : AppCompatActivity() {

    private var hasSeenDialog = false

    private var isFirstPresentation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notificationListenerEnabled = notificationListenerEnabled()

        if (notificationListenerEnabled) {
            setResultAndFinish()
            return
        }

        setContentView(R.layout.activity_setup)

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

    private fun openNotificationListenerSettings() {
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    private fun showDialog() {
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