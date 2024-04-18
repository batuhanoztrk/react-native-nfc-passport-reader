package com.nfcpassportreader.utils

import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.nfcpassportreader.dto.NfcImage
import java.lang.reflect.Type
import java.time.LocalDateTime

fun <T> T.serializeToMap(): Map<String, Any> {
  return convert()
}

@SuppressLint("NewApi")
inline fun <I, reified O> I.convert(): O {
  val gson = GsonBuilder()
    .registerTypeHierarchyAdapter(Bitmap::class.java, BitmapSerializer())
    .registerTypeHierarchyAdapter(
      LocalDateTime::class.java,
      LocalDateTimeSerializer()
    )
    .registerTypeAdapter(NfcImage::class.java, NfcImageSerializer())
    .create()
  val json = gson.toJson(this)
  return gson.fromJson(json, object : TypeToken<O>() {}.type)
}

class LocalDateTimeSerializer : JsonSerializer<LocalDateTime> {
  override fun serialize(
    src: LocalDateTime?,
    typeOfSrc: Type?,
    context: JsonSerializationContext?
  ): JsonElement {
    if (src == null) return JsonNull.INSTANCE

    return JsonPrimitive(src.toString())
  }
}

class BitmapSerializer : JsonSerializer<Bitmap> {
  override fun serialize(
    src: Bitmap?,
    typeOfSrc: Type?,
    context: JsonSerializationContext?
  ): JsonElement {
    if (src == null) return JsonNull.INSTANCE

    return JsonPrimitive(src.toBase64())
  }
}

class NfcImageSerializer : JsonSerializer<NfcImage> {
  override fun serialize(
    src: NfcImage?,
    typeOfSrc: Type?,
    context: JsonSerializationContext?
  ): JsonElement {
    if (src == null) return JsonNull.INSTANCE

    return JsonPrimitive(src.base64)
  }
}
