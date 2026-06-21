package com.example.miprimeraapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Wraps a TFLite face-embedding model (e.g. MobileFaceNet 112x112 -> 192,
 * or FaceNet 160x160 -> 128). Input/output sizes are read from the model
 * tensors so either model works without code changes.
 *
 * Place the model at: app/src/main/assets/mobilefacenet.tflite
 */
class FaceEmbedder(context: Context, modelAsset: String = "mobilefacenet.tflite") {

    private val interpreter: Interpreter
    val inputSize: Int
    val embeddingSize: Int

    init {
        val afd = context.assets.openFd(modelAsset)
        val channel = afd.createInputStream().channel
        val model = channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        interpreter = Interpreter(model, Interpreter.Options().apply { numThreads = 4 })

        val inShape = interpreter.getInputTensor(0).shape()   // [1, H, W, 3]
        inputSize = inShape[1]
        val outShape = interpreter.getOutputTensor(0).shape() // [1, N]
        embeddingSize = outShape[outShape.size - 1]
        // El backend exige vector(128). Si esto NO imprime 128, el modelo no coincide.
        Log.i("FaceEmbedder", "inputSize=$inputSize embeddingSize=$embeddingSize (backend espera 128)")
    }

    /** Returns an L2-normalized embedding for the cropped face bitmap. */
    fun embed(face: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(face, inputSize, inputSize, true)
        val input = ByteBuffer
            .allocateDirect(inputSize * inputSize * 3 * 4)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            input.putFloat((r - 127.5f) / 128f)
            input.putFloat((g - 127.5f) / 128f)
            input.putFloat((b - 127.5f) / 128f)
        }
        input.rewind()

        val out = Array(1) { FloatArray(embeddingSize) }
        interpreter.run(input, out)
        return l2normalize(out[0])
    }

    fun close() = interpreter.close()

    private fun l2normalize(v: FloatArray): FloatArray {
        var s = 0f
        for (x in v) s += x * x
        val n = sqrt(s)
        return if (n == 0f) v else FloatArray(v.size) { v[it] / n }
    }
}
