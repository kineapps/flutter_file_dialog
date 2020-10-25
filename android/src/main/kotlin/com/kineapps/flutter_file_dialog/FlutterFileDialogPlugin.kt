// Copyright (c) 2020 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.kineapps.flutter_file_dialog

import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class FlutterFileDialogPlugin : FlutterPlugin, ActivityAware, MethodCallHandler {
    private var fileDialog: FileDialog? = null
    private var pluginBinding: FlutterPlugin.FlutterPluginBinding? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var methodChannel: MethodChannel? = null

    // V1 only
    private var registrar: Registrar? = null

    companion object {
        const val LOG_TAG = "FlutterFileDialogPlugin"
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            Log.d(LOG_TAG, "registerWith")
            if (registrar.activity() != null) {
                val plugin = FlutterFileDialogPlugin()
                plugin.doOnAttachedToEngine(registrar.messenger())
                plugin.doOnAttachedToActivity(null, registrar)
            }
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(LOG_TAG, "onAttachedToEngine - IN")

        if (pluginBinding != null) {
            Log.w(LOG_TAG, "onAttachedToEngine - already attached")
        }

        pluginBinding = binding

        val messenger = pluginBinding?.binaryMessenger
        doOnAttachedToEngine(messenger!!)

        Log.d(LOG_TAG, "onAttachedToEngine - OUT")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(LOG_TAG, "onDetachedFromEngine")
        doOnDetachedFromEngine()
    }

    // note: this may be called multiple times on app startup
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.d(LOG_TAG, "onAttachedToActivity")
        doOnAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        Log.d(LOG_TAG, "onDetachedFromActivity")
        doOnDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Log.d(LOG_TAG, "onReattachedToActivityForConfigChanges")
        doOnAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Log.d(LOG_TAG, "onDetachedFromActivityForConfigChanges")
        doOnDetachedFromActivity()
    }

    private fun doOnAttachedToEngine(messenger: BinaryMessenger) {
        Log.d(LOG_TAG, "doOnAttachedToEngine - IN")

        methodChannel = MethodChannel(messenger, "flutter_file_dialog")
        methodChannel?.setMethodCallHandler(this)

        Log.d(LOG_TAG, "doOnAttachedToEngine - OUT")
    }

    private fun doOnDetachedFromEngine() {
        Log.d(LOG_TAG, "doOnDetachedFromEngine - IN")

        if (pluginBinding == null) {
            Log.w(LOG_TAG, "doOnDetachedFromEngine - already detached")
        }
        pluginBinding = null

        methodChannel?.setMethodCallHandler(null)
        methodChannel = null

        Log.d(LOG_TAG, "doOnDetachedFromEngine - OUT")
    }

    private fun doOnAttachedToActivity(activityBinding: ActivityPluginBinding?,
                                       registrar: Registrar? = null) {
        Log.d(LOG_TAG, "doOnAttachedToActivity - IN")

        this.activityBinding = activityBinding
        this.registrar = registrar

        Log.d(LOG_TAG, "doOnAttachedToActivity - OUT")
    }

    private fun doOnDetachedFromActivity() {
        Log.d(LOG_TAG, "doOnDetachedFromActivity - IN")

        if (fileDialog != null) {
            activityBinding?.removeActivityResultListener(fileDialog!!)
            fileDialog = null
        }
        activityBinding = null

        Log.d(LOG_TAG, "doOnDetachedFromActivity - OUT")
    }


    override fun onMethodCall(call: MethodCall, result: Result) {
        Log.d(LOG_TAG, "onMethodCall - IN , method=${call.method}")
        if (fileDialog == null) {
            if (!createFileDialog()) {
                result.error("init_failed", "Not attached", null)
                return
            }
        }
        when (call.method) {
            "pickFile" -> fileDialog!!.pickFile(
                    result,
                    fileExtensionsFilter = parseMethodCallArrayArgument(call, "fileExtensionsFilter"),
                    mimeTypesFilter = parseMethodCallArrayArgument(call, "mimeTypesFilter"),
                    localOnly = call.argument("localOnly") as Boolean? == true
            )
            "saveFile" -> fileDialog!!.saveFile(
                    result,
                    sourceFilePath = call.argument("sourceFilePath"),
                    data = call.argument("data"),
                    fileName = call.argument("fileName"),
                    mimeTypesFilter = parseMethodCallArrayArgument(call, "mimeTypesFilter"),
                    localOnly = call.argument("localOnly") as Boolean? == true
            )
            else -> result.notImplemented()
        }
    }

    private fun createFileDialog(): Boolean {
        Log.d(LOG_TAG, "createFileDialog - IN")

        var fileDialog: FileDialog? = null
        if (registrar != null) {
            // V1 embedding
            fileDialog = FileDialog(
                    activity = registrar!!.activity()
            )
            registrar!!.addActivityResultListener(fileDialog)
        } else if (activityBinding != null) {
            // V2 embedding
            fileDialog = FileDialog(
                    activity = activityBinding!!.activity
            )
            activityBinding!!.addActivityResultListener(fileDialog)
        }
        this.fileDialog = fileDialog

        Log.d(LOG_TAG, "createFileDialog - OUT")

        return fileDialog != null
    }

    private fun parseMethodCallArrayArgument(call: MethodCall, arg: String): Array<String>? {
        if (call.hasArgument(arg)) {
            return call.argument<ArrayList<String>>(arg)?.toTypedArray()
        }
        return null
    }
}
