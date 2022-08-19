package com.shadowings.mnistclassifier

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap
import com.shadowings.mnistclassifier.ml.Model
import com.smarttoolfactory.gesture.pointerMotionEvents
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


@Composable
fun CaptureBitmap(
    content: @Composable () -> Unit,
): () -> Bitmap {

    val context = LocalContext.current

    /**
     * ComposeView that would take composable as its content
     * Kept in remember so recomposition doesn't re-initialize it
     **/
    val composeView = remember { ComposeView(context) }

    /**
     * Callback function which could get latest image bitmap
     **/
    fun captureBitmap(): Bitmap {
        return composeView.drawToBitmap()
    }

    /** Use Native View inside Composable **/
    AndroidView(
        factory = {
            composeView.apply {
                setContent {
                    content.invoke()
                }
            }
        }
    )

    /** returning callback to bitmap **/
    return ::captureBitmap
}

@Composable
fun MainComposable() {
    var motionEvent by remember { mutableStateOf(MotionEvent.Idle) }
    var currentPosition by remember { mutableStateOf(Offset.Unspecified) }
    var previousPosition by remember { mutableStateOf(Offset.Unspecified) }
    var path by remember { mutableStateOf(Path()) }
    val drawModifier = Modifier
        .pointerMotionEvents(
            onDown = { pointerInputChange: PointerInputChange ->
                currentPosition = pointerInputChange.position
                motionEvent = MotionEvent.Down
                pointerInputChange.consume()
            },
            onMove = { pointerInputChange: PointerInputChange ->
                currentPosition = pointerInputChange.position
                motionEvent = MotionEvent.Move
                pointerInputChange.consume()
            },
            onUp = { pointerInputChange: PointerInputChange ->
                motionEvent = MotionEvent.Up
                pointerInputChange.consume()
            },
            delayAfterDownInMillis = 25L
        )
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(screenWidth / 20),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.padding(8.dp),
            text = "Draw a digit and press \"Classify\"",
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        val snapShot = CaptureBitmap {
            Canvas(
                modifier = drawModifier
                    .fillMaxWidth()
                    .height(screenWidth - screenWidth / 10)
                    .background(Color.Black)
            ) {
                when (motionEvent) {
                    MotionEvent.Down -> {
                        path.moveTo(currentPosition.x, currentPosition.y)
                        previousPosition = currentPosition
                    }

                    MotionEvent.Move -> {
                        path.quadraticBezierTo(
                            previousPosition.x,
                            previousPosition.y,
                            (previousPosition.x + currentPosition.x) / 2,
                            (previousPosition.y + currentPosition.y) / 2

                        )
                        previousPosition = currentPosition
                    }

                    MotionEvent.Up -> {
                        path.lineTo(currentPosition.x, currentPosition.y)
                        currentPosition = Offset.Unspecified
                        previousPosition = currentPosition
                        motionEvent = MotionEvent.Idle
                    }

                    else -> Unit
                }

                drawPath(
                    color = Color.White,
                    path = path,
                    style = Stroke(
                        width = 32.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
        Button(onClick = { path = Path() }, modifier = Modifier.padding(8.dp)) {
            Text(text = "Clear", fontSize = 18.sp)
        }
        Button(onClick = {

            val bitmap = snapShot.invoke()
            val scaled = Bitmap.createScaledBitmap(bitmap, 28, 28, false)

            val model = Model.newInstance(context)

            val image = TensorBuffer.createFixedSize(intArrayOf(1, 28, 28, 1), DataType.UINT8)

            image.loadBuffer(getGrayscaleBuffer(scaled))

            val outputs = model.process(image)
            val probability = outputs.probabilityAsCategoryList
            val intProb = probability.map { (it.score * 100).toInt() }
            val maxProb = intProb.maxOrNull() ?: 0
            val index = intProb.indexOf(maxProb).toString()
            model.close()
            Toast.makeText(
                context,
                "Predicted $index with $maxProb% probability",
                Toast.LENGTH_SHORT
            ).show()
        }, modifier = Modifier.padding(8.dp)) {
            Text(text = "Classify", fontSize = 18.sp)
        }
        Button(onClick = {
            val url =
                "https://emmanuelevilla.com/mnist-digit-classification-with-tensorflow-on-android/"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }, modifier = Modifier.padding(8.dp)) {
            Text(text = "Learn more", fontSize = 18.sp)
        }
    }

}