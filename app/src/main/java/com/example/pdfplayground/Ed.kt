package com.example.pdfplayground

import android.graphics.Color
import android.graphics.Paint

// The editor context object, used for passing data between `EditorActivity` and `EditorView`.
// It's named so concisely because I don't want to have to read a whole bunch of useless
// "EditorActivityContext.whatever"s every time I need some state.
object Ed {
    var dragMode = DragMode.Pen
    var trayMode = DragMode.Move
    var lineWeight = LineWeight.Light
    var drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 8f
    }

    fun initWithDefaults() {
        dragMode = DragMode.Pen
        trayMode = DragMode.Move
        lineWeight = LineWeight.Light
    }

    enum class DragMode {
        Move, Line, Pen, Highlighter
    }

    enum class LineWeight {
        Heavy, Medium, Light
    }
}