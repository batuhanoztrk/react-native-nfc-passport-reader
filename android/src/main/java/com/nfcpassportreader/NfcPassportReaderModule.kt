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
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
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

class NfcPassportReaderModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), LifecycleEventListener, ActivityEventListener {

  private val nfcPassportReader = NfcPassportReader(reactContext)
  private var adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(reactContext)
  private var mrzInfo: MRZInfo? = null
  private var isReading = false
  private val jsonToReactMap = JsonToReactMap()

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
    if (!isReading) return

    try {
      adapter?.let {
        currentActivity?.let { activity ->
          val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent
              .getActivity(
                activity, 0,
                Intent(activity, activity.javaClass)
                  .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE
              )
          } else {
            PendingIntent
              .getActivity(
                activity, 0,
                Intent(activity, activity.javaClass)
                  .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
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

          sendErrorEvent(Exception("Current activity is null"))
        }

      } ?: run {
        sendErrorEvent(Exception("NfcAdapter is null"))
      }
    } catch (e: Exception) {
      sendErrorEvent(e)
    }
  }

  override fun onHostPause() {
//    TODO("Not yet implemented")
  }

  override fun onActivityResult(p0: Activity?, p1: Int, p2: Int, p3: Intent?) {
//    TODO("Not yet implemented")
  }

  override fun onNewIntent(p0: Intent?) {
    p0?.let { intent ->
      if (!isReading) return

      sendEvent("onTagDiscovered", null)

      if (mrzInfo?.documentNumber.isNullOrEmpty() || mrzInfo?.dateOfExpiry.isNullOrEmpty() || mrzInfo?.dateOfBirth.isNullOrEmpty()) {
        sendErrorEvent(Exception("MRZ info is not set"))
        return
      }

      if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
        val tag = intent.extras!!.getParcelable<Tag>(NfcAdapter.EXTRA_TAG)

        if (listOf(*tag!!.techList).contains("android.nfc.tech.IsoDep")) {
          CoroutineScope(Dispatchers.IO).launch {
            try {
              val bacKey: BACKeySpec = BACKey(
                mrzInfo!!.documentNumber,
                mrzInfo!!.dateOfBirth,
                mrzInfo!!.dateOfExpiry
              )

              val result = nfcPassportReader.readPassport(IsoDep.get(tag), bacKey)

              val map = result.serializeToMap()
              val reactMap = jsonToReactMap.convertJsonToMap(JSONObject(map))

              sendEvent("onNfcResult", reactMap)
            } catch (e: Exception) {
              sendErrorEvent(e)
            }
          }
        } else {
          sendErrorEvent(Exception("IsoDep is not supported"))
        }
      }
    }
  }

  override fun onHostDestroy() {
    reactApplicationContext.removeLifecycleEventListener(this)
    reactApplicationContext.removeActivityEventListener(this)
  }

  private fun sendEvent(eventName: String, params: Any?) {
    reactApplicationContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  private fun sendErrorEvent(exception: Exception) {
    sendEvent("onNfcError", exception.message ?: "Unknown Error")
  }

  @ReactMethod
  fun startReading(mrzString: String) {
    mrzInfo = MRZInfo(mrzString)
    isReading = true
  }

  @ReactMethod
  fun stopReading() {
    isReading = false
    mrzInfo = null
    adapter?.disableForegroundDispatch(currentActivity)
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
