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
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.custom.*
import com.google.firebase.ml.custom.model.FirebaseCloudModelSource
import com.google.firebase.ml.custom.model.FirebaseModelDownloadConditions
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.text.FirebaseVisionText

import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.and

@RuntimePermissions
class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val INTENT_PHOTO = 1001
        const val DIM_BATCH_SIZE = 1
        const val DIM_PIXEL_SIZE = 3
        const val DIM_IMG_SIZE_X = 224
        const val DIM_IMG_SIZE_Y = 224
        const val LABEL_PATH = "labels.txt"
        const val HOSTED_MODEL_NAME = "mobilenet"
    }

    private var imageMaxWidth = 0
    private var imageMaxHeight = 0
    private var imageFilePath = ""
    private val labelList by lazy { loadLabelList() }
    private var interpreter: FirebaseModelInterpreter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupCustomModel()
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
//                    runImageLabeling(capturePhoto)
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
        val image = FirebaseVisionImage.fromBitmap(recognitionTarget)
        val detector = FirebaseVision.getInstance().visionTextDetector
        detector.detectInImage(image)
                .addOnSuccessListener { texts ->
                    processTextRecognitionResult(texts)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
    }

    /**
     * テキスト認識の結果を処理する
     * @param texts 認識結果のテキスト情報
     */
    private fun processTextRecognitionResult(texts: FirebaseVisionText) {
        val blocks = texts.blocks
        if (blocks.size == 0) {
            showToast("Text not found")
            return
        }

        graphicOverlay.clear()

        for (i in blocks.indices) {
            val lines = blocks[i].lines
            for (j in lines.indices) {
                val elements = lines[j].elements
                for (k in elements.indices) {
                    val textGraphic = TextGraphic(graphicOverlay, elements[k])

                    graphicOverlay.add(textGraphic)
                }
            }
        }
    }

    /**
     * 対象の画像の顔検出を行う
     * @param detectionTarget 検出対象の画像
     */
    private fun runFaceDetection(detectionTarget: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(detectionTarget)
        val options = FirebaseVisionFaceDetectorOptions.Builder()
                .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build()
        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
        detector.detectInImage(image)
                .addOnSuccessListener { faces ->
                    processFaceDetection(faces)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
    }

    /**
     * 顔検出の結果を処理する
     * @param faces 検出された顔のリスト
     */
    private fun processFaceDetection(faces: List<FirebaseVisionFace>) {
        if (faces.isEmpty()) {
            showToast("Face not found")
            return
        }

        graphicOverlay.clear()

        for (face in faces) {
            val faceGraphic = FaceGraphic(graphicOverlay, face)
            graphicOverlay.add(faceGraphic)
        }
    }

    /**
     * 対象の画像からラベル付けを行う
     * @param labelingTarget ラベル付け対象の画像
     */
    private fun runImageLabeling(labelingTarget: Bitmap) {
        val inputDims = intArrayOf(DIM_BATCH_SIZE, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, DIM_PIXEL_SIZE)
        val outputDims = intArrayOf(DIM_BATCH_SIZE, labelList.size)

        val dataOption = FirebaseModelInputOutputOptions.Builder()
                .setInputFormat(0, FirebaseModelDataType.BYTE, inputDims)
                .setOutputFormat(0, FirebaseModelDataType.BYTE, outputDims)
                .build()

        val inputData = convertBitmapToByteBuffer(labelingTarget)

        try {
            val inputs = FirebaseModelInputs.Builder().add(inputData).build()
            interpreter?.let {
                it.run(inputs, dataOption)
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            showToast("Error running model inference")
                        }
                        .addOnSuccessListener {
                            val labelProbArray = it.getOutput<Array<ByteArray>>(0)
                            processImageLabeling(labelProbArray)
                        }
            }
        } catch (e: FirebaseMLException) {
            showToast("Error running model inference")
            e.printStackTrace()
        }
    }

    /**
     * ラベル付けの結果を処理する
     * @param labels アウトプットされた情報
     */
    private fun processImageLabeling(labels: Array<ByteArray>) {
        if (labels.isEmpty()) {
            showToast("Not found")
            return
        }

        graphicOverlay.clear()

        Log.d(TAG, "Labeling")
        var topScore = 0.0f
        var topLabel = ""
        for (i in 0 until labelList.size) {
            val score = (labels[0][i] and 0xff.toByte()) / 255f
            if (score > topScore) {
                topScore = score
                topLabel = labelList[i]
            }
        }

        val labelGraphic = LabelGraphic(graphicOverlay, "$topLabel : $topScore")
        graphicOverlay.add(labelGraphic)
    }

    private fun setupCustomModel() {
        try {
            val conditions = FirebaseModelDownloadConditions.Builder()
                    .requireWifi()
                    .build()

            val cloudSource = FirebaseCloudModelSource.Builder(HOSTED_MODEL_NAME)
                    .enableModelUpdates(true)
                    .setInitialDownloadConditions(conditions)
                    .setUpdatesDownloadConditions(conditions)
                    .build()

            FirebaseModelManager.getInstance()
                    .registerCloudModelSource(cloudSource)

            val modelOptions = FirebaseModelOptions.Builder()
                    .setCloudModelName(HOSTED_MODEL_NAME)
                    .build()

            interpreter = FirebaseModelInterpreter.getInstance(modelOptions)
        } catch (e: FirebaseMLException) {
            showToast("Error while setting up the model")
            e.printStackTrace()
        }
    }

    private fun loadLabelList(): List<String> {
        val labelList = ArrayList<String>()
        BufferedReader(InputStreamReader(applicationContext.assets.open(LABEL_PATH))).use {
            var line = it.readLine()
            while (line != null) {
                labelList.add(line)
                line = it.readLine()
            }
        }

        return labelList
    }

    private fun convertBitmapToByteBuffer(data: Bitmap): ByteBuffer {
        val intValues = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)
        val imageData = ByteBuffer.allocateDirect(
                DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
        imageData.order(ByteOrder.nativeOrder())
        val scaledBitmap = Bitmap.createScaledBitmap(data, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y,
                true)
        imageData.rewind()
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.width, 0, 0,
                scaledBitmap.width, scaledBitmap.height)
        // Convert the image to int points.
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val value = intValues[pixel++]
                imageData.put((value shr 16 and 0xFF).toByte())
                imageData.put((value shr 8 and 0xFF).toByte())
                imageData.put((value and 0xFF).toByte())
            }
        }
        return imageData
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
