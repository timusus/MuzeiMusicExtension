package com.simplecity.muzei.music.activity

import agency.tango.materialintroscreen.MaterialIntroActivity
import agency.tango.materialintroscreen.MessageButtonBehaviour
import agency.tango.materialintroscreen.SlideFragmentBuilder
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import com.simplecity.muzei.music.MusicExtensionApplication
import com.simplecity.muzei.music.R

class SetupActivity : MaterialIntroActivity() {

    private var hasSeenDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideBackButton()

        addSlide(SlideFragmentBuilder()
                .title(getString(R.string.slideOneTitle))
                .description(getString(R.string.slideOneDescription))
                .backgroundColor(R.color.slide_one)
                .buttonsColor(R.color.button_one)
                .neededPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                .build())

        addSlide(SlideFragmentBuilder()
                .title(getString(R.string.slideTwoTitle))
                .description(getString(R.string.slideTwoDescription))
                .backgroundColor(R.color.slide_two)
                .buttonsColor(R.color.button_two)
                .build(), MessageButtonBehaviour({
            openNotificationListenerSettings()
        }, getString(R.string.settingsButton)))
    }

    override fun onResume() {
        super.onResume()

        if (notificationListenerEnabled()) {
            setResultAndFinish()
        }
    }

    private fun notificationListenerEnabled() = (applicationContext as MusicExtensionApplication).notificationsEnabled

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
                .setNegativeButton(getString(R.string.closeButton), { _, _ -> setResultAndFinish() })
                .show()
    }

    override fun performFinish() {
        if (!notificationListenerEnabled() && !hasSeenDialog) {
            showDialog()
        } else {
            setResultAndFinish()
        }
    }

    private fun setResultAndFinish() {
        setResult(Activity.RESULT_OK)
        finish()
    }

}