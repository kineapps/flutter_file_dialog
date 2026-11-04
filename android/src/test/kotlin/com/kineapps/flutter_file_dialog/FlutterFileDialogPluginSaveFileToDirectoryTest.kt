// Copyright (c) 2026 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.kineapps.flutter_file_dialog

import android.app.Activity
import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileNotFoundException
import java.io.OutputStream
import java.nio.channels.FileChannel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class FlutterFileDialogPluginSaveFileToDirectoryTest {

    private val directoryUri = Uri.parse("content://test/tree/dir")
    private val fileUri = Uri.parse("content://test/document/file.txt")
    private val bytes = byteArrayOf(0, 1, 2, 127, -1)

    private val contentResolver = mock(ContentResolver::class.java)
    private val activity = mock(Activity::class.java).apply {
        `when`(this.contentResolver).thenReturn(this@FlutterFileDialogPluginSaveFileToDirectoryTest.contentResolver)
    }
    private val plugin = FlutterFileDialogPlugin().apply {
        val binding = mock(ActivityPluginBinding::class.java)
        `when`(binding.activity).thenReturn(activity)
        onAttachedToActivity(binding)
    }
    private val documentFile: MockedStatic<DocumentFile> = mockStatic(DocumentFile::class.java).apply {
        val folder = mock(DocumentFile::class.java)
        val newFile = mock(DocumentFile::class.java)
        `when`(newFile.uri).thenReturn(fileUri)
        `when`(folder.createFile("text/plain", "file.txt")).thenReturn(newFile)
        `when`<DocumentFile> { DocumentFile.fromTreeUri(activity, directoryUri) }.thenReturn(folder)
    }

    @After
    fun tearDown() {
        documentFile.close()
    }

    private fun givenOutputStream(outputStream: OutputStream?) {
        `when`(contentResolver.openOutputStream(fileUri)).thenReturn(outputStream)
    }

    private fun givenOpeningFails(error: Exception) {
        `when`(contentResolver.openOutputStream(fileUri)).thenAnswer { throw error }
    }

    private fun callSaveFileToDirectory(): FakeMethodChannelResult {
        val result = FakeMethodChannelResult()
        plugin.onMethodCall(
                MethodCall("saveFileToDirectory", mapOf(
                        "directory" to directoryUri.toString(),
                        "mimeType" to "text/plain",
                        "fileName" to "file.txt",
                        "data" to bytes)),
                result)
        return result
    }

    private fun verifyOnlyOpenedOutputStream() {
        verify(contentResolver).openOutputStream(fileUri)
        verifyNoMoreInteractions(contentResolver)
    }

    @Test
    fun `saveFileToDirectory - should write to a parcel pipe without accessing its channel`() {
        // GIVEN: Robolectric backs createPipe with a regular file, not a native pipe.
        // Reject channel access explicitly; native ESPIPE behavior needs a device test.
        val pipe = ParcelFileDescriptor.createPipe()
        ParcelFileDescriptor.AutoCloseInputStream(pipe[0]).use { inputStream ->
            val outputStream = object : ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]) {
                override fun getChannel(): FileChannel {
                    throw AssertionError("A pipe must not be truncated")
                }
            }
            outputStream.use {
                givenOutputStream(outputStream)

                // WHEN
                val result = callSaveFileToDirectory()

                // THEN
                assertArrayEquals(bytes, inputStream.readBytes())
                assertEquals(1, result.completionCount)
                assertEquals(1, result.successCount)
                assertEquals(fileUri.path, result.lastSuccessValue)
                verifyOnlyOpenedOutputStream()
            }
        }
    }

    @Test
    fun `saveFileToDirectory - should flush and close a non-file stream`() {
        // GIVEN
        val outputStream = RecordingOutputStream()
        givenOutputStream(outputStream)

        // WHEN
        val result = callSaveFileToDirectory()

        // THEN
        assertArrayEquals(bytes, outputStream.toByteArray())
        assertEquals(listOf("flush", "close"), outputStream.events)
        assertEquals(1, result.completionCount)
        assertEquals(1, result.successCount)
        assertEquals(fileUri.path, result.lastSuccessValue)
        verifyOnlyOpenedOutputStream()
    }

    @Test
    fun `saveFileToDirectory - should reply save_file_failed when openOutputStream returns null`() {
        // GIVEN
        givenOutputStream(null)

        // WHEN
        val result = callSaveFileToDirectory()

        // THEN
        assertEquals(1, result.completionCount)
        assertEquals("save_file_failed", result.lastErrorCode)
        assertEquals("openOutputStream returned null", result.lastErrorMessage)
        verifyOnlyOpenedOutputStream()
    }

    @Test
    fun `saveFileToDirectory - should reply save_file_failed when opening fails`() {
        // GIVEN
        givenOpeningFails(FileNotFoundException("Provider failed"))

        // WHEN
        val result = callSaveFileToDirectory()

        // THEN
        assertEquals(1, result.completionCount)
        assertEquals("save_file_failed", result.lastErrorCode)
        assertEquals("Provider failed", result.lastErrorMessage)
        verifyOnlyOpenedOutputStream()
    }

    @Test
    fun `saveFileToDirectory - should reply security_exception when access is denied`() {
        // GIVEN
        givenOpeningFails(SecurityException("Access denied"))

        // WHEN
        val result = callSaveFileToDirectory()

        // THEN
        assertEquals(1, result.completionCount)
        assertEquals("security_exception", result.lastErrorCode)
        assertEquals("Access denied", result.lastErrorMessage)
        verifyOnlyOpenedOutputStream()
    }
}
