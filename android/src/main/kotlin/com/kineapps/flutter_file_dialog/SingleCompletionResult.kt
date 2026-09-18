// Copyright (c) 2026 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.kineapps.flutter_file_dialog

import android.util.Log
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.atomic.AtomicBoolean

private const val LOG_TAG = "SingleCompletionResult"

/**
 * A [MethodChannel.Result] wrapper that guarantees the wrapped result is
 * completed at most once. Subsequent completion attempts are logged and
 * ignored instead of throwing IllegalStateException "Reply already submitted".
 *
 * As a last line of defence the wrapped result's own "Reply already
 * submitted" IllegalStateException is also caught and logged, in case the raw
 * result was already replied to by a path that bypassed this wrapper (for
 * example the method-call handler replying to an escaped exception).
 */
internal class SingleCompletionResult(
        private val inner: MethodChannel.Result
) : MethodChannel.Result {

    private val submitted = AtomicBoolean(false)

    override fun success(result: Any?) {
        if (!submitted.compareAndSet(false, true)) {
            Log.w(LOG_TAG, "Result already submitted, ignoring success()")
            return
        }
        try {
            inner.success(result)
        } catch (e: IllegalStateException) {
            Log.w(LOG_TAG, "Inner result rejected success()", e)
        }
    }

    override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
        if (!submitted.compareAndSet(false, true)) {
            Log.w(LOG_TAG, "Result already submitted, ignoring error($errorCode)")
            return
        }
        try {
            inner.error(errorCode, errorMessage, errorDetails)
        } catch (e: IllegalStateException) {
            Log.w(LOG_TAG, "Inner result rejected error($errorCode)", e)
        }
    }

    override fun notImplemented() {
        if (!submitted.compareAndSet(false, true)) {
            Log.w(LOG_TAG, "Result already submitted, ignoring notImplemented()")
            return
        }
        try {
            inner.notImplemented()
        } catch (e: IllegalStateException) {
            Log.w(LOG_TAG, "Inner result rejected notImplemented()", e)
        }
    }
}
