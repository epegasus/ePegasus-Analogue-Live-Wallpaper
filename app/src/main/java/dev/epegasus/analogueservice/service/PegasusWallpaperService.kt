package dev.epegasus.analogueservice.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.annotation.DrawableRes
import dev.epegasus.analogueservice.R
import java.util.Calendar

class PegasusWallpaperService : WallpaperService() {

    /**
     * Service Starting Hierarchy
     *      1) Service:         onCreate()
     *      2) Service:         onCreateEngine()
     *          i) Engine:              onCreate()
     *          ii) Engine:             onSurfaceCreated()
     *          iii) Engine:            onSurfaceChanged()
     *          iv) Engine:             onVisibilityChanged()
     *          v) Engine:              onTouchEvent()                  // if any
     *          vi) Engine:             onSurfaceDestroyed()
     *          iii) Engine:            onDestroy()
     *      3) Service:         onDestroy()
     */

    override fun onCreateEngine(): Engine {
        return PegasusWallpaperServiceEngine()
    }

    inner class PegasusWallpaperServiceEngine : WallpaperService.Engine() {

        private val handler by lazy { Handler(Looper.getMainLooper()) }
        private val runnable by lazy { Runnable { draw() } }

        private val bitmapBg by lazy { getBMP(R.drawable.img_clock_bg) }
        private val bitmapHr by lazy { getBMP(R.drawable.img_clock_hr) }
        private val bitmapMin by lazy { getBMP(R.drawable.img_clock_min) }
        private val bitmapSec by lazy { getBMP(R.drawable.img_clock_sec) }

        private var isDefaultUpdated = false
        private var imageResolution = 200
        private var xCoordinateCanvas = 100f
        private var yCoordinateCanvas = 300f
        private var degreeHr = 0.0f
        private var degreeMin = 0.0f
        private var degreeSec = 0.0f

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)

            // Disabling few things for less complexity
            setTouchEventsEnabled(false)
            setOffsetNotificationsEnabled(false)
        }

        /**
         *  It is very important that a wallpaper only use CPU while it is visible. So, only draw if visible.
         */
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            // Start/Stop drawing on Canvas
            getInitialAllAngle()
            if (visible) handler.post(runnable)
            else handler.removeCallbacks(runnable)
        }

        /**
         * Set angles of all needles according to current time
         *      Hours:      total 12h (Rotation: 360)
         *      Minutes:    total 60m (Rotation: 360) ~ 360/60 = 6 times
         *      Seconds:    total 60s (Rotation: 360) ~ 360/60 = 6 times
         */
        private fun getInitialAllAngle() {
            val calender = Calendar.getInstance()
            val hour = calender.get(Calendar.HOUR)
            val minute = calender.get(Calendar.MINUTE)
            val second = calender.get(Calendar.SECOND)

            degreeHr = (hour * 30F) + (minute * 0.5F)
            degreeMin = minute * 6F
            degreeSec = second * 6F
        }

        private fun draw() {
            // if 'Surface' is not created yet! lockCanvas cannot provide canvas for performing any action
            if (!surfaceHolder.surface.isValid) {
                Log.d(TAG, "draw: Surface is not Valid (created yet).")
                return
            }
            val canvas = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    surfaceHolder.lockHardwareCanvas()
                } else {
                    surfaceHolder.lockCanvas()
                }
            } catch (ex: Exception) {
                Log.d(TAG, "draw: LockHardwareCanvas: $ex")
                surfaceHolder.lockCanvas()
            }

            canvas?.let {
                // Update image 'height/width' only if not default
                if (!isDefaultUpdated) {
                    isDefaultUpdated = true
                    imageResolution = (it.width * 70) / 100
                    xCoordinateCanvas = ((it.width / 2) - imageResolution / 2).toFloat()
                    yCoordinateCanvas = ((it.height / 2) - imageResolution / 2).toFloat()
                }
                // Set Background of Clock
                it.drawColor(0, PorterDuff.Mode.CLEAR)
                it.drawBitmap(bitmapBg, xCoordinateCanvas, yCoordinateCanvas, null)

                hrNeedleControl(it)
                minNeedleControl(it)
                secNeedleControl(it)
                surfaceHolder.unlockCanvasAndPost(it)
                repeatMethod()
            } ?: kotlin.run {
                Log.d(TAG, "draw: Canvas is not found.")
                return
            }
        }

        private fun repeatMethod() {
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 1000)
        }

        private fun getBMP(@DrawableRes resId: Int): Bitmap {
            val temp = BitmapFactory.decodeResource(applicationContext.resources, resId)
            return Bitmap.createScaledBitmap(temp, imageResolution, imageResolution, true)
        }

        private fun hrNeedleControl(canvas: Canvas) {
            val scaleWidth = (imageResolution / bitmapHr.width).toFloat()
            val scaleHeight = (imageResolution / bitmapHr.height).toFloat()
            val matrix = Matrix().apply {
                postScale(scaleWidth, scaleHeight)
                postRotate(degreeHr)
            }
            val bitmap = Bitmap.createBitmap(bitmapHr, 0, 0, bitmapHr.width, bitmapHr.height, matrix, true)

            val leftCoordinate = ((canvas.width - bitmap.width) / 2).toFloat()
            val topCoordinate = ((canvas.height - bitmap.height) / 2).toFloat()
            canvas.drawBitmap(bitmap, leftCoordinate, topCoordinate, null)
        }

        private fun minNeedleControl(canvas: Canvas) {
            val scaleWidth = (imageResolution / bitmapMin.width).toFloat()
            val scaleHeight = (imageResolution / bitmapMin.height).toFloat()
            val matrix = Matrix().apply {
                postScale(scaleWidth, scaleHeight)
                postRotate(degreeMin)
            }
            val bitmap = Bitmap.createBitmap(bitmapMin, 0, 0, bitmapMin.width, bitmapMin.height, matrix, true)

            val leftCoordinate = ((canvas.width - bitmap.width) / 2).toFloat()
            val topCoordinate = ((canvas.height - bitmap.height) / 2).toFloat()
            canvas.drawBitmap(bitmap, leftCoordinate, topCoordinate, null)
        }

        private fun secNeedleControl(canvas: Canvas) {
            val scaleWidth = (imageResolution / bitmapSec.width).toFloat()
            val scaleHeight = (imageResolution / bitmapSec.height).toFloat()
            val matrix = Matrix()
            matrix.postScale(scaleWidth, scaleHeight)
            matrix.postRotate(degreeSec)

            val bitmap = Bitmap.createBitmap(bitmapSec, 0, 0, bitmapSec.width, bitmapSec.height, matrix, false)
            updateAngles()

            val leftCoordinate = ((canvas.width - bitmap.width) / 2).toFloat()
            val topCoordinate = ((canvas.height - bitmap.height) / 2).toFloat()
            canvas.drawBitmap(bitmap, leftCoordinate, topCoordinate, null)
        }

        private fun updateAngles() {
            if (degreeSec >= 360) {
                degreeSec = 6.0F
                if (degreeMin >= 360) {
                    degreeMin = 6.0F
                    if (degreeHr >= 360) degreeHr = 0.5F else degreeHr += 0.5F
                } else {
                    degreeMin += 6.0F
                    if (degreeMin % 6 == 0.0F) {
                        if (degreeHr >= 360) degreeHr = 0.5F else degreeHr += 0.5F
                    }
                }
            } else {
                degreeSec += 6.0F
            }
        }
    }

    companion object {
        const val TAG = "MyTag"
    }
}