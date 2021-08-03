package com.jcsamples.storagebestpractices

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jcsamples.storagebestpractices.databinding.ActivityAddImageBinding
import com.jcsamples.storagebestpractices.utils.ImageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddImageActivity : AppCompatActivity() {

    companion object {
        private const val WRITE_EXTERNAL_STORAGE_REQUEST = 101
        private val specificFolder = "${Environment.DIRECTORY_DCIM}/Facebook/"
        private const val IMAGE_URL =
            "https://i.pinimg.com/originals/0c/ab/98/0cab9809dde1839436b46a0c2f76cff9.jpg"
    }

    private lateinit var binding: ActivityAddImageBinding
    private var pendingBitmapResource: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.edtUrl.setText(IMAGE_URL)
        binding.btnDownloadImage.setOnClickListener { downloadImage() }

    }

    private fun downloadImage() {
        val url = binding.edtUrl.text.toString()

        val snackBar = Snackbar.make(binding.root, "Downloading", Snackbar.LENGTH_INDEFINITE)
        snackBar.show()

        Glide.with(this)
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    snackBar.dismiss()
                    this@AddImageActivity.pendingBitmapResource = resource
                    checkPermission()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    snackBar.dismiss()
                }

            })
    }

    private fun saveImage() {
        val fileName = "${System.currentTimeMillis()}.jpg"
        CoroutineScope(Dispatchers.IO).launch {
            val savedImageUri =
                ImageUtil.writeFile(
                    this@AddImageActivity,
                    fileName,
                    specificFolder,
                    pendingBitmapResource!!
                )

            runOnUiThread {
                if (savedImageUri != null) {
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Snackbar.make(binding.root, "Error while saving image", 2000).show()
                }
            }
        }
    }

    private fun checkPermission() {
        // we do not need write storage permission to store media files in Android 10 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || haveStoragePermission()) {
            saveImage()
        } else {
            requestPermission()
        }
    }

    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        ActivityCompat.requestPermissions(
            this, permissions,
            WRITE_EXTERNAL_STORAGE_REQUEST
        )
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setMessage("App need read storage permission to access files that other apps created.")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> requestPermission() }
            .show()
    }

    private fun goToSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            WRITE_EXTERNAL_STORAGE_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveImage()
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    showPermissionDeniedDialog()
                } else goToSettings()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}