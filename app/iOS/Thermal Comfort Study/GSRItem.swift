//
//  GSRItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/14/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

class GSRItem: BandDataItem {
    
    private static let GSR_NAME = "Gsr"
    private var gsr: Double!
    
    init(username: String, timestamp: Date, gsr: Double) {
        super.init(username, timestamp)
        self.gsr = gsr
    }
    
    func toXML() -> String {
        return super.toXML(dataName: type(of: self).GSR_NAME,
                           value: self.gsr)
    }
}
