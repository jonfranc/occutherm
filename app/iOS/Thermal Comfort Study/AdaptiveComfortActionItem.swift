//
//  AdaptiveComfortActionItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/19/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

class AdaptiveComfortActionItem: NSObject {
    
    private var username: String?
    private var timestamp: String?
    private var action: String?
    private var actionDescription: String?
    
    public static let ELEMENT = "item"
    public static let ITEM_ID_PREFIX = "_ActionData"
    
    // Expects nil for description if action is not "Other"
    init(username: String, timestamp: Date, action: String, description: String?) {
        self.username = username
        
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
        self.timestamp = dateFormatter.string(from: timestamp)
        
        self.action = action
        self.actionDescription = description
    }
    
    public func toXML() -> String {
        // If action equals other, set value to description
        var value = action!
        if let description = actionDescription {
            value = description
        }
        
        var str=""
        str.append("<item id=\"" + getId() + "\">")
        str.append("<transducerData id=\"occthermcomf_bosch\" ")
        str.append("type=\"TCS\" ")
        str.append("class=\"ActionData\" ")
        str.append("username=\"\(username!)\" ")
        str.append("timestamp=\"\(timestamp!)\" ")
        str.append("name=\"AdaptiveComfortAction\" ")
        str.append("value=\"\(value)\"/>")
        str.append("</" + AdaptiveComfortActionItem.ELEMENT + ">")
    
        return str
    }
    
    public func getId() -> String {
        return (AdaptiveComfortActionItem.ITEM_ID_PREFIX + timestamp!)
    }
}
