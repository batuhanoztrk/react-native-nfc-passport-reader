import CoreNFC
import Foundation
import OpenSSL
import React
import UIKit

@objc(NfcPassportReader)
class NfcPassportReader: NSObject {
  private let passportReader = PassportReader()
  private let passportUtil = PassportUtil()

  @objc
  static func requiresMainQueueSetup() -> Bool {
    return true
  }

  @objc func isNfcSupported(
    _ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    if #available(iOS 13.0, *) {
      resolve(NFCNDEFReaderSession.readingAvailable)
    } else {
      resolve(false)
    }
  }

  @objc func startReading(
    _ options: NSDictionary, resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    let bacKey = options["bacKey"] as? NSDictionary
    let includeImages = options["includeImages"] as? Bool

    let documentNo = bacKey?["documentNo"] as? String
    let expiryDate = bacKey?["expiryDate"] as? String
    let birthDate = bacKey?["birthDate"] as? String

    if let documentNo = documentNo, let expiryDate = expiryDate, let birthDate = birthDate {
      if let birthDateFormatted = birthDate.convertToYYMMDD() {
        passportUtil.dateOfBirth = birthDateFormatted
      } else {
        reject("ERROR_INVALID_BIRTH_DATE", "Invalid birth date", nil)
      }

      if let expiryDateFormatted = expiryDate.convertToYYMMDD() {
        passportUtil.expiryDate = expiryDateFormatted
      } else {
        reject("ERROR_INVALID_EXPIRY_DATE", "Invalid expiry date", nil)
      }

      passportUtil.passportNumber = documentNo

      let mrzKey = passportUtil.getMRZKey()

      var tags: [DataGroupId] = [.COM, .DG1, .DG11]

      if includeImages ?? false {
        tags.append(.DG2)
      }

      let finalTags = tags // Create immutable copy

      let customMessageHandler: (NFCViewDisplayMessage) -> String? = { displayMessage in
        switch displayMessage {
        case .requestPresentPassport:
          return "Hold your iPhone near an NFC-enabled ID Card / Passport."
        case .successfulRead:
          return "ID Card / Passport Successfully Read."
        case .readingDataGroupProgress(let dataGroup, let progress):
          let progressString = self.handleProgress(percentualProgress: progress)
          let readingDataString = "Read Data"
          return "\(readingDataString) \(dataGroup) ...\n\(progressString)"
        case .error(let error):
          return error.errorDescription
        default:
          return nil
        }
      }

      Task {
        do {
          let passport = try await self.passportReader.readPassport(
            mrzKey: mrzKey, tags: finalTags, customDisplayMessage: customMessageHandler)
          print("passport: \(passport)")

          let result: NSMutableDictionary = [
            "birthDate": passport.dateOfBirth.convertToYYYYMMDD(),
            "placeOfBirth": passport.placeOfBirth,
            "documentNo": passport.documentNumber,
            "expiryDate": passport.documentExpiryDate.convertToYYYYMMDD(),
            "firstName": passport.firstName,
            "gender": passport.gender,
            "identityNo": passport.personalNumber,
            "lastName": passport.lastName,
            "mrz": passport.passportMRZ,
            "nationality": passport.nationality,
          ]

          if includeImages ?? false {
            if let passportImage = passport.passportImage,
               let imageData = passportImage.jpegData(compressionQuality: 0.8)
            {
              result["photo"] = imageData.base64EncodedString()
            }
          }

          resolve(result)
        } catch {
          reject("ERROR_READ_PASSPORT", "Error reading passport", nil)
        }
      }
    } else {
      reject("ERROR_INVALID_BACK_KEY", "Invalid bac key", nil)
    }
  }

  func handleProgress(percentualProgress: Int) -> String {
    let barWidth = 10
    let completedWidth = Int(Double(barWidth) * Double(percentualProgress) / 100.0)
    let remainingWidth = barWidth - completedWidth

    let completedBar = String(repeating: "🔵", count: completedWidth)
    let remainingBar = String(repeating: "⚪️", count: remainingWidth)

    return "[\(completedBar)\(remainingBar)] \(percentualProgress)%"
  }
}
