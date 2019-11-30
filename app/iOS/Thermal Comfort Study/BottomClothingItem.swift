//
//  BottomClothingItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/21/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public class BottomClothingItem: SurveyDataItem {
    
    private static let BOTTOM_CLOTHING_NAME = "BottomClothing"
    
    init(username: String, timestamp: Date, bottomClothing: String) {
        super.init(username, timestamp, type(of: self).BOTTOM_CLOTHING_NAME, bottomClothing)
    }
}
