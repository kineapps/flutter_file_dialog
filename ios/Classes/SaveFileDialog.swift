// Copyright (c) 2020 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import UIKit

class SaveFileDialog: NSObject, UIDocumentPickerDelegate {
    private var flutterResult: FlutterResult?
    private var params: SaveFileDialogParams?

    deinit {
        writeLog("SaveFileDialog.deinit")
    }

    func saveFile(_ params: SaveFileDialogParams, result: @escaping FlutterResult) {
        flutterResult = result
        self.params = params

        // get source file URL
        guard let sourceFilePath = params.sourceFilePath else {
            result(FlutterError(code: "invalid_arguments",
                                message: "Missing 'sourceFilePath'",
                                details: nil)
            )
            return
        }
        let fileUrl = URL(fileURLWithPath: sourceFilePath)

        // check that source file exists
        if !FileManager.default.fileExists(atPath: sourceFilePath) {
            result(FlutterError(code: "file_not_found",
                                message: "File not found: '\(sourceFilePath)'",
                                details: nil)
            )
            return
        }

        // get parent view controller
        guard let parentViewController = UIApplication.shared.keyWindow?.rootViewController else {
            result(FlutterError(code: "fatal",
                                message: "Getting rootViewController failed",
                                details: nil)
            )
            return
        }

        // create document picker
        let documentPickerViewController = UIDocumentPickerViewController(url: fileUrl, in: .exportToService)
        documentPickerViewController.delegate = self

        // show dialog
        parentViewController.present(documentPickerViewController, animated: true, completion: nil)
    }

    // MARK: - UIDocumentPickerDelegate

    public func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentAt url: URL) {
        writeLog("didPickDocumentAt")
        flutterResult?(url.path)
    }

    // MARK: - UIDocumentPickerDelegate

    public func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        writeLog("didPickDocumentsAt")
        flutterResult?(urls[0].path)
    }

    // MARK: - UIDocumentPickerDelegate

    public func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        writeLog("documentPickerWasCancelled")
        flutterResult?(nil)
    }
}
