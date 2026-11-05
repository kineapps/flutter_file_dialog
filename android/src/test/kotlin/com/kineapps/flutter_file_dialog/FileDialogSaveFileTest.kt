// Copyright (c) 2026 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.kineapps.flutter_file_dialog

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class FileDialogSaveFileTest {

    @Test
    fun `onActivityResult - should save on a background thread and delete the temporary source file`() {
        // GIVEN a saveFile dialog started with data written to a temporary file
        val destinationUri = Uri.parse("content://test/document/file.txt")
        val bytes = byteArrayOf(0, 1, 2, 127, -1)
        val outputStream = RecordingOutputStream()
        val contentResolver = mock(ContentResolver::class.java)
        `when`(contentResolver.openOutputStream(destinationUri, "wt")).thenReturn(outputStream)
        val activity = mock(Activity::class.java)
        `when`(activity.contentResolver).thenReturn(contentResolver)
        val dialog = FileDialog(activity)
        val result = FakeMethodChannelResult()
        val fileNamePrefix = "save_temp_test_${System.nanoTime()}"
        dialog.saveFile(
                result = result,
                sourceFilePath = null,
                data = bytes,
                fileName = fileNamePrefix,
                mimeTypesFilter = null,
                localOnly = false)
        val requestCode = ArgumentCaptor.forClass(Int::class.java)
        verify(activity).startActivityForResult(any(Intent::class.java), requestCode.capture())

        // WHEN
        dialog.onActivityResult(requestCode.value, Activity.RESULT_OK, Intent().setData(destinationUri))
        result.awaitCompletion()

        // THEN
        assertArrayEquals(bytes, outputStream.toByteArray())
        assertEquals(1, result.successCount)
        assertEquals(destinationUri.path, result.lastSuccessValue)
        val leftovers = File(System.getProperty("java.io.tmpdir")!!)
                .listFiles { file -> file.name.startsWith(fileNamePrefix) }
        assertEquals(0, leftovers?.size ?: 0)
    }
}
