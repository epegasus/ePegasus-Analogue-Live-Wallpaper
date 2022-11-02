package dev.epegasus.analogueservice.service

import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder

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
        private val paint by lazy {
            Paint().apply {
                color = Color.WHITE
                strokeWidth = 5f
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)

            // Disabling few things for less complexity
            setTouchEventsEnabled(false)
            setOffsetNotificationsEnabled(false)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.d(TAG, "onSurfaceChanged: called")
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            Log.d(TAG, "onSurfaceCreated: called")
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            Log.d(TAG, "onSurfaceDestroyed: called")
        }

        /**
         *  It is very important that a wallpaper only use CPU while it is visible.
         */
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            // Start/Stop drawing on Canvas
            if (visible) handler.post(runnable)
            else handler.removeCallbacks(runnable)
        }

        private fun draw() {
            // if 'Surface' is not created yet! lockCanvas cannot provide canvas for performing any action
            if (!surfaceHolder.surface.isValid) {
                return
            }
            val canvas = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    surfaceHolder.lockHardwareCanvas()
                } else {
                    surfaceHolder.lockCanvas()
                }
            } catch (ex: Exception) {
                Log.d(TAG, "draw: $ex")
                surfaceHolder.lockCanvas()
            }
            val startPair = Pair((canvas.width / 2).toFloat(), 0f)
            val endPair = Pair((canvas.width / 2).toFloat(),  (canvas.height / 1).toFloat())
            canvas.drawLine(startPair.first, startPair.second, endPair.first, endPair.second, paint)
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    companion object {
        const val TAG = "MyTag"
    }
}