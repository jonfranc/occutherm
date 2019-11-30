//
//  ActivityItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/21/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public class ActivityItem: SurveyDataItem {
    private static let ACTIVITY_NAME = "Activty"
    
    init(username: String, timestamp: Date, activity: String) {
        super.init(username, timestamp, type(of: self).ACTIVITY_NAME, activity)
    }
}
