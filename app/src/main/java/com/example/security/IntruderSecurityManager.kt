package com.example.security

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.IntruderRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * مدير سجل الحماية والتقاط صور المتلصصين وسارقي الهاتف سراً (Intruder Silent Capture)
 * يلتقط صورة بالكاميرا الأمامية في صمت وبدون صوت أو فلاش عند محاولة لمس الهاتف أثناء الحراسة
 * أو إدخال رمز الأمان بشكل خاطئ مرتين متتاليتين.
 */
class IntruderSecurityManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "IntruderSecurityManager"
        private const val PREFS_NAME = "cortex_intruder_records_prefs"
        private const val KEY_RECORDS_JSON = "intruder_records_json"

        @Volatile
        private var instance: IntruderSecurityManager? = null

        fun getInstance(context: Context): IntruderSecurityManager {
            return instance ?: synchronized(this) {
                instance ?: IntruderSecurityManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _intruderRecords = MutableStateFlow<List<IntruderRecord>>(emptyList())
    val intruderRecords: StateFlow<List<IntruderRecord>> = _intruderRecords.asStateFlow()

    private val _latestAlert = MutableStateFlow<IntruderRecord?>(null)
    val latestAlert: StateFlow<IntruderRecord?> = _latestAlert.asStateFlow()

    private val _hasNewIntruderAlert = MutableStateFlow(false)
    val hasNewIntruderAlert: StateFlow<Boolean> = _hasNewIntruderAlert.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        val raw = prefs.getString(KEY_RECORDS_JSON, null) ?: return
        try {
            val arr = JSONArray(raw)
            val list = mutableListOf<IntruderRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    IntruderRecord(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        reasonAr = obj.optString("reasonAr", "محاولة تطفل على الهاتف"),
                        photoPath = obj.optString("photoPath", ""),
                        formattedDate = obj.optString("formattedDate", ""),
                        formattedTime = obj.optString("formattedTime", "")
                    )
                )
            }
            _intruderRecords.value = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading intruder records", e)
        }
    }

    private fun saveRecords() {
        try {
            val arr = JSONArray()
            _intruderRecords.value.forEach { record ->
                val obj = JSONObject().apply {
                    put("id", record.id)
                    put("timestamp", record.timestamp)
                    put("reasonAr", record.reasonAr)
                    put("photoPath", record.photoPath)
                    put("formattedDate", record.formattedDate)
                    put("formattedTime", record.formattedTime)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_RECORDS_JSON, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving intruder records", e)
        }
    }

    /**
     * يلتقط صورة المتلصص بالكاميرا الأمامية بهدوء تام ويسجلها في سجل الحماية.
     */
    fun captureIntruderSilent(reasonAr: String) {
        scope.launch {
            val date = Date()
            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.forLanguageTag("ar"))
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.forLanguageTag("ar"))
            val formattedDate = dateFormat.format(date)
            val formattedTime = timeFormat.format(date)

            val photoDir = File(context.filesDir, "intruder_photos").apply { mkdirs() }
            val photoFile = File(photoDir, "intruder_${System.currentTimeMillis()}.jpg")

            val hasCameraPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCameraPermission) {
                executeFrontCameraCapture(photoFile) { savedPath ->
                    val record = IntruderRecord(
                        timestamp = date.time,
                        reasonAr = reasonAr,
                        photoPath = savedPath,
                        formattedDate = formattedDate,
                        formattedTime = formattedTime
                    )
                    addRecord(record)
                }
            } else {
                // If permission is not yet granted, still record the intrusion incident
                val record = IntruderRecord(
                    timestamp = date.time,
                    reasonAr = reasonAr,
                    photoPath = "",
                    formattedDate = formattedDate,
                    formattedTime = formattedTime
                )
                addRecord(record)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun executeFrontCameraCapture(destinationFile: File, onComplete: (String) -> Unit) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            onComplete("")
            return
        }

        try {
            val frontCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (frontCameraId == null) {
                onComplete("")
                return
            }

            val thread = HandlerThread("SilentIntruderThread").apply { start() }
            val backgroundHandler = Handler(thread.looper)

            val imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
            var cameraDeviceRef: CameraDevice? = null

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        FileOutputStream(destinationFile).use { fos ->
                            fos.write(bytes)
                        }
                        onComplete(destinationFile.absolutePath)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving intruder photo", e)
                        onComplete("")
                    } finally {
                        image.close()
                        reader.close()
                        try {
                            cameraDeviceRef?.close()
                        } catch (_: Exception) {}
                        thread.quitSafely()
                    }
                }
            }, backgroundHandler)

            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDeviceRef = camera
                    try {
                        val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                            addTarget(imageReader.surface)
                            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                        }

                        @Suppress("DEPRECATION")
                        camera.createCaptureSession(
                            listOf(imageReader.surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    try {
                                        session.capture(captureBuilder.build(), null, backgroundHandler)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Capture failed", e)
                                        camera.close()
                                        thread.quitSafely()
                                        onComplete("")
                                    }
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    camera.close()
                                    thread.quitSafely()
                                    onComplete("")
                                }
                            },
                            backgroundHandler
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating capture request", e)
                        camera.close()
                        thread.quitSafely()
                        onComplete("")
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    thread.quitSafely()
                    onComplete("")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    thread.quitSafely()
                    onComplete("")
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed front camera capture", e)
            onComplete("")
        }
    }

    private fun addRecord(record: IntruderRecord) {
        val current = _intruderRecords.value.toMutableList()
        current.add(0, record)
        _intruderRecords.value = current
        _latestAlert.value = record
        _hasNewIntruderAlert.value = true
        saveRecords()
    }

    fun dismissLatestAlert() {
        _hasNewIntruderAlert.value = false
        _latestAlert.value = null
    }

    fun deleteRecord(recordId: String) {
        val current = _intruderRecords.value.toMutableList()
        val found = current.find { it.id == recordId }
        if (found != null) {
            if (found.photoPath.isNotBlank()) {
                try {
                    File(found.photoPath).delete()
                } catch (_: Exception) {}
            }
            current.remove(found)
            _intruderRecords.value = current
            saveRecords()
        }
    }

    fun clearAllRecords() {
        _intruderRecords.value.forEach { record ->
            if (record.photoPath.isNotBlank()) {
                try {
                    File(record.photoPath).delete()
                } catch (_: Exception) {}
            }
        }
        _intruderRecords.value = emptyList()
        _hasNewIntruderAlert.value = false
        _latestAlert.value = null
        saveRecords()
    }
}
