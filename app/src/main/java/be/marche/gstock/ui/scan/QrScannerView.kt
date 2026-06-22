package be.marche.gstock.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import be.marche.gstock.ui.common.MessageBox
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Full-screen camera preview that decodes QR / barcodes and reports decoded values.
 * Handles the runtime camera permission itself.
 *
 * The same code held in front of the camera is reported only once per [SCAN_COOLDOWN_MS]; a
 * different code is reported immediately. This lets several codes be scanned in a row (e.g. a
 * tool cart) without a re-arm signal, and without spamming callbacks while one code stays in view.
 */
@Composable
fun QrScannerView(
    onQrScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission) {
        CameraPreview(onQrScanned = onQrScanned, modifier = modifier)
    } else {
        MessageBox(
            message = "Camera permission is required to scan QR codes.",
            modifier = modifier,
            onRetry = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        )
    }
}

@Composable
private fun CameraPreview(
    onQrScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnQrScanned by rememberUpdatedState(onQrScanned)
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    // clipToBounds() is essential: PreviewView scales the preview (FILL_CENTER) so the inner
    // TextureView is larger than this box and centered, which otherwise paints the camera image
    // over the composables above and below it (e.g. the worker banner and the cart).
    Box(modifier.fillMaxSize().clipToBounds()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    // COMPATIBLE (TextureView) instead of the default PERFORMANCE (SurfaceView):
                    // a SurfaceView renders in its own window and paints over adjacent Compose UI
                    // (e.g. the worker banner above the preview), making it disappear. A TextureView
                    // composes normally in the view hierarchy, so surrounding UI stays visible.
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { ia ->
                            ia.setAnalyzer(
                                analysisExecutor,
                                QrAnalyzer { value -> currentOnQrScanned(value) },
                            )
                        }
                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )
        // Reticle marking the frame center: only the code aimed at here is reported.
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.6f)
                .aspectRatio(1f)
                .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
        )
    }
}

/** Minimum gap before the *same* value is reported again, to avoid spamming while it stays in view. */
private const val SCAN_COOLDOWN_MS = 2500L

/**
 * A code must stay centered for this long before it is reported. This debounces panning across a
 * sheet of codes so a lookup only fires for the one the user settles the reticle on.
 */
private const val CENTER_DWELL_MS = 400L

/**
 * How close a code's center must be to the frame center to count as "aimed at", as a fraction of the
 * smaller image dimension. Roughly matches the on-screen reticle.
 */
private const val CENTER_RADIUS_FRACTION = 0.25f

private class QrAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()
    private var lastValue: String? = null
    private var lastReportedAt = 0L
    // The value currently under the reticle and when it first got there, for dwell-time debouncing.
    private var candidateValue: String? = null
    private var candidateSince = 0L

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        // ML Kit reports bounding boxes in the upright (post-rotation) image space, so swap the
        // dimensions for 90°/270° to compute the center in that same space.
        val rotated = rotation == 90 || rotation == 270
        val width = if (rotated) mediaImage.height else mediaImage.width
        val height = if (rotated) mediaImage.width else mediaImage.height
        val image = InputImage.fromMediaImage(mediaImage, rotation)
        scanner.process(image)
            .addOnSuccessListener { barcodes -> handleBarcodes(barcodes, width, height) }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun handleBarcodes(barcodes: List<Barcode>, width: Int, height: Int) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * CENTER_RADIUS_FRACTION

        // The aimed-at code: the one whose bounding-box center is nearest the frame center, and only
        // if it falls within the reticle radius.
        val value = barcodes
            .filter { it.rawValue != null && it.boundingBox != null }
            .minByOrNull { it.boundingBox!!.distanceSquaredTo(cx, cy) }
            ?.takeIf { it.boundingBox!!.distanceSquaredTo(cx, cy) <= radius * radius }
            ?.rawValue

        val now = System.currentTimeMillis()
        if (value == null) {
            // Nothing aimed at: clear the candidate so a code must dwell again before firing.
            candidateValue = null
            return
        }
        // Restart the dwell timer whenever the aimed-at value changes.
        if (value != candidateValue) {
            candidateValue = value
            candidateSince = now
            return
        }
        if (now - candidateSince < CENTER_DWELL_MS) return

        // Dwell satisfied: report a new value immediately, repeat the same one only after cooldown.
        if (value != lastValue || now - lastReportedAt >= SCAN_COOLDOWN_MS) {
            lastValue = value
            lastReportedAt = now
            onResult(value)
        }
    }
}

private fun android.graphics.Rect.distanceSquaredTo(x: Float, y: Float): Float {
    val dx = exactCenterX() - x
    val dy = exactCenterY() - y
    return dx * dx + dy * dy
}
