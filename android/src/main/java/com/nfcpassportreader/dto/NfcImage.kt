package com.nfcpassportreader.dto

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

data class NfcImage(
  var bitmap: Bitmap? = null,
  var base64: String? = null
) {
  companion object {
    fun bitmapToBase64(image: Bitmap?): String {
      val byteArrayOutputStream = ByteArrayOutputStream()
      image?.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
      val byteArray = byteArrayOutputStream.toByteArray()
      val base64Encoded =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
          java.util.Base64.getEncoder().encodeToString(byteArray)
        } else {
          Base64.decode(byteArray, Base64.DEFAULT)
        }
      return base64Encoded.toString()
    }

    fun base64ToBitmap(base64: String): Bitmap? {
      return try {
        val decodedString: ByteArray = Base64.decode(base64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
      } catch (e: Exception) {
        null
      }
    }
  }
}
