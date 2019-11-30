//
//  MinuteCaloriesItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/14/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

class MinuteCaloriesItem: BandDataItem {
    
    private static let MINUTE_CALORIES_NAME = "MinuteCalories"
    private var minuteCalories: Double!
    
    init(username: String, timestamp: Date, minuteCalories: Double) {
        super.init(username, timestamp)
        self.minuteCalories = minuteCalories
    }
    
    func toXML() -> String {
        return super.toXML(dataName: type(of: self).MINUTE_CALORIES_NAME,
                           value: self.minuteCalories)
    }
    
    
}
