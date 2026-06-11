package com.example.miprimeraapp

import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.sqrt

object FaceRecognitionEngine {

    private val LANDMARKS = listOf(
        FaceLandmark.LEFT_EYE,
        FaceLandmark.RIGHT_EYE,
        FaceLandmark.NOSE_BASE,
        FaceLandmark.MOUTH_LEFT,
        FaceLandmark.MOUTH_RIGHT,
        FaceLandmark.MOUTH_BOTTOM,
        FaceLandmark.LEFT_CHEEK,
        FaceLandmark.RIGHT_CHEEK,
        FaceLandmark.LEFT_EAR,
        FaceLandmark.RIGHT_EAR
    )

    fun extraerVector(rostro: Face): List<Float>? {
        val box = rostro.boundingBox
        val w = box.width().toFloat()
        val h = box.height().toFloat()
        if (w == 0f || h == 0f) return null

        val vals = mutableListOf<Float>()
        for (tipo in LANDMARKS) {
            val lm = rostro.getLandmark(tipo)
            if (lm != null) {
                vals.add((lm.position.x - box.left) / w)
                vals.add((lm.position.y - box.top) / h)
            } else {
                vals.add(-1f)
                vals.add(-1f)
            }
        }
        return vals
    }

    fun distancia(v1: List<Float>, v2: List<Float>): Float {
        if (v1.size != v2.size) return Float.MAX_VALUE
        var suma = 0f
        var count = 0
        for (i in v1.indices) {
            if (v1[i] < 0f || v2[i] < 0f) continue
            val d = v1[i] - v2[i]
            suma += d * d
            count++
        }
        return if (count == 0) Float.MAX_VALUE else sqrt(suma)
    }

    fun reconocer(
        vector: List<Float>,
        personas: Map<String, List<List<Float>>>,
        umbral: Float = 0.20f
    ): Pair<String?, Float> {
        var mejorNombre: String? = null
        var mejorDist = Float.MAX_VALUE
        for ((nombre, vectores) in personas) {
            for (v in vectores) {
                val d = distancia(vector, v)
                if (d < mejorDist) {
                    mejorDist = d
                    mejorNombre = nombre
                }
            }
        }
        return Pair(
            if (mejorDist <= umbral) mejorNombre else null,
            mejorDist
        )
    }
}
