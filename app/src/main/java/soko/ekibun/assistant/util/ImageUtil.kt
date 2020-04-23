package soko.ekibun.assistant.util

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream


object ImageUtil {
    private fun getCachePath(context: Context): File {
        return File(context.cacheDir, "images")
    }

    private fun deleteAllFiles(root: File) {
        val files = root.listFiles()
        if (files != null) for (f in files) {
            if (f.isDirectory) { // 判断是否为文件夹
                deleteAllFiles(f)
                try {
                    f.delete()
                } catch (e: java.lang.Exception) {
                }
            } else {
                if (f.exists()) { // 判断是否存在
                    deleteAllFiles(f)
                    try {
                        f.delete()
                    } catch (e: java.lang.Exception) {
                    }
                }
            }
        }
    }

    fun saveToCache(context: Context, bitmap: Bitmap): File {
        val cachePath = getCachePath(context)
        cachePath.mkdirs() // don't forget to make the directory
        deleteAllFiles(cachePath)
        val f = File(cachePath, "image_${System.currentTimeMillis()}")
        try {
            val stream = FileOutputStream(f, false) // overwrites this image every time
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return f
    }


    fun getYUV420sp(scaled: Bitmap): ByteArray {
        val inputWidth = scaled.width
        val inputHeight = scaled.height
        val argb = IntArray(inputWidth * inputHeight)
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        val yuv = ByteArray(inputWidth * inputHeight * 3 / 2)
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight)
        return yuv
    }

    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var y: Int
        var u: Int
        var v: Int
        var yIndex = 0
        var uvIndex = frameSize

        var r: Int
        var g: Int
        var b: Int
        var argbIndex = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                // a is not used obviously
                //a = (argb[argbIndex] & 0xff000000) >> 24;
                r = argb[argbIndex] and 0xff0000 shr 16
                g = argb[argbIndex] and 0xff00 shr 8
                b = argb[argbIndex] and 0xff
                argbIndex++
                // well known RGB to YUV algorithm
                y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
                y = Math.max(0, Math.min(y, 255))
                u = Math.max(0, Math.min(u, 255))
                v = Math.max(0, Math.min(v, 255))
                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                // meaning for every 4 Y pixels there are 1 V and 1 U. Note the sampling is every other
                // pixel AND every other scanline.
                // ---Y---
                yuv420sp[yIndex++] = y.toByte()
                // ---UV---
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv420sp[uvIndex++] = v.toByte()
                    yuv420sp[uvIndex++] = u.toByte()
                }
            }
        }
    }
}