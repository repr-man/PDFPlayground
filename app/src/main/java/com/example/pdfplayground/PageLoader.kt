package com.example.pdfplayground

import android.app.AlertDialog
import android.content.*
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import java.io.Closeable
import java.io.FileInputStream

// The coolest part of the application.  Most likely will be the subject of several minutes of
// ranting in the demo video.
@Suppress("JoinDeclarationAndAssignment")
@RequiresApi(Build.VERSION_CODES.Q)
class PageLoader(context: Context, contentResolver: ContentResolver, intent: Intent) : Closeable {
    private val file: ParcelFileDescriptor
    val pdf: PdfRenderer
    private val bitmaps: Array<Bitmap>
    private val indices: Array<Int>
    private var insertIdx = 0

    // Number of pages in the pdf.
    val size: Int

    init {
        file = contentResolver.openFile(intent.getParcelableExtra("pdfFile")!!, "rw", null)!!
        pdf = PdfRenderer(file)
        size = pdf.pageCount
        val numPages: Int
        if(size == 0) {
            // A 0 page PDF messes up the logic, so we give the user a dialog box and kick them back
            // to the sourcing activity.
            AlertDialog.Builder(context)
                .setTitle("Unreadable PDF")
                .setMessage("The PDF you tried to open has 0 pages.  It is not able to be displayed.")
                .setPositiveButton("Ok") { _, _ ->
                    (context as ViewerActivity).finish()
                }
            numPages = 0
            indices = Array(0) { TODO("Unreachable") }
        }
        // Caps the size of the bitmap buffer at 5, but allows it to be smaller if needed.
        else if(size < 5) {
            numPages = pdf.pageCount
            indices = Array(size) { 0 }
        } else {
            numPages = 5
            indices = Array(5) { 0 }
        }
        bitmaps = Array(numPages) { idx ->
            getPage(idx).also {        // Updates the index array before returning.
                indices[idx] = idx
            }
        }
    }

    // Cleans up.
    override fun close() {
        pdf.close()
        file.close()
    }

    // Gets the requested bitmap from the cache, or loads it into the cache it not found.
    // Also, it's an operator function, so we can use indexing syntax on it!
    operator fun get(idx: Int): Bitmap {
        val i = indices.indexOf(idx)
        return if(i != -1) {
            bitmaps[i]
        } else {
            loadPage(idx)
        }
    }

    // Gets the specified page, puts it into the cache, and returns it.
    private fun loadPage(idx: Int): Bitmap {
        return getPage(idx).also {
            bitmaps[insertIdx] = it
            indices[insertIdx] = idx
            incIdx()
        }
    }

    // Loads the page from internal storage and renders it into a bitmap.
    private fun getPage(idx: Int): Bitmap {
        pdf.openPage(idx).use { page ->
            return Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888).also {
                page.render(it!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
    }

    // Handles the wraparound loading into the cache.
    private fun incIdx() {
        if(insertIdx < indices.size - 1)
            insertIdx += 1
        else
            insertIdx = 0
    }

    // When jumping to a page, this loads it and caches the pages next to it.
    fun loadPos(pos: Int) {
        if(pos in 0 until size) {
            loadPage(pos)
            if(pos - 2 in 0 until pos) {
                loadPage(pos - 2)
                loadPage(pos - 1)
            } else if(pos - 1 in 0 until pos) {
                loadPage(pos - 1)
            }
            if(pos + 2 in pos until size) {
                loadPage(pos + 2)
                loadPage(pos + 1)
            } else if(pos + 1 in pos until size) {
                loadPage(pos + 1)
            }
        }
    }
}