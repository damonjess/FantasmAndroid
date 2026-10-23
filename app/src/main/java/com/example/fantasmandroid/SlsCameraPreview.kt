package com.example.fantasmandroid

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors

data class NormalizedLine(val start: Offset, val end: Offset)

@OptIn(ExperimentalGetImage::class)
@Composable
fun SlsCameraPreview(
    slsEnabled: Boolean,
    twilightEnabled: Boolean,
    flipCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var skeletonLines by remember { mutableStateOf<List<NormalizedLine>>(emptyList()) }
    var skeletonJoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val poseDetector = remember {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Rebind whenever front/back camera is toggled
    LaunchedEffect(flipCamera, cameraProvider, previewView) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val view = previewView ?: return@LaunchedEffect

        val cameraSelector = if (flipCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(view.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null && slsEnabled) {
                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                val isRotated = imageProxy.imageInfo.rotationDegrees % 180 != 0
                val imgWidth = if (isRotated) inputImage.height.toFloat() else inputImage.width.toFloat()
                val imgHeight = if (isRotated) inputImage.width.toFloat() else inputImage.height.toFloat()

                poseDetector.process(inputImage)
                    .addOnSuccessListener { pose ->
                        val (lines, joints) = extractSkeleton(pose, imgWidth, imgHeight)
                        skeletonLines = lines
                        skeletonJoints = joints
                    }
                    .addOnCompleteListener { imageProxy.close() }
            } else {
                skeletonLines = emptyList()
                skeletonJoints = emptyList()
                imageProxy.close()
            }
        }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { view ->
                    previewView = view
                    ProcessCameraProvider.getInstance(ctx).addListener({
                        cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
                    }, ContextCompat.getMainExecutor(ctx))
                }
            }
        )

        if (twilightEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x4400FF66))
            )
        }

        if (slsEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                skeletonLines.forEach { line ->
                    drawLine(
                        color = Color(0xFF00FF00),
                        start = Offset(line.start.x * size.width, line.start.y * size.height),
                        end = Offset(line.end.x * size.width, line.end.y * size.height),
                        strokeWidth = 8f
                    )
                }
                skeletonJoints.forEach { joint ->
                    val center = Offset(joint.x * size.width, joint.y * size.height)
                    drawCircle(color = Color.Black, radius = 12f, center = center)
                    drawCircle(color = Color.White, radius = 9f, center = center)
                }
            }
        }
    }
}

private fun extractSkeleton(
    pose: Pose,
    width: Float,
    height: Float
): Pair<List<NormalizedLine>, List<Offset>> {
    val connections = listOf(
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
        PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
        PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
        PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
        PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
        PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
        PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
        PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
    )

    val lines = mutableListOf<NormalizedLine>()
    val joints = mutableListOf<Offset>()

    for ((startType, endType) in connections) {
        val startLandmark = pose.getPoseLandmark(startType)
        val endLandmark = pose.getPoseLandmark(endType)
        if (startLandmark != null && endLandmark != null &&
            startLandmark.inFrameLikelihood > 0.5f && endLandmark.inFrameLikelihood > 0.5f
        ) {
            val startOffset = Offset(startLandmark.position.x / width, startLandmark.position.y / height)
            val endOffset = Offset(endLandmark.position.x / width, endLandmark.position.y / height)
            lines.add(NormalizedLine(startOffset, endOffset))
            joints.add(startOffset)
            joints.add(endOffset)
        }
    }
    return lines to joints.distinct()
}