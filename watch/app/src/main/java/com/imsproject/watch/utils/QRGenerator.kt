package com.imsproject.watch.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.imsproject.watch.SCREEN_RADIUS
import java.util.EnumMap

object QRGenerator {
    private const val bgColor = -0x1
    private const val fgColor = -0x1000000

    fun generate(text: String): ImageBitmap {
        return encodeBitmap(text, SCREEN_RADIUS.toInt(), SCREEN_RADIUS.toInt())
    }

    @Throws(WriterException::class)
    private fun encodeBitmap(contents: String?, width: Int, height: Int): ImageBitmap {
        return createBitmap(encode(contents, width, height))
    }

    @Throws(WriterException::class)
    private fun encode(contents: String?, width: Int, height: Int): BitMatrix {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.MARGIN] = 0 // No white border
        try {
            return QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, width, height,hints)
        } catch (e: WriterException) {
            throw e
        } catch (e: Exception) {
            // ZXing sometimes throws an IllegalArgumentException
            throw WriterException(e)
        }
    }

    private fun createBitmap(matrix: BitMatrix): ImageBitmap {
        val width: Int = matrix.width
        val height: Int = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0..<height) {
            val offset = y * width
            for (x in 0..<width) {
                pixels[offset + x] = if (matrix.get(x, y)) fgColor else bgColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap.asImageBitmap()
    }
}
