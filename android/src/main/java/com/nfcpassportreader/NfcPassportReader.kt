package com.nfcpassportreader

import android.content.Context
import android.nfc.tech.IsoDep
import com.nfcpassportreader.utils.*
import com.nfcpassportreader.dto.*
import net.sf.scuba.smartcards.CardService
import org.jmrtd.BACKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.CardSecurityFile
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.icao.DG11File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.lds.iso19794.FaceImageInfo

class NfcPassportReader(context: Context) {
  private val bitmapUtil = BitmapUtil(context)
  private val dateUtil = DateUtil()

  fun readPassport(isoDep: IsoDep, bacKey: BACKeySpec, includeImages: Boolean): NfcResult {
    isoDep.timeout = 10000

    val cardService = CardService.getInstance(isoDep)
    cardService.open()

    val service = PassportService(
      cardService,
      PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
      PassportService.DEFAULT_MAX_BLOCKSIZE,
      false,
      false
    )
    service.open()

    var paceSucceeded = false
    try {
      val cardSecurityFile =
        CardSecurityFile(service.getInputStream(PassportService.EF_CARD_SECURITY))
      val securityInfoCollection = cardSecurityFile.securityInfos

      for (securityInfo in securityInfoCollection) {
        if (securityInfo is PACEInfo) {
          service.doPACE(
            bacKey,
            securityInfo.objectIdentifier,
            PACEInfo.toParameterSpec(securityInfo.parameterId),
            null
          )
          paceSucceeded = true
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    service.sendSelectApplet(paceSucceeded)

    if (!paceSucceeded) {
      try {
        service.getInputStream(PassportService.EF_COM).read()
      } catch (e: Exception) {
        e.printStackTrace()

        service.doBAC(bacKey)
      }
    }

    val nfcResult = NfcResult()

    val dg1In = service.getInputStream(PassportService.EF_DG1)
    val dg1File = DG1File(dg1In)
    val mrzInfo = dg1File.mrzInfo

    val dg11In = service.getInputStream(PassportService.EF_DG11)
    val dg11File = DG11File(dg11In)

    val name = dg11File.nameOfHolder.substringAfterLast("<<").replace("<", " ")
    val surname = dg11File.nameOfHolder.substringBeforeLast("<<")

    nfcResult.firstName = name
    nfcResult.lastName = surname

    nfcResult.placeOfBirth = dg11File.placeOfBirth.joinToString(separator = " ")

    nfcResult.identityNo = mrzInfo.personalNumber
    nfcResult.gender = mrzInfo.gender.toString()

    nfcResult.birthDate = dateUtil.convertFromNfcDate(dg11File.fullDateOfBirth)
    nfcResult.expiryDate = dateUtil.convertFromMrzDate(mrzInfo.dateOfExpiry)

    nfcResult.documentNo = mrzInfo.documentNumber
    nfcResult.nationality = mrzInfo.nationality
    nfcResult.mrz = mrzInfo.toString()

    if (includeImages) {
      val dg2In = service.getInputStream(PassportService.EF_DG2)
      val dg2File = DG2File(dg2In)
      val faceInfos = dg2File.faceInfos
      val allFaceImageInfos: MutableList<FaceImageInfo> = ArrayList()
      for (faceInfo in faceInfos) {
        allFaceImageInfos.addAll(faceInfo.faceImageInfos)
      }
      if (allFaceImageInfos.isNotEmpty()) {
        val faceImageInfo = allFaceImageInfos.iterator().next()
        val image = bitmapUtil.getImage(faceImageInfo)
        nfcResult.originalFacePhoto = image
      }
    }

    if (dg11File.length > 0) return nfcResult
    else throw Exception("DG11 file is empty")
  }
}
