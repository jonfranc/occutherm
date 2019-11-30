//
//  OuterLayerClothingItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/21/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public class OuterLayerClothingItem: SurveyDataItem {
    private static let OUTER_LAYER_CLOTHING_NAME = "OuterLayerClothing"
    
    init(username: String, timestamp: Date, outerLayerClothing: String) {
        super.init(username, timestamp, type(of: self).OUTER_LAYER_CLOTHING_NAME, outerLayerClothing)
    }
}
