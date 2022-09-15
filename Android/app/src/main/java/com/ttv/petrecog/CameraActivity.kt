package com.ttv.petrecog

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.ttv.face.FaceEngine
import com.ttv.face.FaceResult
import com.ttv.facerecog.R
import dmax.dialog.SpotsDialog
import io.fotoapparat.Fotoapparat
import io.fotoapparat.preview.Frame
import io.fotoapparat.selector.back
import io.fotoapparat.util.FrameProcessor
import io.fotoapparat.view.CameraView
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class CameraActivity : AppCompatActivity() {

    private val permissionsDelegate = PermissionsDelegate(this)
    private var hasPermission = false

    private var appCtx: Context? = null
    private var context: Context? = null
    private var btnRegister: Button? = null
    private var cameraView: CameraView? = null
    private var rectanglesView: FaceRectView? = null
    private var faceRectTransformer: FaceRectTransformer? = null
    private var frontFotoapparat: Fotoapparat? = null
    private var startVerifyTime: Long = 0
    private var recogScore: String? = null
    private var verifyName: String? = null
    private val VERIFY_THRESHOLD: Float = 0.95f
    private var captureStart: Int = 0
    private var mode: Int = 0
    private var mydb: DBHelper? = null
    private var progressDialog: AlertDialog? = null

    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            val i: Int = msg.what
            if (i == 0) {
                var drawInfoList = ArrayList<FaceRectView.DrawInfo>();
                var detectionResult = msg.obj as ArrayList<FaceResult>
                for(faceResult in detectionResult) {
                    var maskInfo = faceResult.mask;

                    var rect : Rect = faceRectTransformer!!.adjustRect(Rect(faceResult.left, faceResult.top, faceResult.right, faceResult.bottom));
                    var drawInfo : FaceRectView.DrawInfo;
                    drawInfo = FaceRectView.DrawInfo(
                        rect,
                        0,
                        0,
                        faceResult.liveness,
                        Color.YELLOW,
                        recogScore,
                        faceResult.livenessScore,
                        maskInfo
                    );
//                    if(faceResult.liveness == 1)
//                        drawInfo = FaceRectView.DrawInfo(rect, 0, 0, faceResult.liveness, Color.GREEN, recogScore, faceResult.livenessScore, maskInfo);
//                    else if(faceResult.liveness == -1)
//                        drawInfo = FaceRectView.DrawInfo(rect, 0, 0, faceResult.liveness, Color.YELLOW, recogScore, faceResult.livenessScore, maskInfo);
//                    else
//                        drawInfo = FaceRectView.DrawInfo(rect, 0, 0, faceResult.liveness, Color.RED, recogScore, faceResult.livenessScore, maskInfo);

                    drawInfoList.add(drawInfo);
                }

                rectanglesView!!.clearFaceInfo();
                rectanglesView!!.addFaceInfo(drawInfoList);
            } else if(i == 1) {
                var verifyResult = msg.obj as Int
                val intent = Intent()
                intent.putExtra("verifyResult", verifyResult);
                intent.putExtra("verifyScore", recogScore);
                intent.putExtra("verifyName", verifyName);
                setResult(RESULT_OK, intent)
                finish()
            } else if(i == 2) {
                recogScore = msg.obj as String
            } else if(i == 3) {
                verifyName = msg.obj as String
            } else if(i == 4) {
                val feats = msg.obj as ByteArray
                progressDialog!!.dismiss()

                val cacheDirectory = cacheDir
                val saveName = cacheDirectory.toString() + File.separator + "verify.jpg"

                if(mode == 0 && feats != null) {
                    val userName = String.format("Dog%03d", MainActivity.userLists.size + 1)
                    val bitmap = BitmapFactory.decodeFile(saveName)
                    val headImg = Utils.crop(bitmap, 0, 0, bitmap.width, bitmap.height, 120, 120)

                    val inputView = LayoutInflater.from(context)
                        .inflate(R.layout.dialog_input_view, null, false)
                    val editText = inputView.findViewById<EditText>(R.id.et_user_name)
                    val ivHead = inputView.findViewById<ImageView>(R.id.iv_head)
                    ivHead.setImageBitmap(headImg)
                    editText.setText(userName)
                    val confirmUpdateDialog: AlertDialog = AlertDialog.Builder(context!!)
                        .setView(inputView)
                        .setPositiveButton(
                            "OK", null
                        )
                        .setNegativeButton(
                            "Cancel", null
                        )
                        .create()
                    confirmUpdateDialog.show()
                    confirmUpdateDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener { v: View? ->
                            val s = editText.text.toString()
                            if (TextUtils.isEmpty(s)) {
                                editText.error = application.getString(R.string.name_should_not_be_empty)
                                return@setOnClickListener
                            }

                            var exists:Boolean = false
                            for(user in MainActivity.userLists) {
                                if(TextUtils.equals(user.userName, s)) {
                                    exists = true
                                    break
                                }
                            }

                            if(exists) {
                                editText.error = application.getString(R.string.duplicated_name)
                                return@setOnClickListener
                            }
                            val face = FaceEntity(s, headImg, feats)
                            mydb!!.insertUser(s, headImg, feats)
                            MainActivity.userLists.add(face)

                            confirmUpdateDialog.cancel()

                            Toast.makeText(context, "Register succeed!", Toast.LENGTH_SHORT).show()
                        }
                }
                else if(mode == 1 && feats != null) {
                    var maxScore:Double = 0.0
                    var maxScoreName:String = ""
                    for(user in MainActivity.userLists) {
                        val score = Utils.getSimilarity(user.feature, feats)
                        if(maxScore < score) {
                            maxScore = score
                            maxScoreName = user.userName
                        }
                    }

                    Log.e("TestEngine", "max score: " + maxScore)
                    val bitmap = BitmapFactory.decodeFile(saveName)
                    val headImg = Utils.crop(bitmap, 0, 0, bitmap.width, bitmap.height, 120, 120)

                    val inputView = LayoutInflater.from(context)
                        .inflate(R.layout.dialog_score_view, null, false)
                    val txtUserName = inputView.findViewById<TextView>(R.id.txt_user_name)
                    val txtResult = inputView.findViewById<TextView>(R.id.txt_result)
                    val ivHead = inputView.findViewById<ImageView>(R.id.iv_head)
                    ivHead.setImageBitmap(headImg)
                    if(maxScore > 0.92)
                        txtUserName.text = maxScoreName
                    else
                        txtUserName.text = "No such dog."
                    txtResult.text = "score: " + maxScore.toString()
                    val confirmUpdateDialog: AlertDialog = AlertDialog.Builder(context!!)
                        .setView(inputView)
                        .setPositiveButton(
                            "OK", null
                        )
                        .create()
                    confirmUpdateDialog.show()
                    confirmUpdateDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener { v: View? ->
                            captureStart = 0
                            confirmUpdateDialog.cancel()
                        }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        appCtx = applicationContext
        context = this
        cameraView = findViewById<View>(R.id.camera_view) as CameraView
        rectanglesView = findViewById<View>(R.id.rectanglesView) as FaceRectView
        btnRegister = findViewById<View>(R.id.btn_verify) as Button
        mode = intent.extras!!.getInt("mode")

        mydb = DBHelper(this)

        progressDialog = SpotsDialog.Builder()
            .setContext(this)
            .setMessage("Processing...")
            .setCancelable(false)
        .build()

        findViewById<Button>(R.id.btn_verify).setOnClickListener {
            if(captureStart == 0)
                captureStart = 1
        }

        if(mode == 0) btnRegister!!.text = "Register"
        else if(mode == 1) btnRegister!!.text = "Verify"

        hasPermission = permissionsDelegate.hasPermissions()
        if (hasPermission) {
            cameraView!!.visibility = View.VISIBLE
        } else {
            permissionsDelegate.requestPermissions()
        }

        frontFotoapparat = Fotoapparat.with(this)
            .into(cameraView!!)
            .lensPosition(back())
            .frameProcessor(SampleFrameProcessor())
//            .previewResolution { Resolution(1280,720) }
            .build()
    }

    override fun onStart() {
        super.onStart()
        if (hasPermission) {
            frontFotoapparat!!.start()
        }
    }


    override fun onStop() {
        super.onStop()
        if (hasPermission) {
            try {
                frontFotoapparat!!.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionsDelegate.hasPermissions() && !hasPermission) {
            hasPermission = true
            cameraView!!.visibility = View.VISIBLE
            frontFotoapparat!!.start()
        } else {
            permissionsDelegate.requestPermissions()
        }
    }

    fun adjustPreview(frameWidth: Int, frameHeight: Int) : Boolean{
        if(faceRectTransformer == null) {
            val frameSize: Size = Size(frameWidth, frameHeight);
            if(cameraView!!.measuredWidth == 0)
                return false;

            var displayOrientation: Int = 90;
            adjustPreviewViewSize (cameraView!!,
                cameraView!!, rectanglesView!!,
                Size(frameSize.width, frameSize.height), displayOrientation, 1.0f);

            faceRectTransformer = FaceRectTransformer (
                frameSize.height, frameSize.width,
                cameraView!!.getLayoutParams().width, cameraView!!.getLayoutParams().height,
                0, 1, false,
                false,
                false);

            return true;
        }

        return true;
    }

    private fun adjustPreviewViewSize(
        rgbPreview: View,
        previewView: View,
        faceRectView: FaceRectView,
        previewSize: Size,
        displayOrientation: Int,
        scale: Float
    ): ViewGroup.LayoutParams? {
        val layoutParams = previewView.layoutParams
        val measuredWidth = previewView.measuredWidth
        val measuredHeight = previewView.measuredHeight
        layoutParams.width = measuredWidth
        layoutParams.height = measuredHeight
//        previewView.layoutParams = layoutParams

        faceRectView.layoutParams.width = measuredWidth
        faceRectView.layoutParams.height = measuredHeight
        return layoutParams
    }

    /* access modifiers changed from: private */ /* access modifiers changed from: public */
    private fun sendMessage(w: Int, o: Any) {
        val message = Message()
        message.what = w
        message.obj = o
        mHandler.sendMessage(message)
    }

    inner class SampleFrameProcessor : FrameProcessor {
        var frThreadQueue: LinkedBlockingQueue<Runnable>? = null
        var frExecutor: ExecutorService? = null
        init {
            frThreadQueue = LinkedBlockingQueue<Runnable>(1)
            frExecutor = ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, frThreadQueue
            ) { r: Runnable? ->
                val t = Thread(r)
                t.name = "frThread-" + t.id
                t
            }
        }

        override fun invoke(frame: Frame) {

            val t0 = System.currentTimeMillis()
            val faceResults:List<FaceResult> = FaceEngine.getInstance().detectFaceFromYuv(frame.image, frame.size.width, frame.size.height, 7, 1)
            if(faceResults.count() > 0) {
                Log.e("TestEngine", "detect time: " + (System.currentTimeMillis() - t0));

                if(captureStart == 1) {
                    captureStart = 2

                    val out = ByteArrayOutputStream()
                    val yuvImage = YuvImage(frame.image, ImageFormat.NV21, frame.size.width, frame.size.height, null)
                    yuvImage.compressToJpeg(Rect(0, 0, frame.size.width, frame.size.height), 100, out)
                    val imageBytes: ByteArray = out.toByteArray()
                    val image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    val matrix = Matrix()
                    matrix.postRotate(90.0f)
                    val captureImage = Bitmap.createBitmap(
                        image, 0, 0, image.width, image.height, matrix, true
                    )

                    var left = faceResults.get(0).left
                    var top = faceResults.get(0).top
                    var right = faceResults.get(0).right
                    var bottom = faceResults.get(0).bottom
                    if(left < 0) left = 0
                    if(top < 0) top = 0
                    if(right >= captureImage.width) right = captureImage.width - 1
                    if(bottom >= captureImage.height) bottom = captureImage.height - 1
                    val cropRect = Rect(left, top, right, bottom)
                    val cropImg = Utils.crop(
                        captureImage,
                        cropRect.left,
                        cropRect.top,
                        cropRect.width(),
                        cropRect.height(),
                        224,
                        224
                    )

                    try {
                        val cacheDirectory = cacheDir
                        Utils.ensureDirExists(cacheDirectory)
                        val saveName = cacheDirectory.toString() + File.separator + "verify.jpg"

                        Utils.bitmapToFile(cropImg, saveName)

//                        sendMessage(4, saveName)
//                        val feats: ByteArray = DogRecognizeTask(context).execute(saveName).get()

                        if(frThreadQueue!!.remainingCapacity() > 0) {
                            runOnUiThread { progressDialog!!.show() }
                            frExecutor!!.execute(
                                FaceRecognizeRunnable(
                                    saveName
                                )
                            )
                        }
                    } catch (e:Exception){}
                }
            }

            if(adjustPreview(frame.size.width, frame.size.height))
                sendMessage(0, faceResults)

        }
    }

    inner class FaceRecognizeRunnable(saveName_: String) : Runnable {
        val saveName: String

        init {
            saveName = saveName_
        }

        override fun run() {
            try {
                // Set header
                val multipart =
                    HttpPostMultipart("http://192.248.148.182:8889/dog_noseprint", "utf-8", null)
                // Add form field
                multipart.addFilePart("image", File(saveName))
                // Print result
                val response = multipart.finish()
                //            Log.e("TestEngine", "response: " + response);
                val parentJson = JSONObject(response)
                val dataArr = parentJson.getJSONArray("data")
                if (dataArr.length() > 0) {
                    val bytes = ByteArray(dataArr.length() * 4)
                    val buffer = ByteBuffer.wrap(bytes)
                    buffer.order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until dataArr.length()) {
                        val feat = dataArr.getDouble(i).toFloat()
                        buffer.putFloat(feat)
                    }

                    sendMessage(4, bytes)
                    return
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            sendMessage(4, 0)
        }
    }
}