package com.example.camera12x

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import java.io.*
import java.lang.Runnable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class Camera2Fragment : Fragment() {
    companion object {
        fun newInstance() = Camera2Fragment()
        private val TAG = "Camera2Fragment"

        // Maximum number of images that will be held in the reader's buffer
        private const val IMAGE_BUFFER_SIZE: Int = 2

        private const val PREVIEW_FRAME_COUNT: Int = 1

        // Maximum time allowed to wait for the result of an image capture
        private const val IMAGE_CAPTURE_TIMEOUT_MILLIS: Long = 5000

        /** Helper data class used to hold capture metadata with their associated image */
        data class CombinedCaptureResult(
            val image: Image,
            val metadata: CaptureResult,
            val format: Int
        ) : Closeable {
            override fun close() = image.close()
        }
    }

    private lateinit var activity: Camera12XActivity

    // ID of the current CameraDevice.
    private lateinit var cameraID: String

    // Detects, characterizes, and connects to a CameraDevice (used for all camera operations)
    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    // [CameraCharacteristics] corresponding to the provided Camera ID
    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(cameraID)
    }

    // Readers used as buffers for camera still shots
    private lateinit var imageReader: ImageReader

    // [Handler] corresponding to [cameraThread]
    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null

    // [HandlerThread] where all buffer reading operations run,   // [Handler] corresponding to [imageReaderThread]
    private var imageReaderThread: HandlerThread? = null
    private var imageReaderHandler: Handler? = null

    // The CameraCaptureSession for camera preview.
    private var captureSession: CameraCaptureSession? = null

    // A reference to the opened CameraDevice
    private var camera2Device: CameraDevice? = null

    // An [AutoFitTextureView] for camera preview.
    private lateinit var textureView: AutoFitTextureView
    private lateinit var imagecapturebutton: Button

    // The camera preview size.
    private lateinit var previewSize: Size

    // TextureView.SurfaceTextureListener handles lifecycle events on a TextureView
    @JvmSynthetic
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureAvailable")
            // Open the Camera and then assign the surface of texture view as the preview surface
            setUpPreviewOutputs()
            configureTransform(width, height)

            /*if (activity.launchFromTestBundle == null) {
                // No intents, and directly setup Preview and ImageCapture.
                activity.logMemoryInfoPss("app init:")
                setupCamera()
            }*/
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
            /*if (activity.previewLatch!!.count.toInt() != 0) {
                Log.d(TAG, "Preview update frames: ${PREVIEW_FRAME_COUNT - activity.previewLatch!!.count}")
                activity.previewLatch!!.countDown()
                if (!activity.launchFromTest) {
                    activity.logMemoryInfoPss("Preview updated:")
                }
            }*/
        }
    }

    // A [Semaphore] to prevent the app from exiting before closing the camera.
    private val cameraOpenCloseLock = Semaphore(1)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        /*activity = (requireActivity() as Camera12XActivity)
        activity.previewLatch = CountDownLatch(PREVIEW_FRAME_COUNT)
        activity.imageLatch = CountDownLatch(1)*/
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_camera12x, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = view.findViewById(R.id.textureview_preview)
        imagecapturebutton = view.findViewById(R.id.btn_take_picture)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable) {
            Log.d(TAG, "Resume and textureView available")
            setUpPreviewOutputs()
            configureTransform(textureView.width, textureView.height)
            setupCamera()
        } else {
            Log.d(TAG, "Set SurfaceTextureListener!")
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    /**
     * Always close Camera when onPause
     */
    override fun onPause() {
        Log.d(TAG, "onPause")
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun getCameraId(): Boolean {
        loop@ for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            // Use back camera only in this sample at first.
            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
            when (cameraDirection) {
                CameraCharacteristics.LENS_FACING_FRONT -> {
                    continue@loop
                }
                CameraCharacteristics.LENS_FACING_BACK -> {
                    Log.d(TAG, "Back CameraId is: $cameraId")
                    cameraID = cameraId
                    return true
                }
                else -> continue@loop
            }
        }
        return false
    }

    /**
     * Begin all camera operations in a coroutine in the main thread. This function:
     * - Opens the camera
     * - Configures the camera session
     * - Starts the preview by dispatching a repeating capture request
     * - Sets up the still image capture listeners
     */
    @SuppressLint("MissingPermission")
    fun setupCamera() = CoroutineScope(Dispatchers.Main).launch {
        if (!getCameraId()) {
            // No BackCamera for this device.
            requireActivity().finish()
            return@launch
        }

        // Open the selected camera
        camera2Device = openCamera(cameraManager, cameraID, cameraHandler)
        Log.d(TAG, "Successfully open camera, cameraDevice : $camera2Device")
        val texture = textureView.surfaceTexture
        val viewSurface = Surface(texture)
        // Initialize an image reader which will be used to capture still photos
        val imageSize = Size(Camera12XActivity.MAX_WIDTH, Camera12XActivity.MAX_HEIGHT)
        imageReader = ImageReader.newInstance(
            imageSize.width,
            imageSize.height,
            ImageFormat.JPEG,
            IMAGE_BUFFER_SIZE
        )

        // Creates list of Surfaces where the camera will output frames, Preview and ImageCapture.
        val targets = listOf(viewSurface, imageReader.surface)

        // Start a capture session using our open camera and list of Surfaces where frames will go
        captureSession = createCaptureSession(camera2Device!!, targets, cameraHandler)
        Log.d(TAG, "Successfully create CaptureSession: $camera2Device")
        val captureRequest = camera2Device!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            .apply {
                addTarget(viewSurface)
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }

        // This will keep sending the capture request as frequently as possible until the
        // session is torn down or session.stopRepeating() is called
        captureSession!!.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

        // setup image capture button listener
        imagecapturebutton.setOnClickListener {
            // Disable click listener to prevent multiple requests simultaneously in flight
            it.post { it.isEnabled = false }

            // Perform I/O heavy operations in a different scope
            CoroutineScope(Dispatchers.IO).launch {
                takePhoto().use { result ->
                    Log.d(TAG, "Result received: $result")
                    // Save the result to disk
                    val output = saveResult(result)
                    Log.d(TAG, "Image saved: ${output.absolutePath}")
                }

                /*activity.imageLatch!!.countDown()
                if (!activity.launchFromTest) {
                    activity.logMemoryInfoPss("Complete take-picture:")
                }*/
                // Re-enable click listener after image is captured
                it.post { it.isEnabled = true }
            }
        }
    }

    /** Opens the camera and returns the opened device (as the result of the suspend coroutine) */
    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) = cont.resume(device)

                @SuppressLint("SyntheticAccessor")
                override fun onDisconnected(device: CameraDevice) {
                    Log.w(TAG, "Camera $cameraId has been disconnected")
                    requireActivity().finish()
                }

                @SuppressLint("SyntheticAccessor")
                override fun onError(device: CameraDevice, error: Int) {
                    val msg = when (error) {
                        ERROR_CAMERA_DEVICE -> "Fatal (device)"
                        ERROR_CAMERA_DISABLED -> "Device policy"
                        ERROR_CAMERA_IN_USE -> "Camera in use"
                        ERROR_CAMERA_SERVICE -> "Fatal (service)"
                        ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                        else -> "Unknown"
                    }
                    val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                    Log.e(TAG, exc.message, exc)
                    if (cont.isActive) cont.resumeWithException(exc)
                }
            },
            handler
        )
    }

    /**
     * Starts a [CameraCaptureSession] and returns the configured session (as the result of the
     * suspend coroutine
     */
    @Suppress("DEPRECATION")
    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->
        // Create a capture session using the predefined targets; this also involves defining the
        // session state callback to be notified of when the session is ready
        device.createCaptureSession(
            targets,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

                @SuppressLint("SyntheticAccessor")
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    val exc = RuntimeException("Camera ${device.id} session configuration failed")
                    Log.e(TAG, exc.message, exc)
                    cont.resumeWithException(exc)
                }
            },
            handler
        )
    }

    // Closes the current [CameraDevice].
    internal fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            camera2Device?.close()
            camera2Device = null
            imageReader.close()
            Log.d(TAG, "Close Camera and release all resources")
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    // Starts a background thread and its [Handler].
    internal fun startBackgroundThread() {
        cameraThread = HandlerThread("Camera2Thread").apply { start() }
        cameraHandler = cameraThread?.looper?.let { Handler(it) }

        imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
        imageReaderHandler = imageReaderThread?.looper?.let { Handler(it) }
    }

    // Stops the background thread and its [Handler].
    internal fun stopBackgroundThread() {
        cameraThread?.quitSafely()
        imageReaderThread?.quitSafely()
        try {
            cameraThread?.join()
            imageReaderThread?.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * Configures the necessary [android.graphics.Matrix] transformation to `textureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = requireActivity().windowManager.defaultDisplay!!.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)
    }

    // Currently, it is fixed size for Camera Preview.
    private fun setUpPreviewOutputs() {
        // TODO(): Use fixed View preview size at first, consider to calculate
        //  chooseOptimalSize for View and Surface, and support rotation.
        previewSize = Size(Camera12XActivity.MAX_WIDTH, Camera12XActivity.MAX_HEIGHT)
        // Fit the aspect ratio of TextureView to the size of preview picked.
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textureView.setAspectRatio(previewSize.width, previewSize.height)
        } else {
            textureView.setAspectRatio(previewSize.height, previewSize.width)
        }
        return
    }

    /**
     * Helper function used to capture a still image using the [CameraDevice.TEMPLATE_STILL_CAPTURE]
     * template. It performs synchronization between the [CaptureResult] and the [Image] resulting
     * from the single capture, and outputs a [CombinedCaptureResult] object.
     */
    private suspend fun takePhoto(): CombinedCaptureResult = suspendCoroutine { cont ->
        @Suppress("ControlFlowWithEmptyBody")
        while (imageReader.acquireNextImage() != null) {
        }

        // Start a new image queue
        val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
        imageReader.setOnImageAvailableListener(
            { reader ->
                val image = reader.acquireNextImage()
                Log.d(TAG, "Image in queue: ${image.timestamp}")
                imageQueue.add(image)
            },
            imageReaderHandler
        )

        val captureRequest = captureSession!!.device.createCaptureRequest(
            CameraDevice.TEMPLATE_STILL_CAPTURE
        ).apply { addTarget(imageReader.surface) }
        captureSession!!.capture(
            captureRequest.build(),
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                    Log.d(TAG, "Capture result received: $resultTimestamp")

                    // Set a timeout in case image captured is dropped from the pipeline
                    val exc = TimeoutException("Image dequeuing took too long")
                    val timeoutRunnable = Runnable { cont.resumeWithException(exc) }
                    imageReaderHandler?.postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIMEOUT_MILLIS)

                    // Loop in the coroutine's context until an image with matching timestamp comes
                    // We need to launch the coroutine context again because the callback is done in
                    //  the handler provided to the `capture` method, not in our coroutine context
                    @Suppress("BlockingMethodInNonBlockingContext")
                    CoroutineScope(cont.context).launch {
                        while (true) {
                            // Dequeue images while timestamps don't match
                            val image = imageQueue.take()
                            Log.d(TAG, "Matching image dequeued: ${image.timestamp}")
                            // Unset the image reader listener
                            imageReaderHandler?.removeCallbacks(timeoutRunnable)
                            imageReader.setOnImageAvailableListener(null, null)

                            // Clear the queue of images, if there are left
                            while (imageQueue.size > 0) {
                                imageQueue.take().close()
                            }

                            // TODO() Compute EXIF orientation metadata

                            // Build the result and resume progress
                            cont.resume(CombinedCaptureResult(image, result, imageReader.imageFormat))
                            // There is no need to break out of the loop, this coroutine will suspend
                        }
                    }
                }
            },
            cameraHandler
        )
    }

    /** Helper function used to save a [CombinedCaptureResult] into a [File] */
    private suspend fun saveResult(result: CombinedCaptureResult): File = suspendCoroutine { cont ->
        when (result.format) {
            // When the format is JPEG, simply save the bytes as-is.
            ImageFormat.JPEG -> {
                val buffer = result.image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }

                val output = activity.getOutputFile("jpg")
                try {
                    val outputStream: FileOutputStream = FileOutputStream(output)
                    outputStream.write(bytes)
                    outputStream.close()
                    cont.resume(output)
                } catch (exc: Exception) {
                    when (exc) {
                        is FileNotFoundException -> {
                            Log.e(TAG, "$output, file not found. ", exc)
                        }
                        is SecurityException -> {
                            Log.e(TAG, "Security exception.", exc)
                        }
                        is IOException -> {
                            Log.e(TAG, "Unable to write JPEG image to file", exc)
                        }
                    }
                    cont.resumeWithException(exc)
                }
            }
            // No other formats are supported by this sample
            else -> {
                val exc = RuntimeException("Unknown image format: ${result.image.format}")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }
    }
}



