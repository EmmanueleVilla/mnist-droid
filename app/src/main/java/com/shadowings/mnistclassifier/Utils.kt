package com.shadowings.mnistclassifier

import android.graphics.Bitmap
import android.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun getGrayscaleBuffer(bitmap: Bitmap): ByteBuffer {
    val width = bitmap.width
    val height = bitmap.height
    val mImgData: ByteBuffer = ByteBuffer
        .allocateDirect(width * height)
    mImgData.order(ByteOrder.nativeOrder())
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    for (pixel in pixels) {
        val color = Color.red(pixel)
        val byte = color.toByte()
        mImgData.put(byte)
    }
    return mImgData
}