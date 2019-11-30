//
//  ClothingInsulationItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/21/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public class ClothingInsulationItem: SurveyDataItem {
    private static let CLOTHING_INSULATION_NAME = "ClothingInsulation"
    
    init(username: String, timestamp: Date, clothingInsulation: Double) {
        super.init(username, timestamp, type(of: self).CLOTHING_INSULATION_NAME, String(clothingInsulation))
    }
}
