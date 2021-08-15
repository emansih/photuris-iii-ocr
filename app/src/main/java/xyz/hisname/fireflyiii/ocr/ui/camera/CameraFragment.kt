/*
 * Copyright (c)  2021 ASDF Dev Pte. Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.hisname.fireflyiii.ocr.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import xyz.hisname.fireflyiii.ocr.R
import xyz.hisname.fireflyiii.ocr.databinding.FragmentCameraBinding
import xyz.hisname.fireflyiii.ocr.ui.ProgressBar
import xyz.hisname.fireflyiii.ocr.utils.extension.getViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment: Fragment() {

    private var fragmentCameraBinding: FragmentCameraBinding? = null
    private val binding get() = fragmentCameraBinding!!
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var photoFile: File
    private val cameraViewModel by lazy { getViewModel(CameraViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (allPermissionsGranted()){
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        binding.cameraCapture.setOnClickListener {
            takePhoto()
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
        openCloseButton()

        cameraViewModel.isLoading.observe(viewLifecycleOwner){ loader ->
            if(loader){
                ProgressBar.animateView(binding.progressLayout.progressOverlay, View.VISIBLE, 0.4f)
            } else {
                ProgressBar.animateView(binding.progressLayout.progressOverlay, View.GONE, 0f)
            }
        }
    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        photoFile = File(outputDirectory,
                SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions,
                ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                exc.printStackTrace()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                binding.imageView.isVisible = true
                Glide.with(requireContext())
                    .load(photoFile)
                    .into(binding.imageView)
                binding.viewFinder.isGone = true
                binding.cameraCapture.isGone = true
                binding.closeButton.isVisible = true
                binding.submitButton.isVisible = true
            }
        })
    }

    private fun openCloseButton(){
        binding.closeButton.setOnClickListener {
            binding.imageView.isVisible = false
            binding.viewFinder.isVisible = true
            binding.closeButton.isGone = true
            binding.cameraCapture.isVisible = true
            binding.submitButton.isGone = true
            photoFile.delete()
        }
        binding.submitButton.setOnClickListener {
            cameraViewModel.analyseImage(photoFile, requireContext().contentResolver).observe(viewLifecycleOwner){ receiptData ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Scanned Data")
                    .setMessage("Description: " + receiptData?.merchantName + "\n" +
                            "Amount: " + receiptData?.totalPrice + "\n" +
                            "Date: "  + receiptData?.date + "\n" +
                            "Time: " + receiptData?.time)
                    .setPositiveButton("OK"){ _,_ ->

                    }
                    .show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

            imageCapture = ImageCapture.Builder()
                    .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) { }

        }, ContextCompat.getMainExecutor(requireContext()))

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireActivity().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else requireActivity().filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}