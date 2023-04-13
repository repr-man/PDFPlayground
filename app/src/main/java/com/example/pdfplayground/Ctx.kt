package com.example.pdfplayground

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.File

// The global context object for sharing state between activities.
object Ctx {
    // A switch which tells how certain ui elements should be drawn.
    var rightHanded = true

    // Used for passing bitmaps and the bookmark file, respectively, between views and activities.
    var bitmapHolder: Bitmap? = null
    var currentBookmarkFile: File? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    fun showSaveDialog(activity: AppCompatActivity, context: Context, bitmapToSave: Bitmap) {
        // Sets up the file name entry dialog box.
        val layout =
            LayoutInflater.from(activity).inflate(R.layout.filename_editor, null, false)
        val pageEntryBox = layout.findViewById<EditText>(R.id.filenameBox)
        AlertDialog.Builder(context)
            .setTitle("Enter a file name")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok") { _, _ ->
                val filenameString: String? = pageEntryBox.text.toString().ifEmpty { null }

                // Validates the name and makes an error dialog if invalid.
                if(filenameString.isNullOrBlank() || fileExists(filenameString)) {
                    AlertDialog.Builder(context)
                        .setTitle("Warning")
                        .setMessage("Invalid file name.  Please try again.")
                        .setPositiveButton("Ok", null)
                        .show()
                    return@setPositiveButton
                }

                // More `MediaStore` weirdness.  Saves the bitmap to the image directory.
                val resolver = context.contentResolver!!
                val uri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "${filenameString}.jpg")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    })!!
                resolver.openOutputStream(uri).use {
                    bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
            }
            .show()
    }
    // Checks if the text entered in the file name entry dialog with ".jpg" appended exists.
    private fun fileExists(s: String?) =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "$s.jpg").exists()
}