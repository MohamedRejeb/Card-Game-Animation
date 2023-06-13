package com.mohamedrejeb.cardsgame

import androidx.compose.ui.geometry.Offset
import kotlin.math.pow
import kotlin.math.sqrt

fun calculateDistanceBetweenTwoPoints(p1: Offset, p2: Offset): Float {
    return calculateDistanceBetweenTwoPoints(p1.x, p1.y, p2.x, p2.y)
}

fun calculateDistanceBetweenTwoPoints(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    return sqrt((x2 - x1).toDouble().pow(2.0) + (y2 - y1).toDouble().pow(2.0)).toFloat()
}