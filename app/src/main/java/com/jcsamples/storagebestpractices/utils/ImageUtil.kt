package com.jcsamples.storagebestpractices.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


object ImageUtil {
    fun writeFile(context: Context, fileName: String, filePath: String, bitmap: Bitmap): Uri? {
        val imageOutputStream: OutputStream?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver: ContentResolver = context.contentResolver
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                filePath
            )
            val imageUri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageOutputStream = resolver.openOutputStream(imageUri!!)
            if (writeImageOutputStream(imageOutputStream, bitmap)) {
                return imageUri
            }
        } else {
            @Suppress("DEPRECATION")
            val imageDir = File(Environment.getExternalStorageDirectory(), filePath).absolutePath
            val file = File(imageDir)
            if (!file.exists()) if (!file.mkdir()) return null
            val image = File(imageDir, fileName)
            imageOutputStream = FileOutputStream(image)
            if (writeImageOutputStream(imageOutputStream, bitmap)) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.path),
                    arrayOf("image/jpg"),
                    null
                )
                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileProvider",
                    image
                )
            }
        }

        return null
    }

    private fun writeImageOutputStream(imageOutputStream: OutputStream?, bitmap: Bitmap): Boolean {
        return if (imageOutputStream != null) {
            imageOutputStream.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageOutputStream)
            }
            true
        } else false
    }
}