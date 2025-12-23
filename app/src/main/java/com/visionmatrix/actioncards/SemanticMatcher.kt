package com.visionmatrix.actioncards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import kotlin.math.sqrt

class SemanticMatcher(private val context: Context) {

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private val actionVectors = HashMap<String, FloatArray>()

    // 模型输入元数据
    private var inputName: String = "pixel_values"
    private var actualInputShape: LongArray = longArrayOf(1, 3, 256, 256)
    private var inputJavaType: OnnxJavaType = OnnxJavaType.FLOAT

    fun init() {
        try {
            Log.d("SemanticMatcher", ">>> 启动智能分析引擎...")
            val subDir = "mobileclip_s0"
            val modelFileName = "vision_model_uint8.onnx"
            val jsonFileName = "action_embeddings.json"

            ortEnv = OrtEnvironment.getEnvironment()
            val modelPath = "$subDir/$modelFileName"
            val modelBytes = context.assets.open(modelPath).readBytes()
            ortSession = ortEnv?.createSession(modelBytes)

            val inputInfo = ortSession!!.inputInfo
            inputName = inputInfo.keys.iterator().next()
            val firstInput = inputInfo.values.iterator().next()
            val tensorInfo = firstInput.info as TensorInfo
            
            val rawShape = tensorInfo.shape
            actualInputShape = rawShape.copyOf()
            for (i in actualInputShape.indices) {
                if (actualInputShape[i] < 0) actualInputShape[i] = 1 
            }
            
            inputJavaType = tensorInfo.type

            Log.d("SemanticMatcher", "✅ 模型加载成功: $modelFileName")

            // 加载向量库并进行归一化
            val jsonPath = "$subDir/$jsonFileName"
            val jsonString = context.assets.open(jsonPath).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            jsonObject.keys().forEach { key ->
                val jsonArray = jsonObject.getJSONArray(key)
                val rawArray = FloatArray(jsonArray.length()) { jsonArray.getDouble(it).toFloat() }
                // 关键修正：确保 JSON 里的向量也是归一化的
                actionVectors[key] = normalize(rawArray)
            }
            Log.d("SemanticMatcher", "✅ 向量库加载完成，已归一化 ${actionVectors.size} 个场景向量")
        } catch (e: Exception) {
            Log.e("SemanticMatcher", "❌ 初始化失败: ${e.message}", e)
        }
    }

    fun analyzeScene(bitmap: Bitmap): String {
        if (ortSession == null) return "ERROR_INIT"

        try {
            val isNCHW = actualInputShape[1] == 3L || actualInputShape[1] == 1L
            val targetH = if (isNCHW) actualInputShape[2].toInt() else actualInputShape[1].toInt()
            val targetW = if (isNCHW) actualInputShape[3].toInt() else actualInputShape[2].toInt()
            
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            
            val inputTensor = if (inputJavaType == OnnxJavaType.UINT8) {
                preprocessUint8(resizedBitmap, isNCHW)
            } else {
                preprocessFloat(resizedBitmap, isNCHW)
            }

            val inputs = Collections.singletonMap(inputName, inputTensor)
            val results = ortSession!!.run(inputs)
            
            results.use {
                val outputTensor = it.get(0) as OnnxTensor
                val imageEmbeddingRaw = FloatArray(outputTensor.info.shape.last().toInt())
                outputTensor.floatBuffer.get(imageEmbeddingRaw)

                var maxScore = -Float.MAX_VALUE
                var bestScene = "UNKNOWN"
                // 对模型输出进行归一化
                val normalizedImgVec = normalize(imageEmbeddingRaw)

                for ((sceneName, textVec) in actionVectors) {
                    val score = cosineSimilarity(normalizedImgVec, textVec)
                    if (score > maxScore) {
                        maxScore = score
                        bestScene = sceneName
                    }
                }

                Log.d("SemanticMatcher", "🏆 最终判定结果: $bestScene (置信度: $maxScore)")
                inputTensor.close()
                
                // 修正：降低拦截门槛，只要有最匹配的就返回，除非分值极其离谱
                return if (maxScore > -1.0) bestScene else "UNKNOWN"
            }
        } catch (e: Exception) {
            Log.e("SemanticMatcher", "❌ 推理崩溃: ${e.message}", e)
            return "ERROR"
        }
    }

    private fun preprocessUint8(bitmap: Bitmap, isNCHW: Boolean): OnnxTensor {
        val w = bitmap.width
        val h = bitmap.height
        val buffer = ByteBuffer.allocateDirect(1 * 3 * w * h).order(ByteOrder.nativeOrder())
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        if (isNCHW) {
            for (c in 0..2) {
                for (pixel in pixels) {
                    val v = when(c) { 0 -> Color.red(pixel); 1 -> Color.green(pixel); else -> Color.blue(pixel) }
                    buffer.put(v.toByte())
                }
            }
        } else {
            for (pixel in pixels) {
                buffer.put(Color.red(pixel).toByte())
                buffer.put(Color.green(pixel).toByte())
                buffer.put(Color.blue(pixel).toByte())
            }
        }
        buffer.flip()
        return OnnxTensor.createTensor(ortEnv, buffer, actualInputShape, OnnxJavaType.UINT8)
    }

    private fun preprocessFloat(bitmap: Bitmap, isNCHW: Boolean): OnnxTensor {
        val w = bitmap.width
        val h = bitmap.height
        val buffer = ByteBuffer.allocateDirect(1 * 3 * w * h * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val mean = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
        val std = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)

        if (isNCHW) {
            for (c in 0..2) {
                for (pixel in pixels) {
                    val v = when(c) { 0 -> Color.red(pixel); 1 -> Color.green(pixel); else -> Color.blue(pixel) }
                    buffer.put(((v / 255.0f) - mean[c]) / std[c])
                }
            }
        } else {
            for (pixel in pixels) {
                val r = (Color.red(pixel) / 255.0f - mean[0]) / std[0]
                val g = (Color.green(pixel) / 255.0f - mean[1]) / std[1]
                val b = (Color.blue(pixel) / 255.0f - mean[2]) / std[2]
                buffer.put(r); buffer.put(g); buffer.put(b)
            }
        }
        buffer.flip()
        return OnnxTensor.createTensor(ortEnv, buffer, actualInputShape)
    }

    private fun normalize(vec: FloatArray): FloatArray {
        var sum = 0.0f
        for (v in vec) sum += v * v
        val norm = sqrt(sum)
        if (norm < 1e-6f) return vec
        return FloatArray(vec.size) { vec[it] / norm }
    }

    private fun cosineSimilarity(vecA: FloatArray, vecB: FloatArray): Float {
        var dot = 0.0f
        for (i in 0 until minOf(vecA.size, vecB.size)) dot += vecA[i] * vecB[i]
        return dot
    }
}
