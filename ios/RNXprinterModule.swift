//
//  RNXprinterModule.swift
//  RNXprinterModule
//
//  Copyright © 2022 Tam Nguyen. All rights reserved.
//

import Foundation

@objc(RNXprinterModule)
class RNXprinterModule: NSObject {
  @objc
  func constantsToExport() -> [AnyHashable : Any]! {
    return ["count": 1]
  }

  @objc
  static func requiresMainQueueSetup() -> Bool {
    return true
  }
}
