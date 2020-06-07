package com.example.camera_app

import android.graphics.ImageDecoder
import android.os.Build
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileOutputStream
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {


    lateinit var curPhotoPath: String //문자열 형태의 사진경로값(초기값을 null로시작하고싶을때)
    var REQUEST_IMAGE_CAPTURE = 1 //카메라 사진촬영요청코드


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setPermission()//権限チェックメソッド実行

        //撮影ボタンを押した際
        btn_capture.setOnClickListener {
            takeCapture()//기본카메라앱을 실행시켜서 사진촬영
        }

    }
    private fun takeCapture() {
        //기본 카메라 앱 실
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also{
                val photoFile: File? = try{
                createImageFile()
            } catch(ex:IOException){
                null
            }

            //撮影したイメージを送り
            photoFile?.also{
                val photoUri : Uri = FileProvider.getUriForFile(
                    this,
                    "com.example.camera_app",
                        it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE)

                }
            }
        }
    }


    //イメージファイル作成
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg",storageDir)
                .apply {curPhotoPath = absolutePath}
    }

    //権限
    private fun setPermission() {
        //権限メッセージ
        val permission = object :  PermissionListener {

            override fun onPermissionGranted() {
                Toast.makeText(this@MainActivity, "権限許容", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                Toast.makeText(this@MainActivity, "権限拒否", Toast.LENGTH_SHORT).show()
            }
        }
        //権限チェック
        TedPermission.with(this)
                .setPermissionListener(permission)
                .setRationaleMessage("カメラの権限が必要です。")
                .setDeniedMessage("拒否されました。")
                .setPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA)
                .check()

    }

    //撮影したイメージをイメージビューに表示면 기본카메라앱으로부터받아온 사진 결과
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //이미지를 성공적으로 가져왔다면
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val bitmap: Bitmap
            val file = File(curPhotoPath)
            if (Build.VERSION.SDK_INT < 28) {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file))
                iv_profile.setImageBitmap(bitmap)
            } else {
                val decode = ImageDecoder.createSource(
                        this.contentResolver,
                        Uri.fromFile(file)
                )
                bitmap = ImageDecoder.decodeBitmap(decode)
                iv_profile.setImageBitmap(bitmap)
            }
            savePhoto(bitmap)
        }

    }
    //갤러리에 저장
    private fun savePhoto(bitmap: Bitmap) {
        val folderPath = Environment.getExternalStorageDirectory().absolutePath + "/Picture/"//사진폴더로 저장하기위한 경로선언
        val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "${timestamp}.jpeg"
        val folder = File(folderPath)
        if (!folder.isDirectory) {//현재 해당 경로에 폴더가 존재하지 않는지(!) 검사
            folder.mkdirs()//mkdirs:makedirectory의 줄임말로 해당 경로에 폴더 자동으로 새로 만들기
            }
        //실제적인 저장처리
        val out = FileOutputStream(folderPath +fileName)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        Toast.makeText(this,"사진이 앨범에 저장되었", Toast.LENGTH_SHORT).show()
    }

     //写真を撮るとき回転させて撮ってもイメージビューでは正方向に表示するようにする。
    private fun exifOrientationToDegress(exifOrientation: Int): Int {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270
        }
        return 0
    }

    private fun rotate(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

}