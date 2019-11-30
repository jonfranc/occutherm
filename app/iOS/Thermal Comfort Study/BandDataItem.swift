//
//  BandDataItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/14/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

class BandDataItem: NSObject {
    
    public static let ELEMENT = "item"
    public static let ITEM_ID_PREFIX = "_BandData"
    
    private var username: String!
    private var timestamp: String!
    
    init(_ username: String, _ timestamp: Date) {
        self.username = username
        
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
        self.timestamp = dateFormatter.string(from: timestamp)
    }
    
    func toXML(dataName: String, value: Double) -> String {
        var str = ""
        str.append("<" + BandDataItem.ELEMENT + " id=\"" + self.getID() + "\">")
        str.append("<transducerData id=\"occthermcomf_bosch\" ")
        str.append("type=\"TCS\" ")
        str.append("class=\"BandData\" ")
        str.append("username=\"\(username!)\" ")
        str.append("timestamp=\"\(timestamp!)\" ")
        str.append("name=\"\(dataName)\" ")
        str.append("value=\"\(value)\"/>")
        str.append("</" + BandDataItem.ELEMENT + ">")
        
        return str
    }
    
    private func getID() -> String {
        return BandDataItem.ITEM_ID_PREFIX + (self.timestamp)!
    }
}
