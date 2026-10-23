package com.example.fantasmandroid

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeomSize
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.fantasmandroid.ui.theme.AccentGreen
import com.example.fantasmandroid.ui.theme.SignalGreen
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Bridge between the Compose HUD and the bound camera. Session recording and
 * still capture go through this so the UI never has to own CameraX handles.
 */
class CameraHandle {
    internal var onCaptureThumbnail: (File) -> Unit = {}
    internal var onStartVideo: (File) -> Boolean = { false }
    internal var onStopVideo: () -> Unit = {}

    fun captureThumbnail(file: File) = onCaptureThumbnail(file)
    fun startVideo(file: File): Boolean = onStartVideo(file)
    fun stopVideo() = onStopVideo()
}

private data class SkeletonFrame(
    val lines: List<Pair<Offset, Offset>>,
    val joints: List<Offset>,
    val headJoints: List<Offset>
)

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraLayer(
    settings: FantasmSettings,
    handle: CameraHandle,
    videoRecorder: SessionVideoRecorder,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var skeleton by remember { mutableStateOf(SkeletonFrame(emptyList(), emptyList(), emptyList())) }
    var depthMask by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageAspect by remember { mutableFloatStateOf(4f / 3f) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var pendingThumb by remember { mutableStateOf<File?>(null) }

    val latestSettings = rememberUpdatedState(settings)

    val poseDetector = remember {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
        )
    }
    val segmenter = remember {
        Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
                .build()
        )
    }

    LaunchedEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            provider = runCatching { future.get() }.getOrNull()
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { provider?.unbindAll() }
            runCatching { poseDetector.close() }
            runCatching { segmenter.close() }
            executor.shutdown()
        }
    }

    LaunchedEffect(provider, previewView, settings.frontCamera) {
        val cameraProvider = provider ?: return@LaunchedEffect
        val requested = if (settings.frontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        val selector = if (cameraProvider.hasCamera(requested)) {
            requested
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        handle.onCaptureThumbnail = { file -> pendingThumb = file }
        handle.onStartVideo = { file -> videoRecorder.start(file) }
        handle.onStopVideo = { videoRecorder.stop() }

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val videoCapture = VideoCapture.withOutput(videoRecorder.recorder)
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(640, 480),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
            )
            .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
            .build()

        var frameCounter = 0
        analysis.setAnalyzer(executor) { proxy ->
            val current = latestSettings.value
            val wantPose = current.slsEnabled
            val wantDepth = current.depthRenderEnabled
            val thumb = pendingThumb
            val rotation = proxy.imageInfo.rotationDegrees

            val uprightWidth: Int
            val uprightHeight: Int
            val image = proxy.image
            if (image == null) {
                proxy.close()
                return@setAnalyzer
            }
            if (rotation == 90 || rotation == 270) {
                uprightWidth = image.height
                uprightHeight = image.width
            } else {
                uprightWidth = image.width
                uprightHeight = image.height
            }
            imageAspect = uprightWidth.toFloat() / uprightHeight.toFloat()

            if (thumb != null) {
                pendingThumb = null
                runCatching { proxy.saveJpeg(thumb, rotation) }
            }

            if (!wantPose && !wantDepth) {
                skeleton = SkeletonFrame(emptyList(), emptyList(), emptyList())
                depthMask = null
                proxy.close()
                return@setAnalyzer
            }

            frameCounter++
            val input = InputImage.fromMediaImage(image, rotation)
            val poseTask = if (wantPose) {
                poseDetector.process(input).addOnSuccessListener { pose ->
                    skeleton = pose.toSkeleton(uprightWidth, uprightHeight)
                }
            } else {
                null
            }
            val segTask = if (wantDepth && frameCounter % 2 == 0) {
                segmenter.process(input).addOnSuccessListener { mask ->
                    depthMask = buildDepthMask(mask)?.asImageBitmap()
                }
            } else {
                null
            }

            val tasks = listOfNotNull(poseTask, segTask)
            if (tasks.isEmpty()) {
                proxy.close()
            } else {
                Tasks.whenAllComplete(tasks).addOnCompleteListener { proxy.close() }
            }
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                videoCapture,
                analysis
            )
            cameraError = null
        } catch (t: Throwable) {
            cameraError = t.message ?: "Camera unavailable"
            depthMask = null
            skeleton = SkeletonFrame(emptyList(), emptyList(), emptyList())
        }
    }

    LaunchedEffect(settings.slsEnabled) {
        if (!settings.slsEnabled) {
            skeleton = SkeletonFrame(emptyList(), emptyList(), emptyList())
        }
    }
    LaunchedEffect(settings.depthRenderEnabled) {
        if (!settings.depthRenderEnabled) depthMask = null
    }

    val noise = remember { makeNoiseBitmap() }
    val scanTransition = rememberInfiniteTransition(label = "laser")
    val scanY by scanTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laserY"
    )

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewWidth = size.width
            val viewHeight = size.height
            val scale = max(viewWidth / imageAspect, viewHeight)
            val displayWidth = imageAspect * scale
            val displayHeight = scale
            val offsetX = (viewWidth - displayWidth) / 2f
            val offsetY = (viewHeight - displayHeight) / 2f

            if (settings.twilightEnabled) {
                drawRect(Color(0xFF19FF6A).copy(alpha = 0.26f))
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xE6001504)),
                        center = center,
                        radius = max(viewWidth, viewHeight) * 0.75f
                    )
                )
                drawRect(color = Color(0x33001A05))
                drawImage(
                    image = noise,
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(viewWidth.toInt(), viewHeight.toInt()),
                    alpha = 0.10f,
                    blendMode = BlendMode.Plus
                )
            }

            val mask = depthMask
            if (settings.depthRenderEnabled && mask != null) {
                drawRect(Color(0xCC02060A))
                drawImage(
                    image = mask,
                    dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                    dstSize = IntSize(displayWidth.toInt(), displayHeight.toInt()),
                    alpha = 0.95f
                )
                // Laser sweep line.
                val y = offsetY + displayHeight * scanY
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xAA25E7E7), Color.Transparent)
                    ),
                    start = Offset(offsetX, y),
                    end = Offset(offsetX + displayWidth, y),
                    strokeWidth = 2f
                )
            }

            if (settings.slsEnabled) {
                drawSkeleton(
                    frame = skeleton,
                    displayWidth = displayWidth,
                    displayHeight = displayHeight,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    mirrored = settings.frontCamera
                )
            }
        }

        if (cameraError != null) {
            Text(
                text = "CAMERA UNAVAILABLE\n${cameraError}",
                color = Color(0xCCFF6B5A),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
        }
    }
}

private fun DrawScope.drawSkeleton(
    frame: SkeletonFrame,
    displayWidth: Float,
    displayHeight: Float,
    offsetX: Float,
    offsetY: Float,
    mirrored: Boolean
) {
    fun place(point: Offset): Offset {
        val x = if (mirrored) 1f - point.x else point.x
        return Offset(
            offsetX + x * displayWidth,
            offsetY + point.y * displayHeight
        )
    }

    frame.lines.forEach { (start, end) ->
        val a = place(start)
        val b = place(end)
        drawLine(
            color = SignalGreen.copy(alpha = 0.18f),
            start = a,
            end = b,
            strokeWidth = 10f
        )
        drawLine(
            color = SignalGreen,
            start = a,
            end = b,
            strokeWidth = 3f
        )
    }
    frame.joints.forEach { joint ->
        val center = place(joint)
        drawCircle(color = Color.Black.copy(alpha = 0.85f), radius = 5.5f, center = center)
        drawCircle(color = Color.White, radius = 3.6f, center = center)
    }
    frame.headJoints.forEach { joint ->
        val center = place(joint)
        drawCircle(color = Color.Black.copy(alpha = 0.85f), radius = 6.5f, center = center)
        drawCircle(color = Color.White, radius = 4.3f, center = center)
        drawCircle(color = SignalGreen.copy(alpha = 0.35f), radius = 9f, center = center)
    }
}

private fun Pose.toSkeleton(uprightWidth: Int, uprightHeight: Int): SkeletonFrame {
    if (uprightWidth <= 0 || uprightHeight <= 0) {
        return SkeletonFrame(emptyList(), emptyList(), emptyList())
    }
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
        PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
        PoseLandmark.NOSE to PoseLandmark.LEFT_EYE,
        PoseLandmark.NOSE to PoseLandmark.RIGHT_EYE,
        PoseLandmark.LEFT_EYE to PoseLandmark.LEFT_EAR,
        PoseLandmark.RIGHT_EYE to PoseLandmark.RIGHT_EAR,
        PoseLandmark.LEFT_EAR to PoseLandmark.LEFT_SHOULDER,
        PoseLandmark.RIGHT_EAR to PoseLandmark.RIGHT_SHOULDER,
        PoseLandmark.NOSE to PoseLandmark.LEFT_MOUTH,
        PoseLandmark.NOSE to PoseLandmark.RIGHT_MOUTH
    )
    val headTypes = setOf(
        PoseLandmark.NOSE,
        PoseLandmark.LEFT_EYE,
        PoseLandmark.RIGHT_EYE,
        PoseLandmark.LEFT_EAR,
        PoseLandmark.RIGHT_EAR,
        PoseLandmark.LEFT_MOUTH,
        PoseLandmark.RIGHT_MOUTH
    )

    fun landmark(type: Int): Offset? {
        val point = getPoseLandmark(type) ?: return null
        if (point.inFrameLikelihood < 0.45f) return null
        val nx = point.position.x / uprightWidth
        val ny = point.position.y / uprightHeight
        if (nx !in -0.2f..1.2f || ny !in -0.2f..1.2f) return null
        return Offset(nx, ny)
    }

    val lines = mutableListOf<Pair<Offset, Offset>>()
    val joints = mutableListOf<Offset>()
    val head = mutableListOf<Offset>()

    for ((startType, endType) in connections) {
        val start = landmark(startType) ?: continue
        val end = landmark(endType) ?: continue
        lines.add(start to end)
        if (startType in headTypes) head.add(start) else joints.add(start)
        if (endType in headTypes) head.add(end) else joints.add(end)
    }

    return SkeletonFrame(
        lines = lines,
        joints = joints.distinct(),
        headJoints = head.distinct()
    )
}

/** Converts the ML Kit confidence mask into a FLIR-style false-colour depth render. */
private fun buildDepthMask(mask: SegmentationMask): Bitmap? {
    val srcWidth = mask.width
    val srcHeight = mask.height
    if (srcWidth <= 0 || srcHeight <= 0) return null
    val buffer = mask.buffer
    buffer.rewind()
    val floatBuffer = buffer.asFloatBuffer()

    val outWidth = 128
    val outHeight = (srcHeight.toFloat() / srcWidth * outWidth).toInt().coerceAtLeast(1)
    val pixels = IntArray(outWidth * outHeight)
    val cx = srcWidth / 2f
    val cy = srcHeight / 2f

    for (oy in 0 until outHeight) {
        // ML Kit raw masks are bottom-up; flip rows into screen order.
        val invertedRow = outHeight - 1 - oy
        val sy = (invertedRow * srcHeight / outHeight).coerceIn(0, srcHeight - 1)
        for (ox in 0 until outWidth) {
            val sx = (ox * srcWidth / outWidth).coerceIn(0, srcWidth - 1)
            val index = sy * srcWidth + sx
            val confidence = floatBuffer.get(index).coerceIn(0f, 1f)
            if (confidence < 0.24f) {
                pixels[oy * outWidth + ox] = 0
                continue
            }
            val dx = (sx - cx) / cx
            val dy = (sy - cy) / cy
            val distance = sqrt(dx * dx + dy * dy).coerceIn(0f, 1.4f)
            val heat = (confidence * 0.55f + confidence * 0.55f * (1f - distance / 1.6f))
                .coerceIn(0f, 1f)
            val alpha = (confidence * 235f).toInt().coerceIn(0, 255)
            pixels[oy * outWidth + ox] = (alpha shl 24) or (falseColour(heat) and 0x00FFFFFF)
        }
    }

    val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, outWidth, 0, 0, outWidth, outHeight)
    return bitmap
}

private fun falseColour(heat: Float): Int {
    val stops = floatArrayOf(0f, 0.22f, 0.42f, 0.62f, 0.8f, 1f)
    val colors = intArrayOf(
        0xFF0A25FF.toInt(),
        0xFF04A8FF.toInt(),
        0xFF00F03C.toInt(),
        0xFFFFE600.toInt(),
        0xFFFF7A00.toInt(),
        0xFFFF2412.toInt()
    )
    var index = 0
    while (index < stops.size - 2 && heat > stops[index + 1]) index++
    val t = ((heat - stops[index]) / (stops[index + 1] - stops[index])).coerceIn(0f, 1f)
    val from = colors[index]
    val to = colors[index + 1]
    val r = ((from shr 16 and 0xFF) + ((to shr 16 and 0xFF) - (from shr 16 and 0xFF)) * t).toInt()
    val g = ((from shr 8 and 0xFF) + ((to shr 8 and 0xFF) - (from shr 8 and 0xFF)) * t).toInt()
    val b = ((from and 0xFF) + ((to and 0xFF) - (from and 0xFF)) * t).toInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

private fun ImageProxy.saveJpeg(file: File, rotationDegrees: Int) {
    val bitmap = toBitmap()
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    file.parentFile?.mkdirs()
    FileOutputStream(file).use { out ->
        rotated.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
}

private fun makeNoiseBitmap(): ImageBitmap {
    val size = 96
    val random = Random(7)
    val pixels = IntArray(size * size) {
        val v = 60 + random.nextInt(170)
        (0x22 shl 24) or (v shl 16) or (v shl 8) or v
    }
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap.asImageBitmap()
}
