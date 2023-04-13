package com.example.pdfplayground

import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.floatingactionbutton.FloatingActionButton

// The activity for annotating pages of the PDF.
@RequiresApi(Build.VERSION_CODES.Q)
class EditorActivity : AppCompatActivity() {
    private lateinit var editorLayout: ConstraintLayout
    private lateinit var editor: EditorView
    private lateinit var toolbar: Toolbar
    private lateinit var trayButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        // Sets up the toolbar.
        editorLayout = findViewById(R.id.editorLayout)
        toolbar = findViewById(R.id.editorToolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        editor = findViewById<EditorView>(R.id.editorView).apply {
            bindBitmap(Ctx.bitmapHolder!!)
        }

        // Puts the tray on the correct side of the screen.
        trayButton = findViewById<FloatingActionButton>(R.id.toolTray)
        ConstraintSet().apply {
            clone(editorLayout)
            if(Ctx.rightHanded) {
                connect(R.id.toolTray, ConstraintSet.RIGHT, editorLayout.id, ConstraintSet.RIGHT, 16)
            } else {
                connect(R.id.toolTray, ConstraintSet.LEFT, editorLayout.id, ConstraintSet.LEFT, 16)
            }
            applyTo(editorLayout)
        }
    }

    // Inflates and creates the toolbar menus.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        makePenMenus()
        return true
    }

    // Called when you select the highlighter tool from the tool menu.
    fun onHighlighterToolClicked(item: MenuItem) {
        Ed.dragMode = Ed.DragMode.Highlighter
        Ed.trayMode = Ed.DragMode.Move
        trayButton.setImageDrawable(getDrawable(R.drawable.pan_icon))
        Ed.drawPaint.apply {
            this.color = Color.YELLOW
            alpha = 127
        }
        toolbar.menu.findItem(R.id.toolsButton).setIcon(R.drawable.highlighter_tool_icon)
        toolbar.menu.findItem(R.id.paletteButton).subMenu!!.apply {
            this.clear()
            makeColorMenu(
                this,
                intArrayOf(Color.MAGENTA, Color.CYAN, Color.YELLOW),
                intArrayOf(127, 127, 127),
                arrayOf("Magenta", "Cyan", "Yellow")
            )
        }
    }

    // Called when you select the pen tool from the tool menu.
    fun onPenToolClicked(item: MenuItem) {
        makePenMenus()
    }

    // Sets up the pen ui and populates the color menu with the available options.
    // This function is separated out so it can be called from `onCreateOptionsMenu`.
    private fun makePenMenus(){
        Ed.dragMode = Ed.DragMode.Pen
        Ed.trayMode = Ed.DragMode.Move
        Ed.lineWeight = Ed.LineWeight.Light
        trayButton.setImageDrawable(getDrawable(R.drawable.pan_icon))
        Ed.drawPaint.apply {
            this.color = Color.BLACK
            alpha = 255
        }
        toolbar.menu.findItem(R.id.toolsButton).setIcon(R.drawable.edit_icon)
        toolbar.menu.findItem(R.id.paletteButton).subMenu!!.apply {
            this.clear()
            makeColorMenu(
                this,
                intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK),
                intArrayOf(255, 255, 255, 255, 255),
                arrayOf("Red", "Green", "Blue", "White", "Black")
            )
        }
    }

    // Called when you select the line tool from the tool menu.
    fun onLineToolClicked(item: MenuItem) {
        Ed.dragMode = Ed.DragMode.Line
        Ed.trayMode = Ed.DragMode.Move
        trayButton.setImageDrawable(getDrawable(R.drawable.pan_icon))
        Ed.drawPaint.apply {
            this.color = Color.BLACK
            alpha = 255
        }
        toolbar.menu.findItem(R.id.toolsButton).setIcon(R.drawable.line_tool_icon)
        toolbar.menu.findItem(R.id.paletteButton).subMenu!!.apply {
            this.clear()
            makeColorMenu(
                this,
                intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK),
                intArrayOf(255, 255, 255, 255, 255),
                arrayOf("Red", "Green", "Blue", "White", "Black")
            )
        }
    }

    // Populates a menu with colors.
    private fun makeColorMenu(
        menu: SubMenu,
        colors: IntArray,
        alphas: IntArray,
        labels: Array<String>,
    ) {
        colors.forEachIndexed { i, color ->
            menu.add(labels[i])!!.apply {
                setIcon(R.drawable.color_preview_icon)
                icon?.setTint(color)
                setOnMenuItemClickListener {
                    Ed.drawPaint.apply {
                        this.color = color
                        alpha = alphas[i]
                    }
                    true
                }
            }
        }
    }

    // Makes the line weight heavy.
    fun onHeavyLineWeightClicked(item: MenuItem) {
        Ed.lineWeight = Ed.LineWeight.Heavy
        toolbar.menu.findItem(R.id.lineWeightButton).apply {
            setIcon(R.drawable.large_weight_icon)
            Ed.drawPaint.strokeWidth = 24f
        }
    }
    // Makes the line a nice, moderate weight.  You could get in about 15 reps with it.
    fun onMediumLineWeightClicked(item: MenuItem) {
        Ed.lineWeight = Ed.LineWeight.Medium
        toolbar.menu.findItem(R.id.lineWeightButton).apply {
            setIcon(R.drawable.medium_weight_icon)
            Ed.drawPaint.strokeWidth = 16f
        }
    }
    // Makes the line weight light.
    fun onLightLineWeightClicked(item: MenuItem) {
        Ed.lineWeight = Ed.LineWeight.Light
        toolbar.menu.findItem(R.id.lineWeightButton).apply {
            setIcon(R.drawable.small_weight_icon)
            Ed.drawPaint.strokeWidth = 8f
        }
    }

    // Tells the editor to undo the most recent line drawn.
    fun onUndoClicked(view: View) {
        editor.undo()
    }

    // Sets the appropriate drawing mode and updates the tray icon accordingly.
    fun onTrayClicked(view: View) {
        with(Ed) {
            when(trayMode) {
                Ed.DragMode.Move -> {
                    trayMode = dragMode
                    dragMode = Ed.DragMode.Move
                    trayButton.setImageDrawable(when(trayMode) {
                        Ed.DragMode.Line -> getDrawable(R.drawable.line_tool_icon)
                        Ed.DragMode.Pen -> getDrawable(R.drawable.edit_icon)
                        Ed.DragMode.Highlighter -> getDrawable(R.drawable.highlighter_tool_icon)
                        else -> { TODO("Unreachable") }
                    })
                }
                else -> {
                    dragMode = trayMode
                    trayMode = Ed.DragMode.Move
                    trayButton.setImageDrawable(getDrawable(R.drawable.pan_icon))
                }
            }
        }
    }

    fun onSaveClicked(view: View) {
        editor.save(this)
    }
}