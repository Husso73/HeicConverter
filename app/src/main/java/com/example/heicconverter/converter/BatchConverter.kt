package com.example.heicconverter.converter

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.heicconverter.utils.FileUtils

object BatchConverter {

    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun convertAll(
        context:  Context,
        uris:     List<Uri>,
        onProgress: (done: Int) -> Unit = {}   // ← callback
    ): List<Uri> {
        val results = mutableListOf<Uri>()
        uris.forEachIndexed { index, uri ->
            val bitmap = HeicConverter.decode(context, uri)
            val saved  = FileUtils.saveJpg(context, bitmap)
            bitmap.recycle()
            results.add(saved)
            onProgress(index + 1)              // ← notifie après chaque image
        }
        return results
    }
}