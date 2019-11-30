//
//  SurveyDataItem.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/21/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public class SurveyDataItem: NSObject {
    
    public static let ITEM_ID_PREFIX = "_SurveyData"
    
    private var username: String!
    private var dataName: String!
    private var value: String!
    private var timestamp: String!
    
    init(_ username: String, _ timestamp: Date, _ dataName: String, _ value: String) {
        self.username = username
        self.dataName = dataName
        self.value = value
        
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
        self.timestamp = dateFormatter.string(from: timestamp)
    }
    
    private func getID() -> String {
        return SurveyDataItem.ITEM_ID_PREFIX + timestamp
    }
    
    public func toXML() -> String {
        var str = ""
        str.append("<item id=\"" + self.getID() + "\">")
        str.append("<transducerData id=\"occthermcomf_bosch\" ")
        str.append("type=\"TCS\" ")
        str.append("class=\"SurveyData\" ")
        str.append("username=\"\(username!)\" ")
        str.append("timestamp=\"\(timestamp!)\" ")
        str.append("name=\"\(dataName!)\" ")
        str.append("value=\"\(value!)\"/>")
        str.append("</item>")
        
        return str
        
    }
    
    public func set(timestamp: Date) {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
        self.timestamp = dateFormatter.string(from: timestamp)
    }
}
