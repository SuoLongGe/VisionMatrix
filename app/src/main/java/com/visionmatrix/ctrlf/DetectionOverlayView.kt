package com.visionmatrix.ctrlf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 检测结果覆盖层视图
 * 用于在相机预览上绘制检测框
 */
class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var detections: List<DetectionResult> = emptyList()
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    
    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = 32f
        isAntiAlias = true
        isFakeBoldText = true
    }
    
    private val backgroundPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
    }
    
    /**
     * 设置检测结果
     * @param detections 检测结果列表
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     */
    fun setDetections(
        detections: List<DetectionResult>,
        imageWidth: Int,
        imageHeight: Int
    ) {
        this.detections = detections
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        invalidate()
    }
    
    /**
     * 清除检测结果
     */
    fun clearDetections() {
        this.detections = emptyList()
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (detections.isEmpty() || imageWidth == 0 || imageHeight == 0) {
            return
        }
        
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        // 计算缩放比例（保持宽高比）
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val scale = scaleX.coerceAtMost(scaleY)
        
        // 计算偏移量（居中显示）
        val offsetX = (viewWidth - imageWidth * scale) / 2f
        val offsetY = (viewHeight - imageHeight * scale) / 2f
        
        for (detection in detections) {
            // 将检测框坐标从图像坐标系转换到视图坐标系
            val left = offsetX + detection.x * scale
            val top = offsetY + detection.y * scale
            val right = offsetX + (detection.x + detection.width) * scale
            val bottom = offsetY + (detection.y + detection.height) * scale
            
            val rect = RectF(left, top, right, bottom)
            
            // 绘制检测框
            canvas.drawRect(rect, boxPaint)
            
            // 绘制标签背景和文字
            val label = "${detection.className} ${(detection.confidence * 100).toInt()}%"
            val textWidth = textPaint.measureText(label)
            val textHeight = textPaint.descent() - textPaint.ascent()
            
            val labelRect = RectF(
                left,
                top - textHeight - 8,
                left + textWidth + 16,
                top
            )
            
            canvas.drawRect(labelRect, backgroundPaint)
            canvas.drawText(
                label,
                left + 8,
                top - 8,
                textPaint
            )
        }
    }
}


