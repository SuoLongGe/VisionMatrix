package com.visionmatrix.ctrlf

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.ImageProxy
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import java.nio.ByteBuffer
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.visionmatrix.ctrlf.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private lateinit var detector: YOLOv8Detector
    private var targetClass: String? = null
    private var isProcessing = false
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(
                this,
                getString(R.string.camera_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化检测器
        detector = YOLOv8Detector(this)
        
        // 初始化模型
        lifecycleScope.launch {
            detector.initialize()
        }
        
        // 设置搜索按钮点击事件
        binding.searchButton.setOnClickListener {
            val searchText = binding.searchEditText.text.toString().trim()
            if (searchText.isNotEmpty()) {
                targetClass = searchText
                Toast.makeText(
                    this,
                    "开始搜索: $searchText",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                targetClass = null
                binding.overlayView.clearDetections()
            }
        }
        
        // 检查相机权限
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            baseContext, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            // 预览
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
            
            // 图像分析
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        analyzeImage(imageProxy)
                    }
                }
            
            // 选择后置摄像头
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // 解绑所有用例后重新绑定
                cameraProvider.unbindAll()
                
                // 绑定用例到相机
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                Toast.makeText(
                    this,
                    "相机启动失败: ${exc.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun analyzeImage(imageProxy: ImageProxy) {
        if (isProcessing || targetClass == null) {
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
                        binding.overlayView.setDetections(
                            detections,
                            bitmap.width,
                            bitmap.height
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "分析图像时出错", e)
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
            
            val yuvImage = android.graphics.YuvImage(
                nv21,
                ImageFormat.NV21,
                imageProxy.width,
                imageProxy.height,
                null
            )
            
            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
                90,
                out
            )
            
            val imageBytes = out.toByteArray()
            return@withContext BitmapFactory.decodeByteArray(
                imageBytes,
                0,
                imageBytes.size
            )
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "转换图像时出错", e)
            return@withContext null
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        detector.release()
    }
}

