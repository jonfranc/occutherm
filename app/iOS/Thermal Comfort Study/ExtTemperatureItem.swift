//
//  ExtTemperatureItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/14/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

class ExtTemperatureItem: BandDataItem {
    private static let TEMPERATURE_NAME = "Temperature"
    private var externalTemperature: Double!
    
    init(username: String, timestamp: Date, externalTemperature: Double) {
        super.init(username, timestamp)
        self.externalTemperature = externalTemperature
    }

    func toXML() -> String{
        return super.toXML(dataName: type(of: self).TEMPERATURE_NAME,
                           value: self.externalTemperature)
    }
}
