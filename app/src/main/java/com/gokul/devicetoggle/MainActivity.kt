package com.gokul.devicetoggle

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.UserManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private lateinit var audio: AudioManager

    private lateinit var swCamera: Switch
    private lateinit var swMic: Switch
    private lateinit var swSpeaker: Switch
    private lateinit var adminStatus: TextView
    private lateinit var btnAdmin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        admin = ComponentName(this, AdminReceiver::class.java)

        swCamera = findViewById(R.id.swCamera)
        swMic = findViewById(R.id.swMic)
        swSpeaker = findViewById(R.id.swSpeaker)
        adminStatus = findViewById(R.id.adminStatus)
        btnAdmin = findViewById(R.id.btnAdmin)

        btnAdmin.setOnClickListener { requestAdmin() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun isAdminActive() = dpm.isAdminActive(admin)

    private fun requestAdmin() {
        val i = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
        i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_explain))
        startActivity(i)
    }

    private fun refresh() {
        val active = isAdminActive()
        adminStatus.text = if (active) getString(R.string.admin_ok) else getString(R.string.admin_needed)
        btnAdmin.visibility = if (active) android.view.View.GONE else android.view.View.VISIBLE

        // Set states without firing listeners
        swCamera.setOnCheckedChangeListener(null)
        swMic.setOnCheckedChangeListener(null)
        swSpeaker.setOnCheckedChangeListener(null)

        swCamera.isChecked = if (active) !dpm.getCameraDisabled(admin) else true
        swMic.isChecked = !audio.isMicrophoneMute
        swSpeaker.isChecked = !audio.isStreamMute(AudioManager.STREAM_MUSIC)

        swCamera.setOnCheckedChangeListener { b, on -> onCamera(b, on) }
        swMic.setOnCheckedChangeListener { b, on -> onMic(b, on) }
        swSpeaker.setOnCheckedChangeListener { b, on -> onSpeaker(b, on) }
    }

    private fun onCamera(button: CompoundButton, enabled: Boolean) {
        if (!isAdminActive()) {
            button.setOnCheckedChangeListener(null)
            button.isChecked = true
            button.setOnCheckedChangeListener { b, on -> onCamera(b, on) }
            requestAdmin()
            return
        }
        try {
            dpm.setCameraDisabled(admin, !enabled)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun onMic(button: CompoundButton, enabled: Boolean) {
        try {
            audio.isMicrophoneMute = !enabled
            // Full block only works if this app is Device Owner (set via ADB)
            if (dpm.isDeviceOwnerApp(packageName)) {
                if (enabled) {
                    dpm.clearUserRestriction(admin, UserManager.DISALLOW_UNMUTE_MICROPHONE)
                } else {
                    dpm.addUserRestriction(admin, UserManager.DISALLOW_UNMUTE_MICROPHONE)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Mic error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun onSpeaker(button: CompoundButton, enabled: Boolean) {
        try {
            audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (enabled) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE,
                0
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Speaker error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
