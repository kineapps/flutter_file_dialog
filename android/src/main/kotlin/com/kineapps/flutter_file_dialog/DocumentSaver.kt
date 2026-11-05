// Copyright (c) 2026 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.kineapps.flutter_file_dialog

import android.content.ContentResolver
import android.net.Uri
import android.system.Os
import android.system.OsConstants
import android.util.Log
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

private const val LOG_TAG = "DocumentSaver"

/**
 * Writes the whole [inputStream] to the existing document at [destinationUri],
 * replacing its content. Works with providers that return a pipe.
 */
internal fun writeToDocument(
        contentResolver: ContentResolver,
        inputStream: InputStream,
        destinationUri: Uri
) {
    // Ask the provider to truncate; the returned stream may be a pipe.
    var openedWithoutTruncation = false
    val destinationStream = try {
        contentResolver.openOutputStream(destinationUri, "wt")
    } catch (e: Exception) {
        if (e !is FileNotFoundException &&
                e !is UnsupportedOperationException &&
                e !is IllegalArgumentException) {
            throw e
        }
        Log.d(LOG_TAG, "Opening destination with wt failed; retrying with w", e)
        openedWithoutTruncation = true
        contentResolver.openOutputStream(destinationUri, "w")
    } ?: throw IOException("openOutputStream returned null")

    destinationStream.use { outputStream ->
        // Preserve overwrite behavior for regular files without seeking on pipes.
        if (openedWithoutTruncation && outputStream is FileOutputStream &&
                OsConstants.S_ISREG(Os.fstat(outputStream.fd).st_mode)) {
            outputStream.channel.truncate(0)
        }
        inputStream.copyTo(outputStream)
        outputStream.flush()
    }
    Log.d(LOG_TAG, "Saved file to '${destinationUri.path}'")
}

/**
 * Runs [save] on [Dispatchers.IO] and replies its returned path to [result]
 * on the main thread: "security_exception" for a [SecurityException],
 * "save_file_failed" for any other error.
 */
internal fun saveOnBackground(result: MethodChannel.Result, save: () -> String?) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            Log.d(LOG_TAG, "Saving file on background...")
            val path = withContext(Dispatchers.IO) { save() }
            Log.d(LOG_TAG, "...saved file on background, result: $path")
            result.success(path)
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "saveOnBackground", e)
            result.error("security_exception", e.localizedMessage, e.toString())
        } catch (e: Exception) {
            Log.e(LOG_TAG, "saveOnBackground failed", e)
            result.error("save_file_failed", e.localizedMessage, e.toString())
        }
    }
}
