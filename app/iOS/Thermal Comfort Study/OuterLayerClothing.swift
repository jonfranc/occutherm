//
//  OuterLayerClothing.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/20/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public enum OuterLayerClothing {
    case VEST_THIN
    case VEST_THICK
    case SWEATER_JACKET_THIN
    case SWEATER_JACKET_THICK
    case COAT_SUIT_JACKET_THICK
    case NONE
    
    // Declaration for UIPickeer
    static let all = [VEST_THIN, VEST_THICK, SWEATER_JACKET_THIN,
                      SWEATER_JACKET_THICK, COAT_SUIT_JACKET_THICK, NONE]
    
    // Return access to a tuple of values
    public func values() -> (insulation: Double, name: String, description: String) {
        switch self {
        case .VEST_THIN:
            return (0.13, "Vest (thin)", "Sleeveless vest (thin)")
        case .VEST_THICK:
            return (0.22, "Vest (thick)", "Sleeveless vest (thick)")
        case .SWEATER_JACKET_THIN:
            return (0.25, "Sweater/Jacket (thin)", "Long-sleeve sweater (thin)")
        case .SWEATER_JACKET_THICK:
            return (0.36, "Sweater/Jacket (thick)", "Long-sleeve sweater (thick)")
        case .COAT_SUIT_JACKET_THICK:
            return (0.44, "Coat/Suit jacket (thick)", "Single-breasted suit jacket (thick)")
        case .NONE:
            return (0.00, "None", "None")
        }
    }
}
