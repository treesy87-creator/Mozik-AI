
package com.example.brassistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private val requestCapture = 4102
    private lateinit var status: TextView

    private val bg = Color.rgb(8, 9, 20)
    private val cyan = Color.rgb(0, 225, 255)
    private val purple = Color.rgb(157, 85, 255)
    private val white = Color.WHITE

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun makeBackground(
        color: Int,
        border: Int = cyan
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(20).toFloat()
            setStroke(dp(1), border)
        }
    }

    private fun makeButton(
        title: String,
        color: Int,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = title
            textSize = 16f
            isAllCaps = false
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            background = makeBackground(color, color)
            backgroundTintList = null
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { action() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(30), dp(22), dp(24))
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)
        scroll.addView(root)

        val logo = TextView(this).apply {
            text = "M"
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(cyan)
            gravity = Gravity.CENTER
            background = makeBackground(
                Color.rgb(20, 17, 45),
                purple
            )
            layoutParams = LinearLayout.LayoutParams(
                dp(100), dp(100)
            ).apply {
                gravity = Gravity.CENTER
                bottomMargin = dp(18)
            }
        }

        root.addView(logo)

        root.addView(
            makeText("MOZIK AI", 32f, white, true)
        )

        root.addView(
            makeText(
                "YOUR SMART SCREEN ASSISTANT",
                11f,
                cyan,
                true
            ).apply {
                letterSpacing = 0.15f
            }
        )

        val spacer = View(this)
        root.addView(
            spacer,
            LinearLayout.LayoutParams(1, dp(35))
        )

        root.addView(
            makeText("ПАНЕЛЬ УПРАВЛЕНИЯ", 15f, purple, true)
        )

        status = makeText(
            "● ГОТОВ К РАБОТЕ\n\n" +
            "OCR: ожидает запуска\n" +
            "Звук: выключен\n" +
            "Помощник: остановлен",
            15f,
            white
        ).apply {
            background = makeBackground(
                Color.rgb(19, 20, 39),
                purple
            )
            setPadding(dp(18), dp(24), dp(18), dp(24))
            layoutParams = LinearLayout.LayoutParams(
                -1, -2
            ).apply {
                topMargin = dp(20)
                bottomMargin = dp(25)
            }
        }

        root.addView(status)

        val start = makeButton(
            "✦  НАЧАТЬ РАСПОЗНАВАНИЕ",
            Color.rgb(20, 85, 115)
        ) {
            val manager = getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

            startActivityForResult(
                manager.createScreenCaptureIntent(),
                requestCapture
            )
        }

        root.addView(
            start,
            LinearLayout.LayoutParams(-1, dp(62)).apply {
                bottomMargin = dp(14)
            }
        )

        val stop = makeButton(
            "■  ОСТАНОВИТЬ ПОМОЩНИКА",
            Color.rgb(90, 35, 75)
        ) {
            stopService(
                Intent(this, CaptureService::class.java)
            )
            status.text =
                "● ОСТАНОВЛЕН\n\n" +
                "OCR: выключен\n" +
                "Звук: выключен\n" +
                "Помощник: остановлен"
        }

        root.addView(
            stop,
            LinearLayout.LayoutParams(-1, dp(62))
        )

        val footer = makeText(
            "MOZIK AI  •  VERSION 0.1",
            11f,
            Color.GRAY
        )

        root.addView(
            footer,
            LinearLayout.LayoutParams(-1, dp(65))
        )

        setContentView(scroll)
    }

    @Deprecated("Prototype")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (
            requestCode == requestCapture &&
            resultCode == RESULT_OK &&
            data != null
        ) {
            val service = Intent(
                this,
                CaptureService::class.java
            ).apply {
                putExtra(
                    CaptureService.EXTRA_RESULT_CODE,
                    resultCode
                )
                putExtra(
                    CaptureService.EXTRA_RESULT_DATA,
                    data
                )
            }

            ContextCompat.startForegroundService(
                this,
                service
            )

            status.text =
                "● РАСПОЗНАВАНИЕ ЗАПУЩЕНО\n\n" +
                "OCR: работает\n" +
                "Звук: выключен\n" +
                "Помощник: активен\n\n" +
                "Можешь свернуть приложение."
        }
    }
}
