//
//  TopClothingItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/21/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public class TopClothingItem: SurveyDataItem {
    private static let TOP_CLOTHING_NAME = "TopClothing"
    
    init(username: String, timestamp: Date, topClothing: String) {
        super.init(username, timestamp, type(of: self).TOP_CLOTHING_NAME, topClothing)
    }
}
