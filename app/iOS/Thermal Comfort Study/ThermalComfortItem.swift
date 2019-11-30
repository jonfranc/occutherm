//
//  ThermalComfortItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/21/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public class ThermalComfortItem: SurveyDataItem {
    private static let THERMAL_COMFORT_NAME = "ThermalComfort"
    
    init(username: String, timestamp: Date, thermalComfort: String) {
        super.init(username, timestamp, type(of: self).THERMAL_COMFORT_NAME, thermalComfort)
    }
}
