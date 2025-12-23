package com.visionmatrix.actioncards

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.visionmatrix.ctrlf.YOLOv8Detector
import com.visionmatrix.ctrlf.databinding.ActivityScenarioBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ScenarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScenarioBinding
    private lateinit var semanticMatcher: SemanticMatcher
    private lateinit var detector: YOLOv8Detector
    private var currentBitmap: Bitmap? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) processSelectedImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScenarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        semanticMatcher = SemanticMatcher(this)
        detector = YOLOv8Detector(this)
        
        lifecycleScope.launch(Dispatchers.IO) {
            semanticMatcher.init()
            detector.initialize()
        }

        binding.btnOpenGallery.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.tvResult.setOnClickListener {
            val currentText = binding.tvResult.text.toString()
            if (currentText.contains("TEXT") || currentText.contains("提取")) {
                runFullTextPipeline()
            }
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun processSelectedImage(uri: Uri) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            currentBitmap = bitmap
            binding.ivPreview.setImageBitmap(bitmap)
            analyzeImage(bitmap)
        } catch (e: Exception) { Log.e("Scenario", "Load image failed", e) }
    }

    private fun analyzeImage(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            val scene = semanticMatcher.analyzeScene(bitmap)
            withContext(Dispatchers.Main) {
                binding.tvResult.text = "识别到场景: $scene"
                
                if (scene == "OBJECT") {
                    runObjectDetection(bitmap)
                } else if (scene == "TEXT") {
                    binding.tvResult.text = "场景: $scene (点击此处提取并翻译文字)"
                }
            }
        }
    }

    private fun runObjectDetection(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            val results = detector.detect(bitmap, null, 0.15f)
            withContext(Dispatchers.Main) {
                if (!results.isNullOrEmpty()) {
                    // 修正：删除 chineseName，使用 className (英文)
                    val name = results[0].className
                    binding.tvResult.text = "分析结果: 发现物体【$name】"
                } else {
                    binding.tvResult.text = "分析结果: 发现未知物体"
                }
            }
        }
    }

    private fun runFullTextPipeline() {
        val bitmap = currentBitmap ?: return
        lifecycleScope.launch {
            try {
                binding.tvResult.text = "正在执行 OCR 识别..."
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val visionText = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                val originalText = visionText.text

                if (originalText.isBlank()) {
                    binding.tvResult.text = "未检测到文字"
                    return@launch
                }

                binding.tvResult.text = "识别结果:\n$originalText\n\n(正在尝试翻译...)"
                
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(TranslateLanguage.CHINESE).build()
                val translator = Translation.getClient(options)
                
                try {
                    translator.downloadModelIfNeeded().await()
                    val translatedText = translator.translate(originalText).await()
                    binding.tvResult.text = "原文:\n$originalText\n\n翻译:\n$translatedText"
                } catch (e: Exception) {
                    binding.tvResult.text = "文字提取成功，但翻译模型不可用\n\n原文:\n$originalText"
                }
            } catch (e: Exception) {
                binding.tvResult.text = "处理失败: ${e.localizedMessage}"
            }
        }
    }
}
