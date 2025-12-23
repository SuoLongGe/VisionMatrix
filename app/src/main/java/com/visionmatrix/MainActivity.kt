package com.visionmatrix

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.visionmatrix.ctrlf.CtrlFActivity
import com.visionmatrix.actioncards.ScenarioActivity
import com.visionmatrix.actioncards.SemanticMatcher
import com.visionmatrix.ctrlf.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 绑定全新的首页布局
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- 植入“大脑”模拟测试逻辑 ---
        try {
            // 1. 初始化分析器
            val matcher = SemanticMatcher(this)
            matcher.init()

            // 2. 模拟测试：创建一个 500x500 的纯黑位图
            val testBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)

            // 3. 在后台线程运行分析，防止阻塞主线程
            Thread {
                Log.d("VisionTest", "正在对测试图片进行 AI 分析...")
                val result = matcher.analyzeScene(testBitmap)
                Log.d("VisionTest", "测试图片的识别结果: $result")
            }.start()
        } catch (e: Exception) {
            Log.e("VisionTest", "初始化或测试过程中发生崩溃: ${e.message}")
        }
        // -----------------------------

        // 设置“目标检索”卡片的点击事件
        binding.cardCtrlF.setOnClickListener {
            val intent = Intent(this, CtrlFActivity::class.java)
            startActivity(intent)
        }

        // 设置“场景卡片”卡片的点击事件
        binding.cardScenario.setOnClickListener {
            val intent = Intent(this, ScenarioActivity::class.java)
            startActivity(intent)
        }
    }
}
