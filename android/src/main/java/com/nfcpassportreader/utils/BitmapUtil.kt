package com.nfcpassportreader.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import com.nfcpassportreader.dto.NfcImage
import org.jmrtd.lds.AbstractImageInfo
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import jj2000.j2k.decoder.Decoder
import jj2000.j2k.util.ParameterList
import org.jnbis.WsqDecoder
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

class BitmapUtil(private val context: Context) {
  fun getImage(imageInfo: AbstractImageInfo): NfcImage {
    val image = NfcImage()
    val imageLength = imageInfo.imageLength
    val dataInputStream = DataInputStream(imageInfo.imageInputStream)
    val buffer = ByteArray(imageLength)
    try {
      dataInputStream.readFully(buffer, 0, imageLength)
      val inputStream: InputStream = ByteArrayInputStream(buffer, 0, imageLength)
      val bitmapImage = decodeImage(imageInfo.mimeType, inputStream)
      image.bitmap = bitmapImage
      val base64Image = Base64.encodeToString(buffer, Base64.DEFAULT)
      image.base64 = base64Image
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return image
  }


  @Throws(IOException::class)
  fun decodeImage(mimeType: String, inputStream: InputStream): Bitmap? {
    return if (mimeType.equals(
        "image/jp2",
        ignoreCase = true
      ) || mimeType.equals("image/jpeg2000", ignoreCase = true)
    ) {

      // Save jp2 file
      val output: OutputStream = FileOutputStream(File(context.cacheDir, "temp.jp2"))
      val buffer = ByteArray(1024)
      var read: Int
      while (inputStream.read(buffer).also { read = it } != -1) {
        output.write(buffer, 0, read)
      }
      output.close()

      // Decode jp2 file
      val pinfo: Array<Array<String?>> = Decoder.getAllParameters()
      val parameters: ParameterList
      val defaults = ParameterList()
      for (i in pinfo.indices.reversed()) {
        if (pinfo[i][3] != null) {
          defaults[pinfo[i][0]] = pinfo[i][3]
        }
      }
      parameters = ParameterList(defaults)
      parameters.setProperty("rate", "3")
      parameters.setProperty("o", context.cacheDir.toString() + "/temp.ppm")
      parameters.setProperty("debug", "on")
      parameters.setProperty("i", context.cacheDir.toString() + "/temp.jp2")
      val decoder = Decoder(parameters)
      decoder.run()

      // Read ppm file
      val reader = BufferedInputStream(
        FileInputStream(File(context.cacheDir.toString() + "/temp.ppm"))
      )
      if (reader.read() != 'P'.code || reader.read() != '6'.code) return null
      reader.read()
      val widths = StringBuilder()
      val heights = StringBuilder()
      var temp: Char
      while (reader.read().toChar().also { temp = it } != ' ') widths.append(temp)
      while (reader.read().toChar().also { temp = it } >= '0' && temp <= '9') heights.append(
        temp
      )
      if (reader.read() != '2'.code || reader.read() != '5'.code || reader.read() != '5'.code) return null
      reader.read()
      val width = widths.toString().toInt()
      val height = heights.toString().toInt()
      val colors = IntArray(width * height)
      val pixel = ByteArray(3)
      var len: Int
      var cnt = 0
      var total = 0
      val rgb = IntArray(3)
      while (reader.read(pixel).also { len = it } > 0) {
        for (i in 0 until len) {
          rgb[cnt] = if (pixel[i] >= 0) pixel[i].toInt() else pixel[i] + 255
          if (++cnt == 3) {
            cnt = 0
            colors[total++] = Color.rgb(rgb[0], rgb[1], rgb[2])
          }
        }
      }
      Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
    } else if (mimeType.equals("image/x-wsq", ignoreCase = true)) {
      val wsqDecoder = WsqDecoder()
      val bitmap: org.jnbis.Bitmap = wsqDecoder.decode(inputStream)
      val byteData: ByteArray = bitmap.pixels
      val intData = IntArray(byteData.size)
      for (j in byteData.indices) {
        intData[j] =
          -0x1000000 or (byteData[j].toInt() and 0xFF shl 16) or (byteData[j].toInt() and 0xFF shl 8) or (byteData[j].toInt() and 0xFF)
      }
      Bitmap.createBitmap(
        intData,
        0,
        bitmap.width,
        bitmap.width,
        bitmap.height,
        Bitmap.Config.ARGB_8888
      )
    } else {
      BitmapFactory.decodeStream(inputStream)
    }
  }
}

fun Bitmap.toBase64(): String {
  ByteArrayOutputStream().apply {
    compress(Bitmap.CompressFormat.JPEG, 80, this)
    return Base64.encodeToString(toByteArray(), Base64.DEFAULT)
  }
}
