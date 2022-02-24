package com.example.camera12x

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.jar.Manifest

class Camera12XActivity : AppCompatActivity() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        private const val CAMERA_VERSION_X = 0
        private const val CAMERA_VERSION_1 = 1
        private const val CAMERA_VERSION_2 = 2

        const val VIEW_TEXTURE = 1  // Texture View
        const val VIEW_SURFACE = 2  // Surface View
        const val VIEW_PREVIEW = 3  // CameraX PreviewView

        const val MAX_HEIGHT = 640
        const val MAX_WIDTH = 480

        private const val TAG = "Camera12XActivity"
        private const val REQUEST_CODE_PERMISSIONS = 6
        fun createIntent(context: Context?): Intent? {
            return Intent(context, Camera12XActivity::class.java)
        }

    }

    private var cameraVersion = CAMERA_VERSION_X
    private lateinit var cameraFragment : Fragment
    var lanchFromIntent: Boolean = false
    var intentBundle :Bundle? = null
    var viewMode = VIEW_TEXTURE


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // check if launch specific Camera fragment from intent
        intentBundle = intent.extras  // Activity getIntent()
        if (intentBundle != null) {
            lanchFromIntent = true
            //cameraVersion = intent.getIntExtra(INTENT_CAMERA_VERSION, CAMERA_VERSION_X)
            //viewMode = intent.getIntExtra(INTENT_VIEW_MODE, VIEW_TEXTURE)
        }

        // check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !allPermissionsGranted()) {
                ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
        } else {
            startFragment()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //return super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.camera12x_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //return super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.camerax -> cameraVersion = CAMERA_VERSION_X
            R.id.camera2 -> cameraVersion = CAMERA_VERSION_2
            R.id.camera1 -> cameraVersion = CAMERA_VERSION_1
        }
        startFragment()
        return true
    }

    private fun startFragment() {
        when (cameraVersion) {
            CAMERA_VERSION_X -> {
                supportActionBar?.title = "CameraX"
                cameraFragment = CameraXFragment.newInstance()
            }
            CAMERA_VERSION_2 -> {

            }
            CAMERA_VERSION_1 -> {

            }
            else -> {
                finish()
            }
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, cameraFragment)
            .commit()
    }

    internal fun createDefaultPictureFolderIfNotExist() : Boolean {
        val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (folder.exists()) {
            return true
        }
        return folder.mkdir()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startFragment()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }
}