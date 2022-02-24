package com.example.camera12x

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Button
import androidx.camera.core.*
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraXFragment : Fragment() {
    companion object {
        fun newInstance() = CameraXFragment()
        private const val TAG = "CameraXFragment"
    }

    private lateinit var activity: Camera12XActivity
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture

    private lateinit var textureView: AutoFitTextureView
    private lateinit var imagecapturebutton: Button

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    // TextureView.SurfaceTextureListener handles lifecycle events on a TextureView
    private val surfaceTextureListener =
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "onSurfaceTextureAvailable: $texture")
            }

            override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                Log.d(TAG, "onSurfaceTextureDestroyed: $texture")
                return true
            }

            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
                /*if (activity.previewLatch!!.count.toInt() != 0) {
                    Log.d(TAG, "onSurfaceTextureUpdated: $texture")
                    activity.previewLatch!!.countDown()
                    if (!activity.launchFromTest) {
                        activity.logMemoryInfoPss("CameraX Preview updated:")
                    }
                }*/
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = (requireActivity() as Camera12XActivity)
        // activity.previewLatch = CountDownLatch(1)
        // activity.imageLatch = CountDownLatch(1)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_camera12x, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = view.findViewById(R.id.textureview_preview)
        imagecapturebutton = view.findViewById(R.id.btn_take_picture)
        textureView.surfaceTextureListener = surfaceTextureListener

        if (activity.intentBundle == null) {
            // No intents, and directly setup Preview and ImageCapture.
            //activity.logMemoryInfoPss("CameraX init:")
            setupCamera()
        }
    }

    /** Initialize CameraX, and prepare to bind the camera use cases */
    fun setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener(
            Runnable {
                cameraProvider = cameraProviderFuture.get()
                bindUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )

        // Setup ImageCapture Listener
        setupTakePicture()
    }

    @SuppressLint("RestrictedApi")
    private fun bindUseCases() {
        // setup Preview UseCase
        preview =
            Preview.Builder()
                .setTargetName("Preview")
                .setTargetResolution(Size(Camera12XActivity.MAX_HEIGHT,
                    Camera12XActivity.MAX_WIDTH))
                .build()

        // setup ImageCapture UseCAse
        imageCapture =
            ImageCapture.Builder()
                .setTargetName("ImageCapture")
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(Size(Camera12XActivity.MAX_HEIGHT,
                    Camera12XActivity.MAX_WIDTH))
                .build()

        // Unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        // Support Back Camera first
        cameraProvider.bindToLifecycle(this,
            CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)

        when (activity.viewMode) {
            Camera12XActivity.VIEW_PREVIEW -> {
                Log.d(TAG, "PreviewView not supported yet")
            }
            Camera12XActivity.VIEW_TEXTURE -> {
                preview.setSurfaceProvider { surfaceRequest: SurfaceRequest ->
                    // Create the SurfaceTexture and Surface
                    val surfaceTexture = SurfaceTexture(0)
                    Log.d(TAG, "Surface request resolution: ${surfaceRequest.resolution}")

                    surfaceTexture.setDefaultBufferSize(
                        surfaceRequest.resolution.width,
                        surfaceRequest.resolution.height
                    )

                    surfaceTexture.detachFromGLContext()
                    val surface = Surface(surfaceTexture)

                    // Attach the SurfaceTexture on the TextureView that needs to be removed/added again.
                    val viewGroup = textureView.parent as ViewGroup
                    viewGroup.removeView(textureView)
                    viewGroup.addView(textureView)
                    textureView.setSurfaceTexture(surfaceTexture)
                    Log.d(TAG, "Created SurfaceTexture: $surfaceTexture")

                    // Surface provided to camera for producing buffers into and release the SurfaceTexture
                    // and Surface once camera is done with it
                    surfaceRequest.provideSurface(
                        surface,
                        CameraXExecutors.directExecutor(),
                        Consumer {
                            surface.release()
                            surfaceTexture.release()
                            Log.d(TAG, "Created SurfaceTexture released: $surfaceTexture")
                        }
                    )
                }
            }
        }
    }

    /** Setup take-picture action handle process */
    private fun setupTakePicture() {
        // Listener for take picture button
        imagecapturebutton.setOnClickListener {
            Log.d(TAG, "Start ImageCapture")

            activity.createDefaultPictureFolderIfNotExist()
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            val outputFileOptions =
                ImageCapture.OutputFileOptions.Builder(
                    activity.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                    .build()

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(
                outputFileOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    @SuppressLint("SyntheticAccessor")
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                        //activity.imageLatch!!.countDown()
                    }

                    @SuppressLint("SyntheticAccessor")
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Log.d(TAG, "Photo capture saved: {$output.savedUri}")


                         // Post-process the image for app need.
                    /*activity.imageLatch!!.countDown()
                        if (!activity.launchFromTest) {
                            activity.logMemoryInfoPss("CameraX complete take-picture:")
                        }*/
                    }
                }
            )
        }
    }
}

