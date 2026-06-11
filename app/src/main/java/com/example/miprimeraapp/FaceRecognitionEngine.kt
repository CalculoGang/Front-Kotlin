package com.example.miprimeraapp

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

    fun distancia(v1: List<Float>, v2: List<Float>): Float {
        if (v1.size != v2.size || v1.isEmpty()) return Float.MAX_VALUE
        var dot = 0f
        for (i in v1.indices) dot += v1[i] * v2[i]
        return 1f - dot
    }

    fun reconocer(
        vector: List<Float>,
        personas: Map<String, List<List<Float>>>,
        umbral: Float = 0.40f
    ): Pair<String?, Float> {
        var mejorNombre: String? = null
        var mejorDist = Float.MAX_VALUE

        for ((nombre, vectores) in personas) {
            val centroid = centroide(vectores) ?: continue
            val d = distancia(vector, centroid)
            if (d < mejorDist) {
                mejorDist = d
                mejorNombre = nombre
            }
        }

        return Pair(if (mejorDist <= umbral) mejorNombre else null, mejorDist)
    }

    /** Mean of the stored embeddings, re-normalized to unit length. */
    private fun centroide(vectores: List<List<Float>>): List<Float>? {
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
