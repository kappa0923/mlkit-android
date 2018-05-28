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
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark

import jp.kappa0923.android.mlkit.GraphicOverlay.Graphic

/**
 * 認識したテキストと矩形をオーバーレイで描画するためのクラス
 */
class FaceGraphic internal constructor(overlay: GraphicOverlay, private val element: FirebaseVisionFace) : Graphic(overlay) {
    companion object {
        private const val TEXT_COLOR = Color.RED
        private const val TEXT_SIZE = 30.0f
        private const val STROKE_WIDTH = 4.0f
    }

    private val rectPaint: Paint = Paint()
    private val facePaint: Paint = Paint()

    init {
        rectPaint.color = TEXT_COLOR
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = STROKE_WIDTH

        facePaint.color = TEXT_COLOR
        facePaint.textSize = TEXT_SIZE

        postInvalidate()
    }

    /**
     * 認識したテキストと矩形をCanvas上に描画する
     */
    override fun draw(canvas: Canvas) {
        // Draws the bounding box around the FaceBlock.
        val faceRect = RectF(element.boundingBox)
        canvas.drawRect(faceRect, rectPaint)
        canvas.drawText("Smile: ${element.smilingProbability * 100}%", faceRect.left, faceRect.bottom, facePaint)

        drawLandmarkPosition(canvas, FirebaseVisionFaceLandmark.LEFT_EYE, "LeftEye")
        drawLandmarkPosition(canvas, FirebaseVisionFaceLandmark.RIGHT_EYE, "RightEye")
    }

    private fun drawLandmarkPosition(canvas: Canvas, landmarkID: Int, text: String) {
        val landmark: FirebaseVisionFaceLandmark? = element.getLandmark(landmarkID)
        landmark?.let {
            val position = it.position
            canvas.drawCircle(position.x, position.y, 10f, facePaint)
            canvas.drawText(text, position.x, position.y, facePaint)
        }
    }
}
