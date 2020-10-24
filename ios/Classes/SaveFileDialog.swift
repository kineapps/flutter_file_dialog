// Copyright (c) 2020 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import UIKit

class SaveFileDialog: NSObject, UIDocumentPickerDelegate {
    private var flutterResult: FlutterResult?
    private var params: SaveFileDialogParams?
    private var tempFileUrl: URL?

    deinit {
        writeLog("")
        deleteTempFile()
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

        var fileUrl = URL(fileURLWithPath: sourceFilePath)

        // check that source file exists
        if !FileManager.default.fileExists(atPath: sourceFilePath) {
            result(FlutterError(code: "file_not_found",
                                message: "File not found: '\(sourceFilePath)'",
                                details: nil)
            )
            return
        }

        // if file name was specified, create a temp file with the requested file name
        if params.fileName != nil {
            let directory = NSTemporaryDirectory()
            tempFileUrl = NSURL.fileURL(withPathComponents: [directory, params.fileName!])

            do {
                // overwrite existing file
                if FileManager.default.fileExists(atPath: tempFileUrl!.path) {
                    try FileManager.default.removeItem(at: tempFileUrl!)
                }

                writeLog("Copying \(fileUrl) to \(tempFileUrl!)")
                try FileManager.default.copyItem(at: fileUrl, to: tempFileUrl!)
            } catch {
                writeLog(error.localizedDescription)
                result(FlutterError(code: "creating_temp_file_failed",
                                    message: error.localizedDescription,
                                    details: nil)
                )
                return
            }
            fileUrl = tempFileUrl!
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

    private func deleteTempFile() {
        if tempFileUrl != nil {
            do {
                if FileManager.default.fileExists(atPath: tempFileUrl!.path) {
                    writeLog("Deleting temp file \(tempFileUrl!)")
                    try FileManager.default.removeItem(at: tempFileUrl!)
                }
                tempFileUrl = nil
            } catch {
                writeLog(error.localizedDescription)
            }
        }
    }

    // MARK: - UIDocumentPickerDelegate

    public func documentPicker(_: UIDocumentPickerViewController, didPickDocumentAt url: URL) {
        writeLog("didPickDocumentAt")
        deleteTempFile()
        flutterResult?(url.path)
    }

    // MARK: - UIDocumentPickerDelegate

    public func documentPicker(_: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        writeLog("didPickDocumentsAt")
        deleteTempFile()
        flutterResult?(urls[0].path)
    }

    // MARK: - UIDocumentPickerDelegate

    public func documentPickerWasCancelled(_: UIDocumentPickerViewController) {
        writeLog("documentPickerWasCancelled")
        deleteTempFile()
        flutterResult?(nil)
    }
}
