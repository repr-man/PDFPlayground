package com.example.pdfplayground

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Switch
import androidx.appcompat.app.AlertDialog

// The activity that takes care of settings and other odds and ends.
class SettingsActivity : AppCompatActivity() {
    private lateinit var leftHandedSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        leftHandedSwitch = findViewById<Switch>(R.id.leftHandedSwitch).apply {
            setOnClickListener {
                Ctx.rightHanded = !isChecked
//                leftHandedSwitch.toggle()
            }
        }
        if(!Ctx.rightHanded)
            leftHandedSwitch.toggle()
    }

    // Sets the left handed mode switch to the correct state.
    fun onLeftHandedClicked(view: View) {
        leftHandedSwitch.performClick()
    }

    // Makes the dialog box for deleting bookmarks.
    fun onClearBookmarksClicked(view: View) {
        AlertDialog.Builder(this)
            .setTitle("Clear Bookmarks")
            .setMessage("Are you sure you want to clear all your bookmarks?")
            .setPositiveButton("Absolutely") { _, _ ->
                filesDir.listFiles()?.forEach {
                    it.delete()
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("This File") { _, _ ->
                Ctx.currentBookmarkFile?.delete()
            }
            .show()
    }

    // Cleans up.
    override fun onDestroy() {
        Ctx.currentBookmarkFile = null
        super.onDestroy()
    }
}