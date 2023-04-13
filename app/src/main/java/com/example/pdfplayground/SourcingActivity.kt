package com.example.pdfplayground

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*

// The activity that the user lands on when they launch the application.
@RequiresApi(Build.VERSION_CODES.Q)
class SourcingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sourcing)
    }

    // Opens the system file picker, showing only pdf files.
    fun onSourceButtonClicked(view: View) {
        getPDFContent.launch(arrayOf("application/pdf"))
    }

    // Starts the viewer activity with the uri of the file it should display.
    private val getPDFContent = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        startActivity(Intent(this, ViewerActivity::class.java)
            .putExtra("pdfFile", it!!))
    }
}