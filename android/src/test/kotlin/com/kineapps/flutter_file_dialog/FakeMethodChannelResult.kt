// Copyright (c) 2026 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.kineapps.flutter_file_dialog

import io.flutter.plugin.common.MethodChannel

/** Test double recording [MethodChannel.Result] completions. */
internal class FakeMethodChannelResult : MethodChannel.Result {
    var successCount = 0
    var errorCount = 0
    var notImplementedCount = 0
    var lastSuccessValue: Any? = null
    var lastErrorCode: String? = null
    var lastErrorMessage: String? = null
    var lastErrorDetails: Any? = null

    val completionCount get() = successCount + errorCount + notImplementedCount

    override fun success(result: Any?) {
        successCount++
        lastSuccessValue = result
    }

    override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
        errorCount++
        lastErrorCode = errorCode
        lastErrorMessage = errorMessage
        lastErrorDetails = errorDetails
    }

    override fun notImplemented() {
        notImplementedCount++
    }
}
