package com.example.miprimeraapp

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageUtilsTest {

    // alignFace's Matrix needs Android graphics (not on JVM); test the pure sizing math.
    @Test fun alignSide_addsMarginOnBothSidesOfLongerEdge() {
        // 200px box, 25% margin -> 200 * 1.5 = 300
        assertEquals(300f, ImageUtils.alignSide(200, 120, 0.25f), 1e-3f)
        // longer edge wins
        assertEquals(300f, ImageUtils.alignSide(120, 200, 0.25f), 1e-3f)
        // zero margin -> just the box
        assertEquals(200f, ImageUtils.alignSide(200, 200, 0f), 1e-3f)
    }
}
