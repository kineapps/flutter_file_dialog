// Copyright (c) 2026 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.kineapps.flutter_file_dialog

import java.io.ByteArrayOutputStream

/** In-memory output stream recording its flush and close calls. */
internal class RecordingOutputStream : ByteArrayOutputStream() {
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
