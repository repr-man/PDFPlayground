package com.example.pdfplayground

import android.content.*
import android.graphics.*
import android.os.*
import android.util.AttributeSet
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.graphics.*
import kotlin.math.*

// A custom view that implements all the mechanics of capturing an image from the PDF.
class CaptureView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val bmp = Ctx.bitmapHolder!!

    private var scale = 0f
    private var innerRect = Rect()
    private var outerRect = Rect()
    private var startPoint = PointF()
    private var leftThumbPos = Point()
    private var rightThumbPos = Point()
    private val thumbRadius = 40f
    private var showThumbs = true
    private var moveState = MoveState.None

    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outerPaint = Paint().apply {
        color = Color.argb(127, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val thumbPaintInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        elevation = 3f
    }
    private val thumbPaintOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
        elevation = 3f
    }

    /// Populates the variables necessary for this `View`.
    fun setup() {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        var yBound = false

        // Calculates the scale factor for both axes.
        scale = 1 / (bmp.width / screenWidth)
        if(bmp.height * scale > screenHeight) {
            scale = 1 / (bmp.height / screenHeight)
            yBound = true
        }

        // Sets the location for the PDF bitmap to be drawn.
        if(yBound) {
            startPoint.set((screenWidth - bmp.width * scale) / (scale*scale), 0f)
        } else {
            startPoint.set(0f, (screenHeight - bmp.height * scale) / (scale*scale))
        }

        // Defaults to capturing the inner two thirds of each dimension.
        innerRect.set(
            (screenWidth / 6).toInt(),
            (screenHeight / 6).toInt(),
            (screenWidth / 6 * 5).toInt(),
            (screenHeight / 6 * 5).toInt(),
        )
        outerRect.set(0, 0, screenWidth.toInt(), screenHeight.toInt())

        if(Ctx.rightHanded) {
            leftThumbPos.set(innerRect.left, innerRect.bottom)
            rightThumbPos.set(innerRect.right, innerRect.top)
        } else {
            leftThumbPos.set(innerRect.left, innerRect.top)
            rightThumbPos.set(innerRect.right, innerRect.bottom)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.apply {
            save()

            withScale(scale, scale, 0f, 0f) {
                drawBitmap(bmp, startPoint.x, startPoint.y, bmpPaint)
            }

            drawPath(outerRect.minus(innerRect).boundaryPath, outerPaint)

            // Draws thumbs if not in the middle of the screenshot sequence.
            if(showThumbs) {
                val leftThumbY: Float
                val rightThumbY: Float
                if(Ctx.rightHanded) {
                    leftThumbY = innerRect.bottom.toFloat()
                    rightThumbY = innerRect.top.toFloat()
                } else {
                    leftThumbY = innerRect.top.toFloat()
                    rightThumbY = innerRect.bottom.toFloat()
                }

                drawCircle(innerRect.left.toFloat(),
                    leftThumbY,
                    thumbRadius,
                    thumbPaintInner)
                drawCircle(innerRect.left.toFloat(),
                    leftThumbY,
                    thumbRadius,
                    thumbPaintOuter)
                drawCircle(innerRect.right.toFloat(),
                    rightThumbY,
                    thumbRadius,
                    thumbPaintInner)
                drawCircle(innerRect.right.toFloat(),
                    rightThumbY,
                    thumbRadius,
                    thumbPaintOuter)
            }

            restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action and MotionEvent.ACTION_MASK) {
            // Checks if the touch is within the bounds of either of the thumbs.
            MotionEvent.ACTION_DOWN -> {
                moveState = if(event.x in (leftThumbPos.x - thumbRadius)..(leftThumbPos.x + thumbRadius)
                    && event.y in (leftThumbPos.y - thumbRadius)..(leftThumbPos.y + thumbRadius))
                    MoveState.Left
                else if(event.x in (rightThumbPos.x - thumbRadius)..(rightThumbPos.x + thumbRadius)
                    && event.y in (rightThumbPos.y - thumbRadius)..(rightThumbPos.y + thumbRadius)
                ) {
                    MoveState.Right
                } else {
                    MoveState.None
                }
            }
            // It's not pretty, but it adjusts the bounds of the cropping rectangle, accounting for
            // left handed mode.
            MotionEvent.ACTION_MOVE -> {
                when(moveState) {
                    MoveState.Left -> if(Ctx.rightHanded) {
                        if(event.x <= innerRect.right && event.y >= innerRect.top) {
                            innerRect.left = event.x.toInt()
                            innerRect.bottom = event.y.toInt()
                            leftThumbPos.set(innerRect.left, innerRect.bottom)
                        }
                    } else {
                        if(event.x <= innerRect.right && event.y <= innerRect.bottom) {
                            innerRect.left = event.x.toInt()
                            innerRect.top = event.y.toInt()
                            leftThumbPos.set(innerRect.left, innerRect.top)
                        }
                    }
                    MoveState.Right -> if(Ctx.rightHanded) {
                        if(event.x >= innerRect.left && event.y <= innerRect.bottom) {
                            innerRect.right = event.x.toInt()
                            innerRect.top = event.y.toInt()
                            rightThumbPos.set(innerRect.right, innerRect.top)
                        }
                    } else {
                        if(event.x >= innerRect.left && event.y >= innerRect.top) {
                            innerRect.right = event.x.toInt()
                            innerRect.bottom = event.y.toInt()
                            rightThumbPos.set(innerRect.right, innerRect.bottom)
                        }
                    }
                    MoveState.None -> {}
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                moveState = MoveState.None
            }
        }

        return true
    }

    /// Takes a screenshot of the selected area and saves it in a user-defined file.
    @RequiresApi(Build.VERSION_CODES.Q)
    fun captureArea(activity: CaptureActivity) {
        showThumbs = false
        invalidate()

        // This weird pattern with the `Handler` is used to force the posted code to wait until the
        // redraw has taken place.  We need to redraw to remove the thumbs from the screenshot.
        Handler().post {
            val left = min(leftThumbPos.x, rightThumbPos.x)
            val right = max(leftThumbPos.x, rightThumbPos.x)
            val top = min(leftThumbPos.y, rightThumbPos.y)
            val bottom = max(leftThumbPos.y, rightThumbPos.y)

            val outBmp = Bitmap.createBitmap(
                right - left,
                bottom - top,
                Bitmap.Config.ARGB_8888)

            // Adjusts the coordinates of the thumbs for any system ui bars, etc.
            val windowLocation = IntArray(2)
            getLocationInWindow(windowLocation)

            // Takes a screenshot of the selected area.
            PixelCopy.request(
                activity.window!!,
                Rect(
                    left + windowLocation[0],
                    top + windowLocation[1],
                    right + windowLocation[0],
                    bottom + windowLocation[1]
                ),
                outBmp,
                { copyResult: Int ->
                    if(copyResult == PixelCopy.SUCCESS) {
                        Ctx.showSaveDialog(activity, context, outBmp)
                    }
                },
                Handler())

            // Adds the thumbs back.  If you watch carefully, you can see them disappear and
            // reappear.  I'm not sure if this would be noticeable on a real device.
            showThumbs = true
            invalidate()
        }
    }

    // The states for tracking which thumb is being dragged.
    private enum class MoveState {
        None, Left, Right
    }
}