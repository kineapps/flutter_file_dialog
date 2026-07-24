// Copyright (c) 2026 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.kineapps.flutter_file_dialog

import android.app.Activity
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.PluginRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.io.File

class FlutterFileDialogPluginTest {

    private fun newBinding(): ActivityPluginBinding {
        val binding = mock(ActivityPluginBinding::class.java)
        `when`(binding.activity).thenReturn(mock(Activity::class.java))
        return binding
    }

    private fun attachedFileDialog(plugin: FlutterFileDialogPlugin, binding: ActivityPluginBinding): FileDialog {
        plugin.onAttachedToActivity(binding)
        val captor = ArgumentCaptor.forClass(PluginRegistry.ActivityResultListener::class.java)
        verify(binding).addActivityResultListener(captor.capture())
        return captor.value as FileDialog
    }

    private fun startSaveFile(fileDialog: FileDialog): FakeMethodChannelResult {
        val sourceFile = File.createTempFile("plugin_test", ".txt").apply {
            writeText("data")
            deleteOnExit()
        }
        val result = FakeMethodChannelResult()
        fileDialog.saveFile(
                result = result,
                sourceFilePath = sourceFile.path,
                data = null,
                fileName = "file.txt",
                mimeTypesFilter = null,
                localOnly = false)
        return result
    }

    @Test
    fun `onAttachedToActivity - should unregister previous FileDialog and cancel its pending result`() {
        // GIVEN a plugin attached to an activity with a dialog result pending
        val plugin = FlutterFileDialogPlugin()
        val firstBinding = newBinding()
        val firstFileDialog = attachedFileDialog(plugin, firstBinding)
        val pending = startSaveFile(firstFileDialog)

        // WHEN the plugin is attached again without a detach in between
        val secondBinding = newBinding()
        plugin.onAttachedToActivity(secondBinding)

        // THEN the stale listener is removed from the old binding, its pending
        // result resolves as cancelled, and a new listener is registered
        verify(firstBinding).removeActivityResultListener(firstFileDialog)
        assertEquals(1, pending.successCount)
        assertNull(pending.lastSuccessValue)
        verify(secondBinding).addActivityResultListener(any())
    }

    @Test
    fun `onDetachedFromActivity - should unregister FileDialog and cancel its pending result`() {
        // GIVEN a plugin attached to an activity with a dialog result pending
        val plugin = FlutterFileDialogPlugin()
        val binding = newBinding()
        val fileDialog = attachedFileDialog(plugin, binding)
        val pending = startSaveFile(fileDialog)

        // WHEN
        plugin.onDetachedFromActivity()

        // THEN
        verify(binding).removeActivityResultListener(fileDialog)
        assertEquals(1, pending.successCount)
        assertNull(pending.lastSuccessValue)
    }
}
