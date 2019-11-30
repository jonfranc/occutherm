//
//  BottomClothing.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/20/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public enum BottomClothing {
    case SHORTS
    case LONG_PANTS_THIN
    case LONG_PANTS_THICK
    case SKIRT_THIN
    case SKIRT_THICK
    case NONE
    
    // Declaration for UIPicker
    static let all = [SHORTS, LONG_PANTS_THIN, LONG_PANTS_THICK, SKIRT_THIN,
                      SKIRT_THICK, NONE]
    
    // Return access to a tuple of values
    public func values() -> (insulation: Double, name: String, description: String) {
        switch self {
        case .SHORTS:
            return (0.08, "Shorts", "Walking shorts")
        case .LONG_PANTS_THIN:
            return (0.15, "Long pants (thin)", "Straight trousers (thin)")
        case .LONG_PANTS_THICK:
            return (0.24, "Long pants (thick)", "Straight trousers (thick)")
        case .SKIRT_THIN:
            return (0.14, "Skirt (thin)", "Skirt (thin)")
        case .SKIRT_THICK:
            return (0.23, "Skirt (thick)", "Skirt (thick)")
        case .NONE:
            return (0.00, "None", "None")
        }
    }
    
}
