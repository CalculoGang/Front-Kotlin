package com.example.kigoapp.face

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object ImageUtils {

    /** YUV_420_888 ImageProxy -> Bitmap via NV21/JPEG. */
    fun toBitmap(image: ImageProxy): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuv = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuv.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val bytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    fun rotate(bmp: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bmp
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    /**
     * Aligned face crop for the embedder: levels the eyes (rolls by [rollDeg]),
     * centers on the face box, and adds [margin] padding — MobileFaceNet was
     * trained on aligned crops with margin, so a raw tight box embeds poorly.
     *
     * Output is a square [outSize] bitmap drawn in one Matrix pass (rotate +
     * scale + center), so the embedder's later resize is a no-op when
     * outSize == model inputSize.
     *
     * @param rollDeg face roll in degrees (ML Kit `headEulerAngleZ`).
     */
    fun alignFace(
        bmp: Bitmap,
        box: Rect,
        rollDeg: Float,
        outSize: Int,
        margin: Float = 0.25f,
    ): Bitmap? {
        val side = alignSide(box.width(), box.height(), margin)
        if (side < 1f || outSize < 1) return null
        val cx = box.exactCenterX()
        val cy = box.exactCenterY()
        val scale = outSize / side

        val out = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val m = Matrix().apply {
            postTranslate(-cx, -cy)           // face center -> origin
            postRotate(-rollDeg)              // ponytail: level eyes; flip sign if faces come out tilted the wrong way
            postScale(scale, scale)           // box side -> outSize
            postTranslate(outSize / 2f, outSize / 2f)
        }
        Canvas(out).apply {
            drawColor(Color.BLACK)            // pad out-of-bounds with black, not garbage
            drawBitmap(bmp, m, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        return out
    }

    /** Square crop side: the longer box edge plus [margin] padding on each side. */
    internal fun alignSide(boxW: Int, boxH: Int, margin: Float): Float =
        maxOf(boxW, boxH) * (1f + 2f * margin)
}
