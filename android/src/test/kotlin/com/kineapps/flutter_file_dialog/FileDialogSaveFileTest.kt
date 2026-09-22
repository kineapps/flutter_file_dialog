// Copyright (c) 2026 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.kineapps.flutter_file_dialog

import android.app.Activity
import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.StructStat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class FileDialogSaveFileTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val destinationUri = Uri.parse("content://test/document/file.txt")
    private val contentResolver = mock(ContentResolver::class.java)
    private val activity = mock(Activity::class.java).apply {
        `when`(this.contentResolver).thenReturn(this@FileDialogSaveFileTest.contentResolver)
    }
    private val dialog = FileDialog(activity)

    private fun newSourceFile(): File = temporaryFolder.newFile().apply {
        writeBytes(byteArrayOf(0, 1, 2, 127, -1))
    }

    @Test
    fun `saveFile - should write to a parcel pipe without accessing its channel`() {
        // GIVEN: Robolectric backs createPipe with a regular file, not a native pipe.
        // Reject channel access explicitly; native ESPIPE behavior needs a device test.
        val sourceFile = newSourceFile()
        val pipe = ParcelFileDescriptor.createPipe()
        ParcelFileDescriptor.AutoCloseInputStream(pipe[0]).use { inputStream ->
            val outputStream = object : ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]) {
                override fun getChannel(): FileChannel {
                    throw AssertionError("A pipe must not be truncated")
                }
            }
            outputStream.use {
                `when`(contentResolver.openOutputStream(destinationUri, "wt"))
                        .thenReturn(outputStream)

                // WHEN
                val path = dialog.saveFile(sourceFile, destinationUri)

                // THEN
                assertEquals(destinationUri.path, path)
                assertArrayEquals(sourceFile.readBytes(), inputStream.readBytes())
                val order = inOrder(contentResolver)
                order.verify(contentResolver).openOutputStream(destinationUri, "wt")
                verifyNoMoreInteractions(contentResolver)
            }
        }
    }

    @Test
    fun `saveFile - should truncate a longer regular file after wt is rejected`() {
        // GIVEN: rw preserves the old content and starts at offset zero, like mode w.
        val sourceFile = newSourceFile()
        val destinationFile = temporaryFolder.newFile().apply {
            writeText("previous content with a longer tail")
        }
        RandomAccessFile(destinationFile, "rw").use { destination ->
            FileOutputStream(destination.fd).use { outputStream ->
                `when`(contentResolver.openOutputStream(destinationUri, "wt"))
                        .thenThrow(FileNotFoundException("Mode wt is not supported"))
                `when`(contentResolver.openOutputStream(destinationUri, "w"))
                        .thenReturn(outputStream)

                // Robolectric fstat ignores the descriptor. Supply the real file's
                // path-based stat; this does not verify native fd classification.
                val stat = Os.stat(destinationFile.path)
                mockStatic(Os::class.java).use { os ->
                    os.`when`<StructStat> { Os.fstat(outputStream.fd) }
                            .thenReturn(stat)

                    // WHEN
                    dialog.saveFile(sourceFile, destinationUri)
                }

                // THEN
                assertArrayEquals(sourceFile.readBytes(), destinationFile.readBytes())
                val order = inOrder(contentResolver)
                order.verify(contentResolver).openOutputStream(destinationUri, "wt")
                order.verify(contentResolver).openOutputStream(destinationUri, "w")
                verifyNoMoreInteractions(contentResolver)
            }
        }
    }

    @Test
    fun `saveFile - should flush and close a non-file stream when wt succeeds`() {
        // GIVEN
        val sourceFile = newSourceFile()
        val outputStream = RecordingOutputStream()
        `when`(contentResolver.openOutputStream(destinationUri, "wt"))
                .thenReturn(outputStream)

        // WHEN
        dialog.saveFile(sourceFile, destinationUri)

        // THEN
        assertArrayEquals(sourceFile.readBytes(), outputStream.toByteArray())
        assertEquals(listOf("flush", "close"), outputStream.events)
        val order = inOrder(contentResolver)
        order.verify(contentResolver).openOutputStream(destinationUri, "wt")
        verifyNoMoreInteractions(contentResolver)
    }

    @Test
    fun `saveFile - should fall back for unsupported or invalid wt mode`() {
        for (error in listOf(UnsupportedOperationException("wt"), IllegalArgumentException("wt"))) {
            // GIVEN
            val outputStream = RecordingOutputStream()
            val resolver = mock(ContentResolver::class.java)
            val activity = mock(Activity::class.java)
            `when`(activity.contentResolver).thenReturn(resolver)
            `when`(resolver.openOutputStream(destinationUri, "wt")).thenAnswer { throw error }
            `when`(resolver.openOutputStream(destinationUri, "w")).thenReturn(outputStream)
            val sourceFile = newSourceFile()

            // WHEN
            FileDialog(activity).saveFile(sourceFile, destinationUri)

            // THEN
            assertArrayEquals(sourceFile.readBytes(), outputStream.toByteArray())
            assertEquals(listOf("flush", "close"), outputStream.events)
            val order = inOrder(resolver)
            order.verify(resolver).openOutputStream(destinationUri, "wt")
            order.verify(resolver).openOutputStream(destinationUri, "w")
            verifyNoMoreInteractions(resolver)
        }
    }

    @Test
    fun `saveFile - should fail without retrying when wt returns null`() {
        // GIVEN
        val sourceFile = newSourceFile()
        `when`(contentResolver.openOutputStream(destinationUri, "wt")).thenReturn(null)

        // WHEN
        val error = assertThrows(IOException::class.java) {
            dialog.saveFile(sourceFile, destinationUri)
        }

        // THEN
        assertEquals("openOutputStream returned null", error.message)
        val order = inOrder(contentResolver)
        order.verify(contentResolver).openOutputStream(destinationUri, "wt")
        verifyNoMoreInteractions(contentResolver)
    }

    @Test
    fun `saveFile - should fail when the fallback returns null`() {
        // GIVEN
        val sourceFile = newSourceFile()
        `when`(contentResolver.openOutputStream(destinationUri, "wt"))
                .thenThrow(FileNotFoundException("wt"))
        `when`(contentResolver.openOutputStream(destinationUri, "w")).thenReturn(null)

        // WHEN
        val error = assertThrows(IOException::class.java) {
            dialog.saveFile(sourceFile, destinationUri)
        }

        // THEN
        assertEquals("openOutputStream returned null", error.message)
        val order = inOrder(contentResolver)
        order.verify(contentResolver).openOutputStream(destinationUri, "wt")
        order.verify(contentResolver).openOutputStream(destinationUri, "w")
        verifyNoMoreInteractions(contentResolver)
    }

    @Test
    fun `saveFile - should propagate other opening errors without retrying`() {
        for (error in listOf(IOException("Provider failed"), SecurityException("Access denied"))) {
            // GIVEN
            val resolver = mock(ContentResolver::class.java)
            val activity = mock(Activity::class.java)
            `when`(activity.contentResolver).thenReturn(resolver)
            `when`(resolver.openOutputStream(destinationUri, "wt")).thenAnswer { throw error }
            val sourceFile = newSourceFile()

            // WHEN
            val thrown = assertThrows(error.javaClass) {
                FileDialog(activity).saveFile(sourceFile, destinationUri)
            }

            // THEN
            assertSame(error, thrown)
            val order = inOrder(resolver)
            order.verify(resolver).openOutputStream(destinationUri, "wt")
            verifyNoMoreInteractions(resolver)
        }
    }

    private class RecordingOutputStream : ByteArrayOutputStream() {
        val events = mutableListOf<String>()

        override fun flush() {
            events.add("flush")
            super.flush()
        }

        override fun close() {
            events.add("close")
            super.close()
        }
    }
}
