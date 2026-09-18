// Copyright (c) 2026 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.kineapps.flutter_file_dialog

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.io.File

class FileDialogTest {

    private fun newDialog(): FileDialog = FileDialog(mock(Activity::class.java))

    /** An activity with no app to handle the dialog intents. */
    private fun newActivityWithoutDocumentsProvider(): Activity {
        val activity = mock(Activity::class.java)
        doThrow(ActivityNotFoundException("No Activity found to handle Intent"))
                .`when`(activity).startActivityForResult(any(Intent::class.java), anyInt())
        return activity
    }

    private fun newDialogWithoutDocumentsProvider(): FileDialog =
            FileDialog(newActivityWithoutDocumentsProvider())

    /** The request code the dialog passed to the activity's (failed) launch. */
    private fun launchedRequestCode(activity: Activity): Int {
        val captor = ArgumentCaptor.forClass(Int::class.java)
        verify(activity).startActivityForResult(any(Intent::class.java), captor.capture())
        return captor.value
    }

    private fun newSourceFile(): File =
            File.createTempFile("file_dialog_test", ".txt").apply {
                writeText("data")
                deleteOnExit()
            }

    private fun startSaveFile(dialog: FileDialog): FakeMethodChannelResult {
        val result = FakeMethodChannelResult()
        dialog.saveFile(
                result = result,
                sourceFilePath = newSourceFile().path,
                data = null,
                fileName = "file.txt",
                mimeTypesFilter = null,
                localOnly = false)
        return result
    }

    @Test
    fun `saveFile - should reply already_active when a dialog is already pending`() {
        // GIVEN
        val dialog = newDialog()
        val first = startSaveFile(dialog)

        // WHEN
        val second = startSaveFile(dialog)

        // THEN
        assertEquals(0, first.completionCount)
        assertEquals(1, second.errorCount)
        assertEquals("already_active", second.lastErrorCode)
    }

    @Test
    fun `onActivityResult - should complete cancelled dialog result exactly once`() {
        // GIVEN
        val dialog = newDialog()
        val result = startSaveFile(dialog)
        val requestCode = dialog.pendingRequestCode

        // WHEN
        val handled = dialog.onActivityResult(requestCode, Activity.RESULT_CANCELED, null)

        // THEN
        assertTrue(handled)
        assertEquals(1, result.successCount)
        assertNull(result.lastSuccessValue)
    }

    @Test
    fun `onActivityResult - should ignore re-delivery after the result is completed`() {
        // GIVEN
        val dialog = newDialog()
        val result = startSaveFile(dialog)
        val requestCode = dialog.pendingRequestCode
        dialog.onActivityResult(requestCode, Activity.RESULT_CANCELED, null)

        // WHEN
        val handled = dialog.onActivityResult(requestCode, Activity.RESULT_CANCELED, null)

        // THEN
        assertFalse(handled)
        assertEquals(1, result.completionCount)
    }

    @Test
    fun `onActivityResult - should not let a re-delivered old result complete a new dialog`() {
        // GIVEN a completed first dialog and a second dialog pending
        val dialog = newDialog()
        startSaveFile(dialog)
        val firstRequestCode = dialog.pendingRequestCode
        dialog.onActivityResult(firstRequestCode, Activity.RESULT_CANCELED, null)
        val second = startSaveFile(dialog)

        // WHEN the first launch's result is delivered again
        val handled = dialog.onActivityResult(firstRequestCode, Activity.RESULT_CANCELED, null)

        // THEN it is ignored and the new dialog's result stays pending
        assertFalse(handled)
        assertEquals(0, second.completionCount)
    }

    @Test
    fun `onActivityResult - should ignore a request code from another FileDialog instance`() {
        // GIVEN
        val staleDialog = newDialog()
        startSaveFile(staleDialog)
        val staleRequestCode = staleDialog.pendingRequestCode
        val dialog = newDialog()
        val result = startSaveFile(dialog)

        // WHEN
        val handled = dialog.onActivityResult(staleRequestCode, Activity.RESULT_CANCELED, null)

        // THEN
        assertFalse(handled)
        assertEquals(0, result.completionCount)
    }

    @Test
    fun `onActivityResult - should return false for an unknown request code`() {
        // GIVEN
        val dialog = newDialog()

        // WHEN
        val handled = dialog.onActivityResult(12345, Activity.RESULT_OK, null)

        // THEN
        assertFalse(handled)
    }

    @Test
    fun `cancelPendingResult - should complete pending result as cancelled`() {
        // GIVEN
        val dialog = newDialog()
        val result = startSaveFile(dialog)

        // WHEN
        dialog.cancelPendingResult()

        // THEN
        assertEquals(1, result.successCount)
        assertNull(result.lastSuccessValue)
    }

    @Test
    fun `cancelPendingResult - should release the active slot for a new dialog`() {
        // GIVEN
        val dialog = newDialog()
        startSaveFile(dialog)
        dialog.cancelPendingResult()

        // WHEN
        val second = startSaveFile(dialog)

        // THEN no already_active error; the new dialog owns the pending slot
        assertEquals(0, second.completionCount)
    }

    @Test
    fun `cancelPendingResult - should delete the temporary source file of a pending save`() {
        // GIVEN a pending saveFile that wrote its data to a temporary file
        val dialog = newDialog()
        val result = FakeMethodChannelResult()
        val fileNamePrefix = "cancel_temp_test_${System.nanoTime()}"
        dialog.saveFile(
                result = result,
                sourceFilePath = null,
                data = byteArrayOf(1, 2, 3),
                fileName = fileNamePrefix,
                mimeTypesFilter = null,
                localOnly = false)

        // WHEN
        dialog.cancelPendingResult()

        // THEN the result is cancelled and the temporary file is deleted
        assertEquals(1, result.successCount)
        assertNull(result.lastSuccessValue)
        val leftovers = File(System.getProperty("java.io.tmpdir"))
                .listFiles { file -> file.name.startsWith(fileNamePrefix) }
        assertEquals(0, leftovers?.size ?: 0)
    }

    @Test
    fun `cancelPendingResult - should do nothing when no result is pending`() {
        // GIVEN
        val dialog = newDialog()

        // WHEN / THEN (no exception, nothing to complete)
        dialog.cancelPendingResult()
    }

    @Test
    fun `saveFile - should reply file_not_found for a missing source file`() {
        // GIVEN
        val dialog = newDialog()
        val result = FakeMethodChannelResult()

        // WHEN
        dialog.saveFile(
                result = result,
                sourceFilePath = "/no/such/file.txt",
                data = null,
                fileName = "file.txt",
                mimeTypesFilter = null,
                localOnly = false)

        // THEN the incoming call's own result is completed and the slot is free
        assertEquals(1, result.errorCount)
        assertEquals("file_not_found", result.lastErrorCode)
        assertEquals(0, startSaveFile(dialog).completionCount)
    }

    @Test
    fun `saveFile - should reply invalid_arguments when fileName and data are missing`() {
        // GIVEN
        val dialog = newDialog()
        val result = FakeMethodChannelResult()

        // WHEN
        dialog.saveFile(
                result = result,
                sourceFilePath = null,
                data = null,
                fileName = null,
                mimeTypesFilter = null,
                localOnly = false)

        // THEN the call fails and the pending-dialog slot stays free
        assertEquals(1, result.errorCount)
        assertEquals("invalid_arguments", result.lastErrorCode)
        assertEquals(0, startSaveFile(dialog).completionCount)
    }

    @Test
    fun `saveFile - should release the pending slot when temp file creation fails`() {
        // GIVEN a fileName too short for File.createTempFile (throws)
        val dialog = newDialog()
        val result = FakeMethodChannelResult()

        // WHEN
        dialog.saveFile(
                result = result,
                sourceFilePath = null,
                data = byteArrayOf(1),
                fileName = "ab",
                mimeTypesFilter = null,
                localOnly = false)

        // THEN the call fails and a new dialog can be started (no already_active)
        assertEquals(1, result.errorCount)
        assertEquals("save_file_failed", result.lastErrorCode)
        assertEquals(0, startSaveFile(dialog).completionCount)
    }

    @Test
    fun `saveFile - should reply internal_error when no activity is available`() {
        // GIVEN
        val dialog = FileDialog(null)
        val result = FakeMethodChannelResult()

        // WHEN
        dialog.saveFile(
                result = result,
                sourceFilePath = newSourceFile().path,
                data = null,
                fileName = "file.txt",
                mimeTypesFilter = null,
                localOnly = false)

        // THEN
        assertEquals(1, result.errorCount)
        assertEquals("internal_error", result.lastErrorCode)
    }

    @Test
    fun `pickFile - should reply activity_not_found when no activity handles the intent`() {
        // GIVEN
        val dialog = newDialogWithoutDocumentsProvider()
        val result = FakeMethodChannelResult()

        // WHEN
        dialog.pickFile(
                result = result,
                fileExtensionsFilter = null,
                mimeTypesFilter = null,
                localOnly = false,
                copyFileToCacheDir = true)

        // THEN the call fails exactly once and the pending slot is released
        assertEquals(1, result.completionCount)
        assertEquals("activity_not_found", result.lastErrorCode)
        assertEquals(-1, dialog.pendingRequestCode)
    }

    @Test
    fun `pickFile - should ignore the RESULT_CANCELED Android delivers for a failed start`() {
        // GIVEN a launch that threw ActivityNotFoundException
        val activity = newActivityWithoutDocumentsProvider()
        val dialog = FileDialog(activity)
        val result = FakeMethodChannelResult()
        dialog.pickFile(
                result = result,
                fileExtensionsFilter = null,
                mimeTypesFilter = null,
                localOnly = false,
                copyFileToCacheDir = true)
        val requestCode = launchedRequestCode(activity)

        // WHEN that launch's cancelled result arrives anyway
        val handled = dialog.onActivityResult(requestCode, Activity.RESULT_CANCELED, null)

        // THEN it is not handled and the already-failed result is untouched
        assertFalse(handled)
        assertEquals(1, result.completionCount)
        assertEquals("activity_not_found", result.lastErrorCode)
    }

    @Test
    fun `saveFile - should reply activity_not_found and delete the temporary source file`() {
        // GIVEN a saveFile that writes its data to a temporary file
        val dialog = newDialogWithoutDocumentsProvider()
        val result = FakeMethodChannelResult()
        val fileNamePrefix = "not_found_temp_test_${System.nanoTime()}"

        // WHEN
        dialog.saveFile(
                result = result,
                sourceFilePath = null,
                data = byteArrayOf(1, 2, 3),
                fileName = fileNamePrefix,
                mimeTypesFilter = null,
                localOnly = false)

        // THEN the call fails once, the slot is free and the temp file is gone
        assertEquals(1, result.completionCount)
        assertEquals("activity_not_found", result.lastErrorCode)
        assertEquals(-1, dialog.pendingRequestCode)
        val leftovers = File(System.getProperty("java.io.tmpdir"))
                .listFiles { file -> file.name.startsWith(fileNamePrefix) }
        assertEquals(0, leftovers?.size ?: 0)
    }
}
