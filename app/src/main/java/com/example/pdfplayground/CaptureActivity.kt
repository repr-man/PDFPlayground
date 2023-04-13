package com.example.pdfplayground

import android.annotation.SuppressLint
import android.graphics.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlin.math.*

// The activity for capturing cropped images from the PDF.
@RequiresApi(Build.VERSION_CODES.Q)
@SuppressLint("ClickableViewAccessibility")
class CaptureActivity : AppCompatActivity() {
    private lateinit var captureView: CaptureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        captureView = findViewById(R.id.capture)
        captureView.setup()

        // Puts the capture button on the correct side of the screen.
        val captureLayout = findViewById<ConstraintLayout>(R.id.captureLayout)
        ConstraintSet().apply {
            clone(captureLayout)
            if(Ctx.rightHanded)
                connect(R.id.captureButton, ConstraintSet.RIGHT, captureLayout.id, ConstraintSet.RIGHT, 16)
            else
                connect(R.id.captureButton, ConstraintSet.LEFT, captureLayout.id, ConstraintSet.LEFT, 16)
            applyTo(captureLayout)
        }
    }

    fun onCaptureClicked(view: View) {
        captureView.captureArea(this)
    }
}