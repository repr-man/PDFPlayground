package com.example.pdfplayground

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation

// A custom view that implements all the rendering and drawing functionality of the editor activity.
@RequiresApi(Build.VERSION_CODES.Q)
class EditorView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private lateinit var bmp: Bitmap
//    private var toolbarHeight = 0
    private var yBound = false
//    private var moveMode = MoveMode.None
    private var startPoint = PointF(0f, 0f)
    private var absolutePoint = PointF(0f, 0f)
    private var deltaPoint = PointF(0f, 0f)
    private var scale = 0f

    private var edits = ArrayDeque<Edits>()
    private var paints = ArrayDeque<Paint>()
    private var translations = ArrayDeque<PointF>()

    private val screenWidth = resources.displayMetrics.widthPixels.toFloat()
    private val screenHeight = resources.displayMetrics.heightPixels.toFloat()
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)

//    // Gets the height of the AppBar plus the height of the notification bar (24dp, according to
//    // Google guidelines).  Needed for offsetting the position of the points when drawing.
//    fun setToolbarHeight() {
//        val tv = TypedValue()
//        context.theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, tv, true)
//        toolbarHeight = 24 + TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
//    }

    // Makes a local copy of the page's bitmap and scales it appropriately and centers it.
    fun bindBitmap(bm: Bitmap) {
        bmp = bm
        scale = 1 / (bmp.width / screenWidth)
        if(bmp.height * scale > screenHeight) {
            scale = 1 / (bmp.height / screenHeight)
            yBound = true
        }

        if(yBound) {
            absolutePoint.set((width + bmp.width * scale) / 4f, 0f)
        } else {
            absolutePoint.set(0f, (height + bmp.height * scale) / 4f)
        }
    }

    // Draws the PDF page and edits using the Painter's Method.  I used `ArrayDeque`s to efficiently
    // push and pop edits, but also be able to iterate from the bottom to the top of the stack,
    // drawing as it goes.
    private fun drawToCanvas(canvas: Canvas, translationX: Float, translationY: Float) {
        canvas.withTranslation(translationX, translationY) {
            withScale(scale, scale, 0f, 0f) {
                canvas.drawBitmap(bmp, 0f, 0f, bitmapPaint)
            }
            edits.forEachIndexed { i, it ->
                when(it) {
                    is Freehand -> canvas.drawPath(it.path, paints[i])
                    is Line -> canvas.drawLine(it.x1, it.y1, it.x2, it.y2, paints[i])
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawToCanvas(canvas, absolutePoint.x, absolutePoint.y)
    }

    // Performs the correct action based on the tool mode.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(Ed.dragMode) {
            Ed.DragMode.Move -> {
//                if(moveMode != MoveMode.Scaling)
                doTranslation(event)
            }
            Ed.DragMode.Line -> {
                drawLine(event)
            }
            Ed.DragMode.Pen -> {
                drawFreehand(event)
            }
            Ed.DragMode.Highlighter -> {
                drawLine(event)
            }
        }

        return true
    }

    // Translates the canvas.
    private fun doTranslation(event: MotionEvent) {
//        if(moveMode == MoveMode.WasScaling) {
//            if(event.action == MotionEvent.ACTION_UP) {
//                moveMode = MoveMode.None
//            }
//        } else
        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                startPoint.set(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                deltaPoint.set(event.x - startPoint.x, event.y - startPoint.y)
                startPoint.set(event.x, event.y)
                absolutePoint.set(absolutePoint.x + deltaPoint.x, absolutePoint.y + deltaPoint.y)

                invalidate()
            }
        }
    }

    // Draws using a line, adding the appropriate `Edit`s, paints, and translations to their
    // respective stacks (`ArrayDeque`s).
    private fun drawLine(event: MotionEvent) {
        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                edits.add(Line(event.x - absolutePoint.x, event.y - absolutePoint.y, 0f, 0f))
                paints.add(Paint(Ed.drawPaint))
                translations.add(absolutePoint)
            }
            MotionEvent.ACTION_MOVE -> {
                (edits.last() as Line).apply {
                    x2 = event.x - absolutePoint.x
                    y2 = event.y - absolutePoint.y
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {}
        }
    }

    // Draws using a `Path`, adding the appropriate `Edit`s, paints, and translations to their
    // respective stacks (`ArrayDeque`s).
    private fun drawFreehand(event: MotionEvent) {
        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                edits.add(Freehand(Path().apply { moveTo(event.x - absolutePoint.x, event.y - absolutePoint.y) }))
                paints.add(Paint(Ed.drawPaint).apply {
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.MITER
                })
                translations.add(absolutePoint)
            }
            MotionEvent.ACTION_MOVE -> {
                (edits.last() as Freehand).apply {
                    path.lineTo(event.x - absolutePoint.x, event.y - absolutePoint.y)
                }
                invalidate()
            }
        }
    }

    // Pops the most recently drawn line and redraws the canvas.
    fun undo() {
        edits.removeLastOrNull()
        paints.removeLastOrNull()
        translations.removeLastOrNull()
        invalidate()
    }

    // Saves the annotated PDF page to a file.
    fun save(activity: EditorActivity) {
        val tmpBmp = Bitmap.createBitmap((bmp.width * scale).toInt(),
            (bmp.height * scale).toInt(), Bitmap.Config.ARGB_8888)
        Canvas(tmpBmp).apply {
            this.drawColor(Color.WHITE)
            drawToCanvas(this, 0f, 0f)
            Ctx.showSaveDialog(activity, context, tmpBmp)
        }
    }

    // Done this way because Kotlin's enum classes aren't actually sum types; they must have the
    // same field types.
    private sealed interface Edits
    private data class Line(val x1: Float, val y1: Float, var x2: Float, var y2: Float) : Edits
    private data class Freehand(val path: Path) : Edits




    // The following is a remnant of my experiments with scaling.  I thought it was interesting,
    // so I left it.  There are a few other locations with commented out code that is associated
    // with this.

//    // There are times when your fingers get too close or too far apart, stopping the scaling
//    // gesture.  At that point, it immediately turns into a translating drag, even though there is
//    // never a `MotionEvent.ACTION_UP`.  Because of the way translation is done, the
//    // `moveX` and `moveY` fields could be in an uninitialized state when this happened, causing the
//    // image to suddenly jump to a seemingly random position.
//    // The solution is to track the state of the scaling.  The mode changes to `Scaling` when the
//    // gesture starts, and to `WasScaling` when it ends.  Then, whenever a translation gesture is
//    // started, it waits until it sees a `MotionEvent.ACTION_UP` before it starts moving the image.
//    private enum class MoveMode {
//        None, Scaling, WasScaling
//    }
}
