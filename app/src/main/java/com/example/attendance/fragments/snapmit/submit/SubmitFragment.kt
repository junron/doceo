package com.example.attendance.fragments.snapmit.submit

import android.Manifest.permission
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.fragments.snapmit.ImagesBottomFragment
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.SafeLiveData
import com.example.attendance.util.android.ocr.MyImageProcessing
import com.example.attendance.viewmodels.AssignmentsViewModel
import com.example.attendance.viewmodels.SubmitViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.quickbirdstudios.yuv2mat.Yuv
import kotlinx.android.synthetic.main.fragment_submit.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.opencv.core.Core.rotate
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.IOException
import kotlin.math.sqrt

class SubmitFragment : Fragment() {
    val submitViewModel: SubmitViewModel by activityViewModels()
    private val assignmentsViewModel: AssignmentsViewModel by activityViewModels()
    private lateinit var imageCapture: ImageCapture
    lateinit var previewView: PreviewView
    lateinit var root: View
    private var scannerType = 0
    var latestImage: Mat? = null
    private lateinit var camPath: String
    private var overflowState = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        submitViewModel.assignmentUUID.value = assignmentsViewModel.currentAssignmentId!!

        val root = inflater.inflate(R.layout.fragment_submit, container, false)
        with(root) {
            bottom_bar_button.setOnClickListener {
                val addPhotoBottomDialogFragment =
                    ImagesBottomFragment.newInstance(submitViewModel.imagesData)
                addPhotoBottomDialogFragment.show(
                    this@SubmitFragment.parentFragmentManager,
                    "images_bottom_fragment"
                )
            }
            perms_button.setOnClickListener { requestPermissions() }

        }
        val camBut = root.camera_button
        camBut.setOnClickListener {
            var outFile: File? = null
            try {
                outFile = File.createTempFile("image", ".png", MainActivity.activity.cacheDir)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            camBut.isClickable = false
            camBut.animate().alpha(0.2f)
            val outputFileOptions = OutputFileOptions.Builder(outFile!!).build()
            val finalOutFile = outFile
            imageCapture.takePicture(outputFileOptions,
                { command: Runnable? ->
                    Thread(
                        command
                    ).start()
                }, object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: OutputFileResults) {
                        //on saved, read out image, perform correction and write back in.
                        if (scannerType == 1 || scannerType == 2) {
                            Thread(Runnable {
                                val imageMat: Mat = Imgcodecs.imread(finalOutFile.absolutePath)
                                Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_RGB2BGR)
                                val warped: Mat = MyImageProcessing.warpImageToPageFromOtherImage(
                                    latestImage,
                                    imageMat,
                                    2000,
                                    sqrt(2.0),
                                    true
                                )
                                Imgproc.cvtColor(warped, warped, Imgproc.COLOR_RGB2BGR)
                                Imgcodecs.imwrite(finalOutFile.absolutePath, warped)
                                val newVal =
                                    submitViewModel.imagesData.value.toMutableList()
                                newVal += finalOutFile
                                submitViewModel.imagesData.postValue(newVal)
                                runBlocking(Dispatchers.Main) {
                                    camBut.isClickable = true
                                    camBut.animate().alpha(1f)
                                    Snackbar.make(root, "Snapped!", 2000).setAction(
                                        "UNDO"
                                    ) {
                                        undoAddImage()
                                    }.show()
                                }
                            }).start()
                            return
                        }
                        root.camera_button.isClickable = true
                        camBut.animate().alpha(1f)
                        runBlocking(Dispatchers.Main) {
                            Snackbar.make(root, "Snapped!", 2000).setAction(
                                "UNDO"
                            ) {
                                undoAddImage()
                            }.show()
                        }
                        val newVal = submitViewModel.imagesData.value.toMutableList()
                        newVal += finalOutFile
                        submitViewModel.imagesData.postValue(newVal)
                    }

                    @SuppressLint("RestrictedApi")
                    override fun onError(exception: ImageCaptureException) {
                        root.camera_button.isClickable = true
                        camBut.animate().alpha(1f)
                        Snackbar.make(root, "Images failed to save.", 3000)
                            .setAction(
                                "Reload camera"
                            ) { v: View? ->
                                CameraX.unbindAll()
                                setCamera()
                            }.show()
                        //Toast.makeText(main, "Image failed to save.", Toast.LENGTH_SHORT).show();
                        Log.d("error", exception.toString())
                    }
                })
        }
        root.next_button.setOnClickListener {
            if (submitViewModel.imagesData.value.isEmpty()) {
                ImagesBottomFragment.newInstance(SafeLiveData(emptyList()))
                    .show(this.parentFragmentManager, "no_images_botfrag")
                return@setOnClickListener
            }
            // NavHostFragment.findNavController(this)
            //     .navigate(R.id.action_nav_ssubmit_to_submit2Fragment)
            Navigation.navigate(R.id.submit2Fragment)
        }
        val scannerButton: FloatingActionButton = root.findViewById(R.id.scanner_button)
        val overlayHolder =
            root.findViewById<ImageView>(R.id.overlay_holder)
        scannerButton.setOnClickListener { v: View ->
            val preferences: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(v.context)
            scannerType = (preferences.getInt("scannerType", 0) + 1) % 3
            preferences.edit().putInt("scannerType", scannerType).apply()
            val newIcon: Int
            when (scannerType) {
                1 -> {
                    newIcon = R.drawable.ic_scan_diamond
                    overlayHolder.scaleType = ImageView.ScaleType.CENTER_CROP
                    overlayHolder.animate().alpha(1f)
                    Toast.makeText(root.context, "Page detection mode", Toast.LENGTH_SHORT).show()
                }
                2 -> {
                    newIcon = R.drawable.ic_scan_lightning
                    overlayHolder.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    overlayHolder.animate().alpha(1f)
                    Toast.makeText(root.context, "Page view mode", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    newIcon = R.drawable.ic_scan_blank
                    overlayHolder.animate().alpha(0f)
                    Toast.makeText(root.context, "Normal mode", Toast.LENGTH_SHORT).show()
                }
            }
            scannerButton.setImageResource(newIcon)
        }
        val preferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val newIcon: Int
        scannerType = preferences.getInt("scannerType", 0)
        when (scannerType) {
            1 -> {
                newIcon = R.drawable.ic_scan_diamond
                overlayHolder.scaleType = ImageView.ScaleType.CENTER_CROP
            }
            2 -> {
                newIcon = R.drawable.ic_scan_lightning
                overlayHolder.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
            else -> {
                newIcon = R.drawable.ic_scan_blank
            }
        }
        scannerButton.setImageResource(newIcon)
        submitViewModel.imagesData.observe({ viewLifecycleOwner.lifecycle }, { files ->
            Log.d("images", files.size.toString())
            val visibility =
                if (files.isEmpty()) View.GONE else View.VISIBLE
            with(root) {
                next_button.visibility = visibility
                bottom_bar_button.visibility = visibility
            }
        })
        val camInbuilt = root.camera_inbuilt_button
        camInbuilt.setOnClickListener {
            val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePhotoIntent.putExtra("return-data", true)
            val outputFile = try {
                File.createTempFile(
                    "camera_files" + System.currentTimeMillis(),
                    ".jpg",
                    requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                )
            } catch (e: IOException) {
                e.printStackTrace()
                return@setOnClickListener
            }
            val imageUri = FileProvider.getUriForFile(
                requireContext(),
                "com.example.attendance.provider",
                outputFile
            )
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            camPath = outputFile.absolutePath
            startActivityForResult(takePhotoIntent, 0)
        }
        camInbuilt.alpha = 0f
        camInbuilt.scaleY = 0f
        camInbuilt.translationY = 200f
        camInbuilt.visibility = View.GONE
        val mediaButton = root.media_button
        mediaButton.setOnClickListener {
            val pickPhoto = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(pickPhoto, 1)
        }
        mediaButton.alpha = 0f
        mediaButton.scaleY = 0f
        mediaButton.translationY = 100f
        mediaButton.visibility = View.GONE
        root.import_button
            .setOnClickListener { v: View? ->
                if (overflowState == 0) {
                    overflowState = 1
                    camInbuilt.visibility = View.VISIBLE
                    camInbuilt.animate().alpha(1f).scaleY(1f).translationY(0f)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                camInbuilt.visibility = View.VISIBLE
                            }
                        })
                    mediaButton.visibility = View.VISIBLE
                    mediaButton.animate().alpha(1f).scaleY(1f).translationY(0f)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                mediaButton.visibility = View.VISIBLE
                            }
                        })
                } else if (overflowState == 1) {
                    overflowState = 0
                    camInbuilt.animate().alpha(0f).scaleY(0f).translationY(200f)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                camInbuilt.visibility = View.GONE
                            }
                        })
                    mediaButton.animate().alpha(0f).scaleY(0f).translationY(100f)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                mediaButton.visibility = View.GONE
                            }
                        })
                }
            }
        this.root = root
        return root
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode != Activity.RESULT_OK) {
            Log.d("SUBMIT", "Result code not ok")
            return
        }
        val path =
            if (requestCode == 0) camPath else getRealPathFromURI(intent!!.data)
        Log.d("SUBMIT", path)
        if (resultCode == Activity.RESULT_OK && (requestCode == 1 || requestCode == 0)) {
            val newVal = submitViewModel.imagesData.value.toMutableList()
            newVal += File(path)
            submitViewModel.imagesData.postValue(newVal)
            runBlocking(Dispatchers.Main) {
                Snackbar.make(root, "Imported", 2000)
                    .setAction("UNDO") {
                        undoAddImage()
                    }.show()
            }
        }
    }

    private fun undoAddImage() {
        val newVal1 =
            submitViewModel.imagesData.value.toMutableList()
        newVal1.removeAt(newVal1.size - 1)
        submitViewModel.imagesData.postValue(newVal1)
    }

    private fun getRealPathFromURI(contentUri: Uri?): String {
        val proj = arrayOf(MediaStore.MediaColumns.DATA)
        val cursor =
            requireActivity().contentResolver.query(contentUri!!, proj, null, null, null)
        val path: String = if (cursor!!.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            cursor.getString(columnIndex)
        } else ""
        cursor.close()
        return path
    }

    override fun onResume() {
        super.onResume()
        previewView = root.camera_preview
        if ((ContextCompat.checkSelfPermission(MainActivity.activity, permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
                MainActivity.activity,
                permission.WRITE_EXTERNAL_STORAGE
            )
                    != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
                MainActivity.activity,
                permission.READ_EXTERNAL_STORAGE
            )
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            requestPermissions()
        } else {
            setCamera()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onStop() {
        super.onStop()
        try {
            CameraX.unbindAll()
        } catch (e: Exception) {
        }
    }

    private fun requestPermissions() {
        val customListener: MultiplePermissionsListener = object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                if (report.areAllPermissionsGranted()) setCamera() else root.perms_button.visibility =
                    View.VISIBLE
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
            }
        }
        val failureListener: MultiplePermissionsListener =
            DialogOnAnyDeniedMultiplePermissionsListener.Builder
                .withContext(MainActivity.activity)
                .withTitle("Camera & storage permissions are needed")
                .withButtonText(android.R.string.ok)
                .withIcon(R.drawable.ic_camera)
                .build()
        val listener: MultiplePermissionsListener =
            CompositeMultiplePermissionsListener(customListener, failureListener)
        Dexter.withActivity(MainActivity.activity)
            .withPermissions(
                permission.CAMERA,
                permission.WRITE_EXTERNAL_STORAGE,
                permission.READ_EXTERNAL_STORAGE
            )
            .withListener(listener)
            .check()
    }

    fun setCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(MainActivity.activity)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            CameraX.unbindAll()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(MainActivity.activity))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val preview = Preview.Builder()
            .setCameraSelector(cameraSelector)
            .build()
        preview.setSurfaceProvider((root.camera_preview as PreviewView).previewSurfaceProvider)
        imageCapture = ImageCapture.Builder()
            .setCameraSelector(cameraSelector)
            .setTargetRotation(root.display.rotation)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setCameraSelector(cameraSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(
            { r: Runnable? ->
                Thread(r).start()
            }
        ) { image: ImageProxy ->
            if (scannerType == 0) {
                image.close()
                return@setAnalyzer
            }
            try {
                @SuppressLint("UnsafeExperimentalUsageError")
                val imageMat: Mat? = image.image?.let { Yuv.rgb(it) }
                var rotation = 2
                when (image.imageInfo.rotationDegrees) {
                    90 -> rotation = 0
                    180 -> rotation = 1
                    270 -> rotation = -1
                }
                if (rotation != 2) rotate(imageMat, imageMat, rotation)
                latestImage = imageMat
                var overlay: Mat? = null
                if (scannerType == 1) overlay =
                    MyImageProcessing.drawPagePolygonTransparent(imageMat)
                if (scannerType == 2) overlay = MyImageProcessing.warpImageToPage(imageMat)
                val overlayBitmap: Bitmap
                overlayBitmap = if (overlay == null) {
                    return@setAnalyzer
                } else {
                    MyImageProcessing.matToBitmap(overlay)
                }
                activity?.runOnUiThread {
                    (root.overlay_holder as ImageView).setImageBitmap(
                        overlayBitmap
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                image.close()
            }
        }
        cameraProvider.bindToLifecycle(
            this,
            cameraSelector,
            imageCapture,
            preview,
            imageAnalysis
        )
        with(root) {
            perms_button.animate().alpha(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        perms_button.alpha = 1f
                        perms_button.visibility = View.GONE
                    }
                })
            with(camera_button) {
                visibility = View.VISIBLE
                scaleX = 0f
                scaleY = 0f
                alpha = 0f
                animate().scaleY(1.5f).scaleX(1.5f).alpha(1f)
            }
        }
    }
}
