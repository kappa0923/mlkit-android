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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.text.FirebaseVisionText

import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@RuntimePermissions
class MainActivity : AppCompatActivity() {
    companion object {
        const val INTENT_PHOTO = 1001
    }

    private var imageMaxWidth = 0
    private var imageMaxHeight = 0
    private var imageFilePath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_photo -> {
                graphicOverlay.clear()
                setImageViewSize()
                showCameraWithPermissionCheck()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            INTENT_PHOTO -> {
                if (resultCode == Activity.RESULT_OK) {
                    val capturePhoto = getScaledBitmap()
                    setRecognizePhotoToView(capturePhoto)
                    val x = (imageMaxWidth - capturePhoto.width).toFloat() / 2
                    val y = (imageMaxHeight - capturePhoto.height).toFloat() / 2
                    graphicOverlay.translate(x, y)

                    // TODO : Run text recognition or face detection.
//                    runTextRecognition(capturePhoto)
//                    runFaceDetection(capturePhoto)
                } else {
                    showToast("Camera Canceled")
                }
            }
        }
    }

    /**
     * カメラの呼び出し
     */
    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showCamera() {
        val imageFile = createImageFile()
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val authorities = "$packageName.fileprovider"
        val imageUri = FileProvider.getUriForFile(applicationContext, authorities, imageFile)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, INTENT_PHOTO)
    }

    /**
     * カメラの画像を保存するための空ファイルを作成
     * @return 空ファイル
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPAN).format(Date())
        val imageFileName = "mlkit_" + timeStamp + "_"
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (!storageDir.exists()) storageDir.mkdirs()
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        imageFilePath = imageFile.absolutePath
        return imageFile
    }

    /**
     * ストレージから撮影した画像を変形しつつ読み込む
     * @return 変形後の読み込み画像
     */
    private fun getScaledBitmap(): Bitmap {
        val bmpOptions = BitmapFactory.Options()
        bmpOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imageFilePath, bmpOptions)

        bmpOptions.inJustDecodeBounds = false

        val originImage = BitmapFactory.decodeFile(imageFilePath, bmpOptions)
        val matrix = Matrix()
        matrix.postRotate(90f)

        val rotatedImage =
                if (originImage.width > originImage.height)
                    Bitmap.createBitmap(originImage, 0, 0,
                            originImage.width,
                            originImage.height,
                            matrix, true)
                else originImage

        val scaleFactor = Math.max(rotatedImage.width.toFloat() / imageMaxWidth.toFloat(),
                rotatedImage.height.toFloat() / imageMaxHeight.toFloat())
        matrix.setScale(1.0f / scaleFactor, 1.0f / scaleFactor)

        return Bitmap.createBitmap(rotatedImage, 0, 0,
                rotatedImage.width,
                rotatedImage.height,
                matrix, true)
    }

    /**
     * 対象の画像を画面にセットする
     * @param photo 対象の画像
     */
    private fun setRecognizePhotoToView(photo: Bitmap) {
        imageView.setImageBitmap(photo)
    }

    /**
     * 対象の画像のテキスト認識を行う
     * @param recognitionTarget 認識対象の画像
     */
    private fun runTextRecognition(recognitionTarget: Bitmap) {
        // TODO : ここにテキスト認識のコードを追加する
    }

    /**
     * テキスト認識の結果を処理する
     * @param texts 認識結果のテキスト情報
     */
    private fun processTextRecognitionResult(texts: FirebaseVisionText) {
        // TODO : ここにテキスト認識の結果を処理するコードを追加する
    }

    /**
     * 対象の画像の顔検出を行う
     * @param detectionTarget 検出対象の画像
     */
    private fun runFaceDetection(detectionTarget: Bitmap) {
        // TODO : ここに顔検出のコードを追加する
    }

    private fun processFaceDetection(faces: List<FirebaseVisionFace>) {
        // TODO : ここに顔検出の結果を処理するコードを追加する
    }

    private fun setImageViewSize() {
        if (imageMaxWidth == 0) {
            imageMaxWidth = imageView.width
        }
        if (imageMaxHeight == 0) {
            imageMaxHeight = imageView.height
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

}
