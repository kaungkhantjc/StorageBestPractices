package com.jcsamples.storagebestpractices.models

import android.net.Uri
import java.util.*

data class ImageModel(
    val id: Long,
    val displayName: String,
    val dateAdded: Date,
    val contentUri: Uri
)
