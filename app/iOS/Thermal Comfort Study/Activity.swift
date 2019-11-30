//
//  Activity.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/14/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public enum Activity: String {
    case WORKING = "Working"
    case MEETING = "Meeting"
    case PRESENTING = "Presenting"
    case LUNCHTIME_ACTIVITY = "Lunchtime Activity"
    case SOCIALIZING = "Socializing"
    case RECREATION = "Recreation"
    case OTHER = "Other"
    
    // Declaration for UIPicker
    static let all = [WORKING, MEETING, PRESENTING, LUNCHTIME_ACTIVITY,
                      SOCIALIZING, RECREATION, OTHER]
}
