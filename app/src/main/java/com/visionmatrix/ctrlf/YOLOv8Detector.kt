package com.visionmatrix.ctrlf

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.tencent.ncnn.Yolov8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * YOLOv8目标检测器
 * 使用NCNN加载YOLOv8模型进行目标检测
 */
class YOLOv8Detector(private val context: Context) {
    
    private var yolov8: Yolov8? = null
    private var isInitialized = false
    
    // COCO类别名称（英文）
    private val classNames = arrayOf(
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
        "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
        "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
        "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
        "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
        "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
        "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair",
        "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
        "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
        "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
    )
    
    // 中文到英文的类别映射
    private val chineseToEnglishMap = mapOf(
        "人" to "person", "人物" to "person", "人像" to "person",
        "自行车" to "bicycle", "单车" to "bicycle",
        "汽车" to "car", "小汽车" to "car", "轿车" to "car",
        "摩托车" to "motorcycle", "电瓶车" to "motorcycle",
        "飞机" to "airplane", "航班" to "airplane",
        "公交车" to "bus", "巴士" to "bus",
        "火车" to "train", "列车" to "train",
        "卡车" to "truck", "货车" to "truck",
        "船" to "boat", "船只" to "boat",
        "红绿灯" to "traffic light", "交通灯" to "traffic light",
        "消防栓" to "fire hydrant",
        "停止标志" to "stop sign", "停车标志" to "stop sign",
        "停车计时器" to "parking meter",
        "长椅" to "bench", "凳子" to "bench",
        "鸟" to "bird", "小鸟" to "bird",
        "猫" to "cat", "猫咪" to "cat",
        "狗" to "dog", "小狗" to "dog",
        "马" to "horse",
        "羊" to "sheep", "绵羊" to "sheep",
        "牛" to "cow", "奶牛" to "cow",
        "大象" to "elephant",
        "熊" to "bear",
        "斑马" to "zebra",
        "长颈鹿" to "giraffe",
        "背包" to "backpack", "双肩包" to "backpack",
        "雨伞" to "umbrella", "伞" to "umbrella",
        "手提包" to "handbag", "手袋" to "handbag",
        "领带" to "tie",
        "行李箱" to "suitcase", "手提箱" to "suitcase",
        "飞盘" to "frisbee",
        "滑雪板" to "skis",
        "滑雪单板" to "snowboard",
        "球" to "sports ball", "运动球" to "sports ball",
        "风筝" to "kite",
        "棒球棒" to "baseball bat",
        "棒球手套" to "baseball glove",
        "滑板" to "skateboard",
        "冲浪板" to "surfboard",
        "网球拍" to "tennis racket",
        "瓶子" to "bottle", "瓶" to "bottle",
        "酒杯" to "wine glass", "玻璃杯" to "wine glass",
        "杯子" to "cup", "杯" to "cup", "水杯" to "cup", "茶杯" to "cup", "咖啡杯" to "cup",
        "叉子" to "fork",
        "刀" to "knife", "刀子" to "knife",
        "勺子" to "spoon",
        "碗" to "bowl",
        "香蕉" to "banana",
        "苹果" to "apple",
        "三明治" to "sandwich",
        "橙子" to "orange", "橘子" to "orange",
        "西兰花" to "broccoli",
        "胡萝卜" to "carrot",
        "热狗" to "hot dog",
        "披萨" to "pizza",
        "甜甜圈" to "donut", "多纳圈" to "donut",
        "蛋糕" to "cake",
        "椅子" to "chair",
        "沙发" to "couch", "长椅" to "couch",
        "盆栽" to "potted plant", "植物" to "potted plant",
        "床" to "bed",
        "餐桌" to "dining table", "桌子" to "dining table",
        "马桶" to "toilet", "厕所" to "toilet",
        "电视" to "tv", "电视机" to "tv",
        "笔记本电脑" to "laptop", "笔记本" to "laptop",
        "鼠标" to "mouse",
        "遥控器" to "remote",
        "键盘" to "keyboard",
        "手机" to "cell phone", "电话" to "cell phone",
        "微波炉" to "microwave",
        "烤箱" to "oven",
        "烤面包机" to "toaster",
        "水槽" to "sink", "洗手池" to "sink",
        "冰箱" to "refrigerator",
        "书" to "book", "书籍" to "book",
        "钟" to "clock", "时钟" to "clock",
        "花瓶" to "vase",
        "剪刀" to "scissors",
        "泰迪熊" to "teddy bear", "玩具熊" to "teddy bear",
        "吹风机" to "hair drier", "电吹风" to "hair drier",
        "牙刷" to "toothbrush"
    )
    
    /**
     * 初始化模型
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            yolov8 = Yolov8()
            val ret = yolov8?.loadModel(
                context.assets,
                "yolov8n_ncnn_model/model.ncnn.param",
                "yolov8n_ncnn_model/model.ncnn.bin"
            )
            
            if (ret == 0) {
                isInitialized = true
                Log.d(TAG, "YOLOv8模型加载成功")
            } else {
                Log.e(TAG, "YOLOv8模型加载失败，错误码: $ret")
            }
        } catch (e: Exception) {
            Log.e(TAG, "初始化YOLOv8模型时出错", e)
        }
    }
    
    /**
     * 检测目标
     * @param bitmap 输入图像
     * @param targetClass 目标类别（中文或英文）
     * @param confidenceThreshold 置信度阈值
     * @return 检测结果列表
     */
    suspend fun detect(
        bitmap: Bitmap,
        targetClass: String? = null,
        confidenceThreshold: Float = 0.25f
    ): List<DetectionResult> = withContext(Dispatchers.IO) {
        if (!isInitialized || yolov8 == null) {
            Log.w(TAG, "模型未初始化")
            return@withContext emptyList()
        }
        
        try {
            val ncnnResults = yolov8?.detect(bitmap, confidenceThreshold) ?: return@withContext emptyList()
            
            // 转换NCNN结果到Kotlin数据类
            val results = ncnnResults.map { ncnnResult ->
                DetectionResult(
                    classId = ncnnResult.classId,
                    className = ncnnResult.className ?: getClassName(ncnnResult.classId),
                    confidence = ncnnResult.confidence,
                    x = ncnnResult.x,
                    y = ncnnResult.y,
                    width = ncnnResult.width,
                    height = ncnnResult.height
                )
            }
            
            // 如果指定了目标类别，进行过滤
            if (targetClass != null && targetClass.isNotBlank()) {
                val targetClassName = translateToEnglish(targetClass.trim())
                val targetClassId = classNames.indexOf(targetClassName)
                
                if (targetClassId >= 0) {
                    return@withContext results.filter { it.classId == targetClassId }
                } else {
                    Log.w(TAG, "未找到类别: $targetClass (翻译后: $targetClassName)")
                    return@withContext emptyList()
                }
            }
            
            return@withContext results
        } catch (e: Exception) {
            Log.e(TAG, "检测时出错", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * 将中文类别名称翻译为英文
     */
    private fun translateToEnglish(chinese: String): String {
        // 先尝试直接匹配
        if (classNames.contains(chinese.lowercase())) {
            return chinese.lowercase()
        }
        
        // 尝试中文映射
        val lowerChinese = chinese.lowercase()
        return chineseToEnglishMap[lowerChinese] ?: chinese.lowercase()
    }
    
    /**
     * 获取类别名称（英文）
     */
    fun getClassName(classId: Int): String {
        return if (classId in classNames.indices) {
            classNames[classId]
        } else {
            "unknown"
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        yolov8 = null
        isInitialized = false
    }
    
    companion object {
        private const val TAG = "YOLOv8Detector"
    }
}

/**
 * 检测结果数据类
 */
data class DetectionResult(
    val classId: Int,
    val className: String,
    val confidence: Float,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

