//
//  AdaptiveComfortAction.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/14/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public enum AdaptiveComfortAction: String {
    case THERMOSTAT_WARMER = "Set thermostat cooler"
    case THERMOSTAT_COOLER = "Set thermostat warmer"
    case SPACE_HEATER_ON = "Turn space heater on"
    case SPACE_HEATER_OFF = "Turn space heater off"
    case FAN_ON = "Turn fan on"
    case FAN_OFF = "Turn fan off"
    case LAYER_ON = "Add sweater/coat etc."
    case LAYER_OFF = "Take off sweater/coat etc."
    case OTHER = "Other (please describe):"
    
    // Declaration for UIPicker
    static let all = [THERMOSTAT_WARMER, THERMOSTAT_COOLER, SPACE_HEATER_ON,
                      SPACE_HEATER_OFF, FAN_ON, FAN_OFF, LAYER_ON, LAYER_OFF,
                      OTHER]
}
