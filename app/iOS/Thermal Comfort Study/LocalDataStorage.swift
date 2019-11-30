//
//  LocalDataStorage.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/14/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation
import XCGLogger

class LocalDataStorage: NSObject {
    
    private let dataFilePrefix = "tcsData"
    private let dataFileExt = ".dat"
    private var dataFileString: String!
    private let utf8 = String.Encoding.utf8
    
    // Initializes local storage with file name app will write to
    override init() {
        super.init()
        
        // Create data file name
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        
        dataFileString = dataFilePrefix +
            dateFormatter.string(from: Date()) +
        dataFileExt
    }
    
    // Writes data (no end line) to file
    func write(data: String) {
        // Document is located in documents folder of app
        // Add a new line to each line of data
        let filePath = NSHomeDirectory() + "/Documents/" + dataFileString
        let encodedData = (data + "\n").data(using: utf8)
        
        // If file exists append to exisitng file
        if let fileHandle = FileHandle(forWritingAtPath: filePath) {
            fileHandle.seekToEndOfFile()
            fileHandle.write(encodedData!)
            
            log.info("Appended data to file")
        } else {
            // Else write to a new file
            do {
                try (data + "\n").write(toFile: filePath,
                                           atomically: true, encoding: utf8)
                log.info("Write data to new file")
            } catch {
                log.error("Unable to create a new file")
            }
        }
    }
}
