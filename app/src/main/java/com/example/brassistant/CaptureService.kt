
package com.example.brassistant

import android.app.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

class CaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL = "br_ai_capture"
        private const val NOTIFICATION_ID = 77
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var handler: Handler? = null

    private val busy = AtomicBoolean(false)

    private val recognizer by lazy {
        TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )
    }

    private var lastAnswer = ""

    override fun onCreate() {
        super.onCreate()

        createChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Mozik AI")
            .setContentText("Распознавание экрана активно · звук выключен")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        worker = HandlerThread("mozik-ocr").also {
            it.start()
        }

        handler = Handler(worker!!.looper)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val code = intent?.getIntExtra(
            EXTRA_RESULT_CODE,
            Activity.RESULT_CANCELED
        ) ?: Activity.RESULT_CANCELED

        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(
                EXTRA_RESULT_DATA,
                Intent::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            (intent?.getParcelableExtra(EXTRA_RESULT_DATA) as? Intent)
        }

        if (code != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val manager = getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        projection = manager.getMediaProjection(code, data)

        startCapture()

        return START_NOT_STICKY
    }

    private fun startCapture() {
        val metrics = resources.displayMetrics

        val width = metrics.widthPixels
        val height = metrics.heightPixels

        reader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            2
        )

        reader?.setOnImageAvailableListener({ source ->

            val image = source.acquireLatestImage()
                ?: return@setOnImageAvailableListener

            if (!busy.compareAndSet(false, true)) {
                image.close()
                return@setOnImageAvailableListener
            }

            process(image)

        }, handler)

        virtualDisplay = projection?.createVirtualDisplay(
            "MozikAI-OCR",
            width,
            height,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader?.surface,
            null,
            handler
        )
    }

    private fun process(image: Image) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride

        val rowPadding = rowStride - pixelStride * image.width
        val paddedWidth = image.width + rowPadding / pixelStride

        val padded = android.graphics.Bitmap.createBitmap(
            paddedWidth,
            image.height,
            android.graphics.Bitmap.Config.ARGB_8888
        )

        padded.copyPixelsFromBuffer(buffer)

        val bitmap = android.graphics.Bitmap.createBitmap(
            padded,
            0,
            0,
            image.width,
            image.height
        )

        padded.recycle()
        image.close()

        recognizer.process(
            InputImage.fromBitmap(bitmap, 0)
        )
            .addOnSuccessListener { result ->

                val text = result.text
                val answer = AnswerEngine.findAnswer(text)

                if (answer.isNotBlank() && answer != lastAnswer) {
                    lastAnswer = answer
                    showAnswer(answer)
                }
            }
            .addOnCompleteListener {
                bitmap.recycle()
                busy.set(false)
            }
    }

    private fun showAnswer(answer: String) {
        val notification = NotificationCompat.Builder(
            this,
            CHANNEL
        )
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Mozik AI · Ответ найден")
            .setContentText(answer)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$answer\n\nПроверь ответ и отправь его вручную в игре."
                )
            )
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.notify(
            (answer.hashCode() and 0x7fffffff) % 10000 + 100,
            notification
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {

            val channel = NotificationChannel(
                CHANNEL,
                "Mozik AI",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Ответы OCR; звук выключен"
                setSound(null, null)
                enableVibration(false)
            }

            val manager = getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        reader?.close()
        projection?.stop()
        recognizer.close()
        worker?.quitSafely()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
