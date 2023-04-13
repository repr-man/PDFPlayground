@file:Suppress("RedundantSemicolon")

package com.example.pdfplayground

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.*
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.*
import java.io.Closeable
import java.io.File

// The main activity that displays all the pages in the PDF.
@Suppress("UNUSED_PARAMETER")
@RequiresApi(Build.VERSION_CODES.Q)
class ViewerActivity : AppCompatActivity(), Closeable {
    private lateinit var pdfPageView: RecyclerView;
    private lateinit var pageLoader: PageLoader;
    private lateinit var toolbar: Toolbar;
    private lateinit var currentPageSelector: EditText;
    private lateinit var viewerDrawer: DrawerLayout;
    private lateinit var bookmarkList: RecyclerView;

    private var scale = 1f;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        // Finds a whole bunch of views and sets up the toolbar.
        viewerDrawer = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.viewerToolbar);
        setSupportActionBar(toolbar);
        currentPageSelector = findViewById(R.id.currentPage);
        pdfPageView = findViewById(R.id.pdfPageView);

        // Sets up the all the paging machinery.
        pageLoader = PageLoader(this, contentResolver, intent);
        pdfPageView.apply {
            adapter = PDFPageListAdapter(pageLoader, currentPageSelector);
            layoutManager = LinearLayoutManager(this@ViewerActivity)
            addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy);
                    // I love this part!
                    // If there are no completely visible pages, it sets the page number to 0.  When
                    // that happens, we can use the first visible page.  This results in the page
                    // number being the one with the most screen real estate currently.
                    val attemptedPageNum = (recyclerView.layoutManager as LinearLayoutManager)
                        .findFirstCompletelyVisibleItemPosition() + 1
                    currentPageSelector.setText(
                        (if(attemptedPageNum != 0) attemptedPageNum
                            else (recyclerView.layoutManager as LinearLayoutManager)
                                .findFirstVisibleItemPosition() + 1)
                        .toString()
                    );
                }
            })
            addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
                // Resizes the pages appropriately so they fit entirely on the screen.
                override fun onChildViewAttachedToWindow(view: View) {
                    view.apply {
                        scaleX = scale;
                        scaleY = scale;
                    }
                }
                override fun onChildViewDetachedFromWindow(view: View) {}
            })
        }

        // Sets up the page indicator display.
        findViewById<TextView>(R.id.totalPages).text = "/${pageLoader.pdf.pageCount}";
        // Sets up the jump to page functionality.
        currentPageSelector.setOnEditorActionListener { textView, id, _ ->
            return@setOnEditorActionListener if(id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_ACTION_SEARCH) {
                this.currentFocus?.let {
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(it.windowToken, 0)
                }
                val pos = textView.text.toString().toInt() - 1;
                pageLoader.loadPos(pos)
                pdfPageView.scrollToPosition(pos)
                true
            } else false
        }
    }

    // Updates the bookmark list and adapter in case they were cleared from the settings activity.
    override fun onResume() {
        super.onResume()
        bookmarkList = findViewById<RecyclerView>(R.id.bookmarkList).apply {
            val fileName = "${intent.getParcelableExtra<Uri>("pdfFile")!!.lastPathSegment!!}.bookmarks"
            val bookmarkFile = File(filesDir, fileName)
            if(!bookmarkFile.exists())
                bookmarkFile.createNewFile()
            adapter = BookmarkListAdapter(bookmarkFile, this@ViewerActivity)
            layoutManager = LinearLayoutManager(this@ViewerActivity)
        }
    }

    // Tells the activity to clean itself up.  Because having destructors in your language is too
    // hard, I guess.   \_;)_/
    override fun onDestroy() {
        close()
        super.onDestroy()
    }

    // Sets up the menus.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.viewer_menu, menu);
        return true;
    }

    // If the bookmark drawer is open, it closes it.  Otherwise, it performs its usual function.
    override fun onBackPressed() {
        if(viewerDrawer.isDrawerOpen(GravityCompat.START))
            viewerDrawer.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }

    // Gets your bookmark file for potential use and launches the settings activity.
    fun onSettingsClicked(item: MenuItem) {
        Ctx.currentBookmarkFile = File(filesDir,
            "${intent.getParcelableExtra<Uri>("pdfFile")!!.lastPathSegment!!}.bookmarks")
        startActivity(Intent(this, SettingsActivity::class.java));
    }

    // Launches the capture activity with the current page.
    fun onCaptureClicked(item: MenuItem) {
        Ctx.bitmapHolder = Bitmap.createBitmap(
            pageLoader[currentPageSelector.text.toString().toInt() - 1]);
        startActivity(Intent(this, CaptureActivity::class.java))
    }

    // Launches the editor activity with the current page.
    fun onEditClicked(item: MenuItem) {
        Ctx.bitmapHolder = Bitmap.createBitmap(
            pageLoader[currentPageSelector.text.toString().toInt() - 1]);
        startActivity(Intent(this, EditorActivity::class.java))
    }

    // Opens the bookmark drawer.
    fun onBookmarksClicked(view: View) {
        viewerDrawer.openDrawer(GravityCompat.START)
    }

    // Adds a bookmark for the current page and updates the list.
    fun onAddBookmarkClicked(view: View) {
        (bookmarkList.adapter as BookmarkListAdapter).add(currentPageSelector.text.toString().toInt())
        bookmarkList.invalidate()
    }

    // Loads the page associated with the bookmark clicked and scrolls to it.
    fun onBookmarkCardClicked(view: View) {
        val pos = (bookmarkList.adapter as BookmarkListAdapter)
            .getPage(bookmarkList.findContainingViewHolder(view)!!.adapterPosition) - 1
        pageLoader.loadPos(pos)
        pdfPageView.scrollToPosition(pos)
        viewerDrawer.closeDrawer(GravityCompat.START)
    }

    // Used to get the position of the bookmark associated with a specific view so it can be removed
    // from the list.
    fun getBookmarkPositionForViewId(view: View) = bookmarkList.getChildAdapterPosition(view)

    fun numPages() = pageLoader.size

    // Cleans up.
    override fun close() {
        pageLoader.close()
    }
}