package com.nfcpassportreader

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.nfcpassportreader.utils.JsonToReactMap
import com.nfcpassportreader.utils.serializeToMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jmrtd.BACKey
import org.jmrtd.BACKeySpec
import org.jmrtd.lds.icao.MRZInfo
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class NfcPassportReaderModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), LifecycleEventListener, ActivityEventListener {

  private val nfcPassportReader = NfcPassportReader(reactContext)
  private var adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(reactContext)
  private var bacKey: BACKeySpec? = null
  private var includeImages = false
  private var isReading = false
  private val jsonToReactMap = JsonToReactMap()
  private var _promise: Promise? = null
  private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  private val outputDateFormat = SimpleDateFormat("yyMMdd", Locale.getDefault())

  init {
    reactApplicationContext.addLifecycleEventListener(this)
    reactApplicationContext.addActivityEventListener(this)

    val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
    reactApplicationContext.registerReceiver(NfcStatusReceiver(), filter)
  }

  inner class NfcStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED == intent?.action) {
        val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
        when (state) {
          NfcAdapter.STATE_OFF -> {
            sendEvent("onNfcStateChanged", "off")
          }

          NfcAdapter.STATE_ON -> {
            sendEvent("onNfcStateChanged", "on")
          }

          NfcAdapter.STATE_TURNING_OFF -> {
            // NFC kapanıyor
          }

          NfcAdapter.STATE_TURNING_ON -> {
            // NFC açılıyor
          }
        }
      }
    }
  }

  override fun getName(): String {
    return NAME
  }

  override fun onHostResume() {
    try {
      adapter?.let {
        currentActivity?.let { activity ->
          val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
          }

          val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent
              .getActivity(
                activity, 0,
                intent,
                PendingIntent.FLAG_MUTABLE
              )
          } else {
            PendingIntent
              .getActivity(
                activity, 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
              )
          }

          val filter = arrayOf(arrayOf("android.nfc.tech.IsoDep"))

          it.enableForegroundDispatch(
            activity,
            pendingIntent,
            null,
            filter
          )
        } ?: run {
          Log.e("NfcPassportReader", "CurrentActivity is null")
        }
      } ?: run {
        Log.e("NfcPassportReader", "NfcAdapter is null")
      }
    } catch (e: Exception) {
      Log.e("NfcPassportReader", e.message ?: "Unknown Error")
    }
  }

  override fun onHostPause() {
  }

  override fun onHostDestroy() {
    adapter?.disableForegroundDispatch(currentActivity)
  }

  override fun onActivityResult(p0: Activity?, p1: Int, p2: Int, p3: Intent?) {
  }

  override fun onNewIntent(p0: Intent?) {
    p0?.let { intent ->
      if (!isReading) return

      sendEvent("onTagDiscovered", null)

      if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
        val tag = intent.extras!!.getParcelable<Tag>(NfcAdapter.EXTRA_TAG)

        if (listOf(*tag!!.techList).contains("android.nfc.tech.IsoDep")) {
          CoroutineScope(Dispatchers.IO).launch {
            try {
              val result = nfcPassportReader.readPassport(IsoDep.get(tag), bacKey!!, includeImages)

              val map = result.serializeToMap()
              val reactMap = jsonToReactMap.convertJsonToMap(JSONObject(map))

              _promise?.resolve(reactMap)
            } catch (e: Exception) {
              reject(e)
            }
          }
        } else {
          reject(Exception("Tag tech is not IsoDep"))
        }
      }
    }
  }

  private fun sendEvent(eventName: String, params: Any?) {
    reactApplicationContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  private fun reject(e: Exception) {
    isReading = false
    bacKey = null
    _promise?.reject(e)
  }

  @ReactMethod
  fun startReading(readableMap: ReadableMap?, promise: Promise) {
    readableMap?.let {
      _promise = promise
      val bacKey = readableMap.getMap("bacKey")

      includeImages =
        readableMap.hasKey("includeImages") && readableMap.getBoolean("includeImages")

      bacKey?.let {
        val documentNo = it.getString("documentNo")
        val expiryDate = it.getString("expiryDate")?.let { date ->
          try {
            outputDateFormat.format(inputDateFormat.parse(date)!!)
          } catch (e: Exception) {
            null
          }
        }
        val birthDate = it.getString("birthDate")?.let { date ->
          try {
            outputDateFormat.format(inputDateFormat.parse(date)!!)
          } catch (e: Exception) {
            null
          }
        }

        if (documentNo == null || expiryDate == null || birthDate == null) {
          reject(Exception("BAC key is not valid"))
          return
        }

        this.bacKey = BACKey(
          documentNo, birthDate, expiryDate
        )

        isReading = true
      } ?: run {
        reject(Exception("BAC key is null"))
      }
    } ?: run {
      reject(Exception("ReadableMap is null"))
    }
  }

  @ReactMethod
  fun stopReading() {
    isReading = false
    bacKey = null
  }

  @ReactMethod
  fun isNfcEnabled(promise: Promise) {
    promise.resolve(NfcAdapter.getDefaultAdapter(reactApplicationContext)?.isEnabled ?: false)
  }

  @ReactMethod
  fun isNfcSupported(promise: Promise) {
    promise.resolve(reactApplicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC))
  }

  @SuppressLint("QueryPermissionsNeeded")
  @ReactMethod
  fun openNfcSettings(promise: Promise) {
    val intent = Intent(Settings.ACTION_NFC_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(reactApplicationContext.packageManager) != null) {
      reactApplicationContext.startActivity(intent)
      promise.resolve(true)
    } else {
      promise.reject(Exception("Activity not found"))
    }
  }

  companion object {
    const val NAME = "NfcPassportReader"
  }
}
