// Copyright (c) 2026 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.kineapps.flutter_file_dialog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SingleCompletionResultTest {

    @Test
    fun `success - should delegate exactly once with the right value`() {
        // GIVEN
        val fake = FakeMethodChannelResult()
        val result = SingleCompletionResult(fake)

        // WHEN
        result.success("file/path")

        // THEN
        assertEquals(1, fake.successCount)
        assertEquals("file/path", fake.lastSuccessValue)
    }

    @Test
    fun `success - should ignore a second success`() {
        // GIVEN
        val fake = FakeMethodChannelResult()
        val result = SingleCompletionResult(fake)
        result.success("first")

        // WHEN
        result.success("second")

        // THEN
        assertEquals(1, fake.successCount)
        assertEquals("first", fake.lastSuccessValue)
    }

    @Test
    fun `success - should be ignored after error`() {
        // GIVEN
        val fake = FakeMethodChannelResult()
        val result = SingleCompletionResult(fake)
        result.error("some_error", "message", null)

        // WHEN
        result.success("value")

        // THEN
        assertEquals(1, fake.errorCount)
        assertEquals(0, fake.successCount)
    }

    @Test
    fun `error - should delegate exactly once with the right arguments`() {
        // GIVEN
        val fake = FakeMethodChannelResult()
        val result = SingleCompletionResult(fake)

        // WHEN
        result.error("some_error", "message", "details")

        // THEN
        assertEquals(1, fake.errorCount)
        assertEquals("some_error", fake.lastErrorCode)
        assertEquals("message", fake.lastErrorMessage)
        assertEquals("details", fake.lastErrorDetails)
    }

    @Test
    fun `error - should be ignored after success`() {
        // GIVEN
        val fake = FakeMethodChannelResult()
        val result = SingleCompletionResult(fake)
        result.success(null)

        // WHEN
        result.error("some_error", "message", null)

        // THEN
        assertEquals(1, fake.successCount)
        assertNull(fake.lastSuccessValue)
        assertEquals(0, fake.errorCount)
    }

    @Test
    fun `notImplemented - should delegate on first call`() {
        // GIVEN
        val fake = FakeMethodChannelResult()
        val result = SingleCompletionResult(fake)

        // WHEN
        result.notImplemented()

        // THEN
        assertEquals(1, fake.notImplementedCount)
    }

    @Test
    fun `notImplemented - should be ignored after success`() {
        // GIVEN
        val fake = FakeMethodChannelResult()
        val result = SingleCompletionResult(fake)
        result.success("value")

        // WHEN
        result.notImplemented()

        // THEN
        assertEquals(1, fake.successCount)
        assertEquals(0, fake.notImplementedCount)
    }

    @Test
    fun `notImplemented - should suppress subsequent success and error`() {
        // GIVEN
        val fake = FakeMethodChannelResult()
        val result = SingleCompletionResult(fake)
        result.notImplemented()

        // WHEN
        result.success("value")
        result.error("some_error", null, null)

        // THEN
        assertEquals(1, fake.notImplementedCount)
        assertEquals(0, fake.successCount)
        assertEquals(0, fake.errorCount)
    }
}
