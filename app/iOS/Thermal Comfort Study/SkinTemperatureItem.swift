//
//  SkinTemperatureItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/14/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

class SkinTemperatureItem: BandDataItem {
    
    private static let SKIN_TEMPERATURE_NAME = "SkinTemperature"
    private var skinTemperature: Double!
    
    init(username: String, timestamp: Date, skinTemperature: Double) {
        super.init(username, timestamp)
        self.skinTemperature = skinTemperature
    }
    
    func toXML() -> String {
        return super.toXML(dataName: type(of: self).SKIN_TEMPERATURE_NAME,
                           value: self.skinTemperature)
    }
}
