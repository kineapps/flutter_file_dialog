// Copyright (c) 2020 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// https://github.com/flutter/flutter/issues/13204
func writeLog(_ message: String) {
    NSLog("\n" + message)
    //print(message)
}
