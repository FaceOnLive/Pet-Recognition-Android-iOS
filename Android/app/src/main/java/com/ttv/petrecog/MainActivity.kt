package com.ttv.petrecog

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ttv.face.FaceEngine
import com.ttv.face.FaceResult
import com.ttv.facerecog.R
import java.io.File


class MainActivity : AppCompatActivity(){
    companion object {
        lateinit var userLists: ArrayList<FaceEntity>
    }

    private var context:Context? = null
    private var mydb: DBHelper? = null

    init {
        userLists = ArrayList(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = this
        FaceEngine.createInstance(this).init()
        mydb = DBHelper(this)
        mydb!!.getAllUsers()

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("mode", 0)
            startActivityForResult(intent, 2)
//            val intent = Intent()
//            intent.setType("image/*")
//            intent.setAction(Intent.ACTION_PICK)
//            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1)
        }

        val btnVerify = findViewById<Button>(R.id.btnVerify)
        btnVerify.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("mode", 1)
            startActivityForResult(intent, 2)
        }
        btnVerify.isEnabled = false;

        val btnUsers = findViewById<Button>(R.id.btnUser)
        btnUsers.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        findViewById<Button>(R.id.btnVerify).isEnabled = userLists.size > 0
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            try {
                var bitmap: Bitmap = ImageRotator.getCorrectlyOrientedImage(this, data?.data!!)
                val faceResults:List<FaceResult> = FaceEngine.getInstance().detectFaceFromBitmap(bitmap, 0)
                if(faceResults.count() == 1) {
                    var left = faceResults.get(0).left
                    var top = faceResults.get(0).top
                    var right = faceResults.get(0).right
                    var bottom = faceResults.get(0).bottom
                    if(left < 0) left = 0
                    if(top < 0) top = 0
                    if(right >= bitmap.width) right = bitmap.width - 1
                    if(bottom >= bitmap.height) bottom = bitmap.height - 1
                    val cropRect = Rect(left, top, right, bottom)
                    val cropImg = Utils.crop(
                        bitmap,
                        cropRect.left,
                        cropRect.top,
                        cropRect.width(),
                        cropRect.height(),
                        224,
                        224
                    )

                    val cacheDirectory = cacheDir
                    Utils.ensureDirExists(cacheDirectory)
                    val saveName = cacheDirectory.toString() + File.separator + "register.jpg"
                    Utils.bitmapToFile(cropImg, saveName)

                    val feats: ByteArray = DogRecognizeTask(this).execute(saveName).get()
//                    FaceEngine.getInstance().extractFeatureFromBitmap(bitmap, faceResults)
//
                    val userName = String.format("Dog%03d", userLists.size + 1)
                    val headRect = Utils.getBestRect(
                        bitmap.width,
                        bitmap.height,
                        Rect(
                            faceResults.get(0).left,
                            faceResults.get(0).top,
                            faceResults.get(0).right,
                            faceResults.get(0).bottom
                        )
                    )
                    val headImg = Utils.crop(
                        bitmap,
                        headRect.left,
                        headRect.top,
                        headRect.width(),
                        headRect.height(),
                        120,
                        120
                    )

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
                            for(user in userLists) {
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
                            userLists.add(face)

                            confirmUpdateDialog.cancel()

                            findViewById<Button>(R.id.btnVerify).isEnabled = userLists.size > 0
                            Toast.makeText(this, "Register succeed!", Toast.LENGTH_SHORT).show()
                        }

                } else if(faceResults.count() > 1) {
                    Toast.makeText(this, "Multiple face detected!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No face detected!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: java.lang.Exception) {
                //handle exception
                e.printStackTrace()
            }
        } else if(requestCode == 2 && resultCode == RESULT_OK) {
            val verifyResult = data!!.getIntExtra ("verifyResult", 0)
            val verifyScore = data!!.getStringExtra ("verifyScore")
            val verifyName = data!!.getStringExtra ("verifyName")
            if(verifyResult == 1) {
                Toast.makeText(this, "Verify succeed! " + verifyName + " " + verifyScore, Toast.LENGTH_SHORT).show()
            } else if(verifyResult == -1) {
                Toast.makeText(this, "Liveness failed! " + verifyScore, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Verify failed! " + verifyScore, Toast.LENGTH_SHORT).show()
            }
        }
    }
}