package com.undrift.ui.components

import androidx.compose.ui.unit.dp
import sv.lib.squircleshape.SquircleShape as libSquircleShape

/**
 * Wrapper around the stoyan-vuchev/squircle-shape library.
 * Uses the library's GPU-accelerated smooth-corner implementation
 * instead of our old CPU-heavy path computation that caused ANRs.
 *
 * Call sites remain unchanged: SquircleShape()
 */
fun SquircleShape() = libSquircleShape(
    radius = 24.dp,
    smoothing = 60
)
