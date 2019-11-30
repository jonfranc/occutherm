//
//  ActivityDescriptionItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/21/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public class ActivityDescriptionItem: SurveyDataItem {
    private static let ACTIVITY_DESCRIPTION_NAME = "ActivityDescription"
    
    init(username: String, timestamp: Date, description: String) {
        super.init(username, timestamp, type(of: self).ACTIVITY_DESCRIPTION_NAME, description)
    }
}
