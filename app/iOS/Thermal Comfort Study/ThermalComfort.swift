//
//  ThermalComfort.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/14/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public enum ThermalComfort: String {
    case UNCOMFORTABLY_COLD = "Uncomfortably Cold"
    case SLIGHTLY_UNCOMFORTABLY_COLD = "Slightly Uncomfortably Cold"
    case COMFORTABLE = "Comfortable"
    case SLIGHTLY_UNCOMFORTABLY_WARM = "Slightly Uncomfortably Warm"
    case UNCOMFORTABLY_WARM = "Uncomfortably Warm"
    
    // Declaration for UIPicker
    static let all = [UNCOMFORTABLY_COLD, SLIGHTLY_UNCOMFORTABLY_COLD,
                      COMFORTABLE, SLIGHTLY_UNCOMFORTABLY_WARM,
                      UNCOMFORTABLY_WARM]
}
