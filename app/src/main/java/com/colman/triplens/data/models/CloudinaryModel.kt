package com.colman.triplens.data.models

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.colman.triplens.base.MyApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CloudinaryModel {

    companion object {
        private var isInitialized = false
    }

    init {
        if (!isInitialized) {
            val config = mapOf(
                "cloud_name" to "doenqjp4c",
                "api_key" to "937292938273937",
                "api_secret" to "cOxRY8T4K1NCJlZ33XWVvhtzlQY"
            )
            MyApplication.appContext?.let {
                MediaManager.init(it, config)
                isInitialized = true
            }
        }
    }

    suspend fun uploadImage(imageUri: Uri, name: String): String = withContext(Dispatchers.IO) {
        val context = MyApplication.appContext
            ?: throw Exception("App context not available")
        val file = uriToFile(imageUri, context)

        val result = MediaManager.get().cloudinary.uploader().upload(file, mapOf(
            "folder" to "posts",
            "public_id" to name
        ))
        result["secure_url"] as? String
            ?: throw Exception("Upload succeeded but no URL returned")
    }

    private fun uriToFile(uri: Uri, context: android.content.Context): File {
        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }
}
