package com.jcsamples.storagebestpractices

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jcsamples.storagebestpractices.adapters.ImageAdapter
import com.jcsamples.storagebestpractices.databinding.ActivityMainBinding
import com.jcsamples.storagebestpractices.decorations.SpacingItemDecoration
import com.jcsamples.storagebestpractices.models.ImageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    companion object {
        private const val READ_EXTERNAL_STORAGE_REQUEST = 100
        private val specificFolder = "${Environment.DIRECTORY_DCIM}/Facebook/"
    }

    private lateinit var binding: ActivityMainBinding
    private val images = mutableListOf<ImageModel>()
    private lateinit var adapter: ImageAdapter
    private var pendingDeleteImage: ImageModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recycler.addItemDecoration(SpacingItemDecoration(2, 20, true))
        adapter = ImageAdapter(images) { imageModel -> showDeleteImageDialog(imageModel) }
        binding.recycler.adapter = adapter

        binding.btnAddImage.setOnClickListener {
            addImageRequestResult.launch(Intent(this, AddImageActivity::class.java))
        }
        checkPermission()
    }

    private val addImageRequestResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadImages()
            }
        }

    private fun checkPermission() {
        if (haveStoragePermission()) {
            loadImages()
        } else {
            requestPermission()
        }
    }

    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun loadImages() {
        images.clear() // to refresh images after adding new image

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        else MediaStore.Images.Media.DATA + " LIKE ? "

        val selectionArgs = arrayOf("%$specificFolder%")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        CoroutineScope(Dispatchers.IO).launch {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val dateModified =
                        Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                    val displayName = cursor.getString(displayNameColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val image = ImageModel(id, displayName, dateModified, contentUri)
                    images.add(image)
                }
            }

            runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun requestPermission() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        ActivityCompat.requestPermissions(this, permissions, READ_EXTERNAL_STORAGE_REQUEST)
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

    private fun showDeleteImageDialog(imageModel: ImageModel) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete confirmation")
            .setMessage("Are you sure to delete ${imageModel.displayName}")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                performDeleteImage(imageModel)
            }
            .show()
    }

    private val deleteImageRequestResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                pendingDeleteImage?.let { performDeleteImage(it) }
                pendingDeleteImage = null
            } else {
                MaterialAlertDialogBuilder(this)
                    .setMessage("User denied to delete this image")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }

    private fun performDeleteImage(imageModel: ImageModel) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                contentResolver.delete(
                    imageModel.contentUri,
                    "${MediaStore.Images.Media._ID} = ?",
                    arrayOf(imageModel.id.toString())
                )

                runOnUiThread {
                    val index = images.indexOf(pendingDeleteImage)
                    adapter.notifyItemRemoved(index)
                }

            } catch (securityException: SecurityException) {
                // This happens when deleting files that other apps created
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException
                            ?: throw securityException
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(recoverableSecurityException.userAction.actionIntent.intentSender)
                            .build()

                    this@MainActivity.pendingDeleteImage = imageModel
                    deleteImageRequestResult.launch(intentSenderRequest)

                } else {
                    throw securityException
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadImages()
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