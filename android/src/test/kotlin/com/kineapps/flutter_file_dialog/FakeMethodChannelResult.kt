// Copyright (c) 2026 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.kineapps.flutter_file_dialog

import android.os.Looper
import io.flutter.plugin.common.MethodChannel
import org.robolectric.Shadows.shadowOf

/** Test double recording [MethodChannel.Result] completions. */
internal class FakeMethodChannelResult : MethodChannel.Result {
    var successCount = 0
    var errorCount = 0
    var notImplementedCount = 0
    var lastSuccessValue: Any? = null
    var lastErrorCode: String? = null
    var lastErrorMessage: String? = null
    var lastErrorDetails: Any? = null
    var completionThread: Thread? = null

    val completionCount get() = successCount + errorCount + notImplementedCount

    override fun success(result: Any?) {
        completionThread = Thread.currentThread()
        successCount++
        lastSuccessValue = result
    }

    override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
        completionThread = Thread.currentThread()
        errorCount++
        lastErrorCode = errorCode
        lastErrorMessage = errorMessage
        lastErrorDetails = errorDetails
    }

    override fun notImplemented() {
        completionThread = Thread.currentThread()
        notImplementedCount++
    }

    /**
     * Runs the Robolectric main looper until the result is completed. The test
     * thread is the main looper thread, so a reply posted to Dispatchers.Main
     * runs only while this loops.
     */
    fun awaitCompletion(timeoutMillis: Long = 5_000) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (completionCount == 0) {
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("Result not completed")
            }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
    }
}
