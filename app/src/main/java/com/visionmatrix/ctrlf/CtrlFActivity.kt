package com.visionmatrix.ctrlf

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.visionmatrix.ctrlf.databinding.ActivityCtrlfBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CtrlFActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCtrlfBinding
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var detector: YOLOv8Detector
    private var targetClass: String? = null
    private var isProcessing = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { if (it) startCamera() else finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCtrlfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("CtrlF", ">>> CtrlFActivity 启动")

        detector = YOLOv8Detector(this)
        lifecycleScope.launch { 
            Log.d("CtrlF", "正在初始化 YOLO 模型...")
            detector.initialize() 
            Log.d("CtrlF", "YOLO 模型初始化指令已发送")
        }

        binding.searchButton.setOnClickListener {
            val text = binding.searchEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                targetClass = text
                Toast.makeText(this, "正在搜索: $text", Toast.LENGTH_SHORT).show()
                Log.d("CtrlF", "用户设置搜索目标: $text")
            } else {
                targetClass = null
                binding.overlayView.clearDetections()
                Log.d("CtrlF", "清除搜索目标")
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor) { imageProxy -> analyzeImage(imageProxy) } }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                Log.d("CtrlF", "相机与分析器绑定成功")
            } catch (e: Exception) { Log.e("CtrlF", "相机绑定失败", e) }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeImage(imageProxy: ImageProxy) {
        // 如果没有输入搜索目标，直接跳过分析，节省性能
        if (targetClass == null) {
            imageProxy.close()
            return
        }

        if (isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true
        lifecycleScope.launch {
            try {
                val bitmap = imageProxyToBitmap(imageProxy)
                if (bitmap != null) {
                    val detections = detector.detect(bitmap, targetClass, 0.25f)
                    
                    withContext(Dispatchers.Main) {
                        // 只有检测到结果时才打印
                        if (!detections.isNullOrEmpty()) {
                            Log.d("CtrlF", "找到目标！数量: ${detections.size}")
                        }
                        binding.overlayView.setDetections(detections, bitmap.width, bitmap.height)
                    }
                }
            } catch (e: Exception) {
                Log.e("CtrlF", "图像分析循环异常", e)
            } finally {
                imageProxy.close()
                isProcessing = false
            }
        }
    }

    private suspend fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            
            val yuvImage = android.graphics.YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height), 90, out)
            
            BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        } catch (e: Exception) { null }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        detector.release()
    }
}
