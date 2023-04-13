package com.example.pdfplayground

import android.app.AlertDialog
import android.os.Build
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File

// The second coolest part of the application.  `PageLoader`'s sidekick.
// It manages all the bookmarks.
class BookmarkListAdapter(
    private val bookmarkFile: File,
    private val activity: ViewerActivity
) : RecyclerView.Adapter<BookmarkListAdapter.ViewHolder>() {
    private val bookmarks = arrayListOf<Bookmark>()

    // Reads all the bookmarks from the bookmark file.  They are stored in plain text as colon
    // separated values for simplicity.  Their form is "$page:$title\n", where the title can be
    // omitted.
    init {
        bookmarkFile.readLines().mapTo(bookmarks) { s ->
            val colonIdx = s.indexOf(":")
            Bookmark(
                if(colonIdx + 1 == s.length)
                    null
                else
                    s.substring(colonIdx + 1),
                s.substring(0 until colonIdx).toInt(),
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.bookmark_list_item, parent, false),
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bookmarkItem.apply {
            // If a bookmark doesn't have a title, one is generated for it of the form "Page $page".
            text = bookmarks[position].name ?: "Page ${bookmarks[position].page}"
            setOnClickListener { activity.onBookmarkCardClicked(it) }

            // Sets up the bookmark editor dialog.
            setOnLongClickListener {
                val layout =
                    LayoutInflater.from(activity).inflate(R.layout.bookmark_editor, null)
                val pageEntryBox = layout.findViewById<EditText>(R.id.pageBox).apply {
                    setText(bookmarks[position].page.toString())
                }
                val nameEntryBox = layout.findViewById<EditText>(R.id.nameBox).apply {
                    setText(bookmarks[position].name)
                }
                AlertDialog.Builder(activity)
                    .setTitle("Edit Bookmark")
                    .setView(layout)
                    .setPositiveButton("Ok") { _, _ ->
                        val pageInput = pageEntryBox.text.toString()
                        val nameInput = nameEntryBox.text.toString()
                        if(pageInput.isNotEmpty()) {
                            if(pageInput.toInt() !in 1..activity.numPages()) {
                                AlertDialog.Builder(activity)
                                    .setTitle("Error")
                                    .setMessage("The page number you entered is not one of the pages in the document (1 - ${activity.numPages()}).  This bookmark will not work correctly!")
                                    .setPositiveButton("Ok", null)
                                    .show()
                            } else {
                                remove(position)
                                add(pageInput.toInt(), nameInput.ifEmpty { null })
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

                return@setOnLongClickListener true
            }

        }
        holder.removeButton.setOnClickListener {
            remove(activity.getBookmarkPositionForViewId(holder.itemView))
        }
    }

    override fun getItemCount() = bookmarks.size

    // Returns the page a specified bookmark refers to.
    fun getPage(bookmarkIdx: Int) = bookmarks[bookmarkIdx].page

    // Adds a new bookmark.
    fun add(page: Int, name: String? = null) {
        val newBookmark = Bookmark(name, page)
        var potentialInsertionIdx = bookmarks.indexOfFirst { it >= newBookmark }

        if(potentialInsertionIdx == -1)                 // Have to check this one first to avoid
            potentialInsertionIdx = bookmarks.size      // out of bounds errors.
        else if(bookmarks[potentialInsertionIdx] == newBookmark)     // Ignore duplicates.
            return

        bookmarks.add(potentialInsertionIdx, newBookmark)
        writeBookmarksToFile()
        notifyItemInserted(potentialInsertionIdx)
    }

    // Removes the specified bookmark and update the bookmark file.  Also, due to the
    // `notifyItemRemoved` function, there's a cool little list removal animation added
    // automatically.  In case you can't tell, I love everything I don't have to implement myself.
    private fun remove(idx: Int) {
        bookmarks.removeAt(idx)
        notifyItemRemoved(idx)
        writeBookmarksToFile()
    }

    // Collects the bookmarks into a string and spits it into a file.
    private fun writeBookmarksToFile() {
        // That's the most sexy concatenation I've ever written.
        bookmarkFile.writeText(bookmarks.fold("") { s, bookmark ->
            s.plus(bookmark)
        })
    }

    // Holds the title text and remove button.
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookmarkItem: TextView = itemView.findViewById(R.id.bookmarkItem)
        val removeButton: ImageButton = itemView.findViewById(R.id.removeBookmarkButton)
    }

    // Contains the data needed for a bookmark.
    private class Bookmark(
        val name: String?,
        val page: Int,
    ) : Comparable<Bookmark> {
        // Implemented so the bookmarks can be ordered in their list automatically.
        override fun compareTo(other: Bookmark) = this.page compareTo other.page

        // Needed for more comparison stuff.
        override fun equals(other: Any?): Boolean {
            if(other is Bookmark) {
                return this.page == other.page
            }
            return false
        }

        // An auto-generated function that probably make comparing the bookmarks faster.
        override fun hashCode(): Int {
            var result = name?.hashCode() ?: 0
            result = 31 * result + page
            return result
        }

        // Creates the string used to represent the bookmarks in internal storage.
        override fun toString() = "$page:${name ?: ""}\n"
    }
}