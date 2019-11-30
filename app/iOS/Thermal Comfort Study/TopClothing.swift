//
//  TopClothing.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/20/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public enum TopClothing {
    case SLEEVELESS_BLOUSE
    case SHORT_SLEEVE_SHIRT
    case LONG_SLEEVE_SHIRT
    case SWEATER
    case SLEEVELESS_DRESS
    case SHORT_SLEEVE_DRESS
    case LONG_SLEEVE_DRESS_THIN
    case LONG_SLEEVE_DRESS_THICK
    
    // Declaration for UIPicker
    static let all = [SLEEVELESS_BLOUSE, SHORT_SLEEVE_SHIRT, LONG_SLEEVE_SHIRT,
                      SWEATER, SLEEVELESS_DRESS, SHORT_SLEEVE_DRESS,
                      LONG_SLEEVE_DRESS_THIN, LONG_SLEEVE_DRESS_THICK]
    
    // Return a tuple of values
    func values() -> (insulation: Double, name: String, description: String) {
        switch self {
        case .SLEEVELESS_BLOUSE:
            return (0.12, "Blouse, sleeveless", "Sleeveless, scoop-neck blouse")
        case .SHORT_SLEEVE_SHIRT:
            return (0.17, "Short-sleeve shirt/T-shirt", "Short-sleeve knit sport shirt")
        case .LONG_SLEEVE_SHIRT:
            return (0.25, "Long-sleeve shirt", "Long-sleeve dress shirt")
        case .SWEATER:
            return (0.34, "Sweater", "Long-sleeve sweatshirt")
        case .SLEEVELESS_DRESS:
            return (0.23, "Dress, sleeveless, scoop-neck", "Sleeveless, scoop neck dress (thin)")
        case .SHORT_SLEEVE_DRESS:
            return (0.29, "Dress, short-sleeve", "Short-sleeve shirtdress (thin)")
        case .LONG_SLEEVE_DRESS_THIN:
            return (0.33, "Dress, long-sleeve (thin)", "Long-sleeve shirtdress (thin)")
        case .LONG_SLEEVE_DRESS_THICK:
            return (0.47, "Dress, long-sleeve (thick)", "Long-sleeve shirtdress (thick)")
        }
    }
}
