package dev.yukineko.ekimemo_map

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.provider.Settings;
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.accessibility.AccessibilityManager
import androidx.annotation.RequiresApi
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel


class MainActivity: FlutterActivity() {
    private val CHANNEL = "dev.yukineko.ekimemo_map/native"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getCommitHash" -> {
                    result.success(BuildConfig.COMMIT_HASH)
                }
                "hasPermission" -> {
                    val am = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
                    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                    result.success(Settings.canDrawOverlays(this) && enabledServices.any { it.id.split("/").first() == context.packageName })
                }
                "setDebugPackageName" -> {
                    val packageName = call.argument<String>("packageName")
                    if (packageName != null) {
                        AssistantService.instance?.setDebugPackageName(packageName)
                        result.success(null)
                    } else {
                        result.error("INVALID_ARGUMENT", "packageName must be provided", null)
                    }
                }
//                TODO?: Remove this
//                "requestAccessibilityPermission" -> {
//                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
//                    startActivity(intent)
//                }
//                "requestOverlayPermission" -> {
//                    if (!Settings.canDrawOverlays(this)) {
//                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
//                        this.startActivity(intent)
//                    }
//                }
                "performTap" -> {
                    val x = call.argument<Double>("x")?.toFloat()
                    val y = call.argument<Double>("y")?.toFloat()
                    if (x != null && y != null) {
                        AssistantService.instance?.performTap(x, y)
                        result.success(null)
                    } else {
                        result.error("INVALID_ARGUMENT", "x and y must be provided", null)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}
