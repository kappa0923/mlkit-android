// MIT License
//
// Copyright (c) 2018 kappa0923
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package jp.kappa0923.android.mlkit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

import jp.kappa0923.android.mlkit.GraphicOverlay.Graphic

import com.google.firebase.ml.vision.text.FirebaseVisionText

/**
 * 認識したテキストと矩形をオーバーレイで描画するためのクラス
 */
class TextGraphic internal constructor(overlay: GraphicOverlay, private val element: FirebaseVisionText.Element) : Graphic(overlay) {
    companion object {
        private const val TEXT_COLOR = Color.RED
        private const val TEXT_SIZE = 50.0f
        private const val STROKE_WIDTH = 4.0f
    }

    private val rectPaint: Paint = Paint()
    private val textPaint: Paint = Paint()

    init {
        rectPaint.color = TEXT_COLOR
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = STROKE_WIDTH

        textPaint.color = TEXT_COLOR
        textPaint.textSize = TEXT_SIZE

        postInvalidate()
    }

    /**
     * 認識したテキストと矩形をCanvas上に描画する
     */
    override fun draw(canvas: Canvas) {
        // Draws the bounding box around the TextBlock.
        val rect = RectF(element.boundingBox)
        canvas.drawRect(rect, rectPaint)

        // Renders the text at the bottom of the box.
        canvas.drawText(element.text, rect.left, rect.bottom, textPaint)
    }

}
