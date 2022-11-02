package dev.epegasus.analogueservice

import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.button.MaterialButton
import dev.epegasus.analogueservice.service.PegasusWallpaperService
import dev.epegasus.analogueservice.service.PegasusWallpaperService.Companion.TAG

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialButton>(R.id.mb_start_Main).setOnClickListener { startWallpaperService() }
    }

    private fun startWallpaperService() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, PegasusWallpaperService::class.java))
            startForResult.launch(intent)
        } catch (ex: ActivityNotFoundException) {
            Log.d(TAG, "startWallpaperService: $ex")
            Toast.makeText(this, "No Activity Found for this service", Toast.LENGTH_SHORT).show()
        }
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show()
        }
    }
}