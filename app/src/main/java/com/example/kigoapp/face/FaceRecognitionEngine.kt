package com.example.kigoapp.face

import kotlin.math.sqrt

/**
 * Identity matching over L2-normalized face embeddings produced by
 * [FaceEmbedder]. Distance is cosine distance (1 - cosine similarity);
 * because embeddings are unit-length the dot product IS the cosine.
 *
 * Typical separation for MobileFaceNet:
 *   same person     -> distance ~0.05 - 0.30
 *   different person -> distance ~0.60 - 0.90
 * Default threshold 0.40 sits in the gap.
 */
object FaceRecognitionEngine {

    /** Mean of the embeddings, re-normalized to unit length (null if empty). */
    fun promediar(vectores: List<List<Float>>): List<Float>? {
        if (vectores.isEmpty()) return null
        val len = vectores[0].size
        val sumas = FloatArray(len)
        var n = 0
        for (v in vectores) {
            if (v.size != len) continue
            for (i in v.indices) sumas[i] += v[i]
            n++
        }
        if (n == 0) return null

        var norm = 0f
        for (i in sumas.indices) {
            sumas[i] /= n
            norm += sumas[i] * sumas[i]
        }
        norm = sqrt(norm)
        if (norm == 0f) return null
        return List(len) { sumas[it] / norm }
    }
}
