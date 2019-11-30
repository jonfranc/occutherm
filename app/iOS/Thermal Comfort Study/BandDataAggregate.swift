//
//  BandDataAggregate.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/14/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation
import XCGLogger

class BandDataAggregate: NSObject {
    
    // Raw sensor reading data from band
    private var rawExtTemperatureData = [Double]()
    private var rawTotalCaloriesData = [Double]()
    private var rawGSRData = [Double]()
    private var rawSkinTemperatureData = [Double]()
    private var bandHasSkinContact = false
    
    // Average of sensor reading data
    private var avgExtTemperature: Double?
    private var avgRateOfCaloriesBurned: Double?
    private var avgGSR: Double?
    private var avgSkinTemperature: Double?
    
    //MARK: Save Sensor Data
    /**
     * Put current band connection status (if user is wearing device)
     */
    func setBand(contactStatus: Bool) {
        bandHasSkinContact = contactStatus
    }
    
    /**
     * Put current raw external temperatures
     * Unit: kCals
     * Frequency: 1 Hz
     * @pre: Band has to be worn
     */
    func addExtTemperatureReading(_ temp: Double) {
        if(bandHasSkinContact) {
            rawExtTemperatureData.append(temp)
        }
    }
    
    /**
     * Put total number of calories since last factory-reset
     * Unit: kCals
     * Frequency: 1 Hz
     * @pre: Band has to be worn
     */
    func addTotalCaloriesReading(_ totalCalories: UInt) {
        if(bandHasSkinContact) {
            rawTotalCaloriesData.append(Double(totalCalories))
        }
    }
    
    /**
     * Put current raw gsr
     * Unit: kOhms
     * Frequency: 0.2 Hz
     * @pre: Band has to be worn
     */
    func addGSRReading(_ gsr: UInt) {
        if(bandHasSkinContact) {
            rawGSRData.append(Double(gsr))
        }
    }
    
    /**
     * Put current raw skin temperature
     * Unit: C
     * Frequency: as value changes
     * @pre: Band has to be worn
     */
    func addSkinTemperatureReading(_ temp: Double) {
        if(bandHasSkinContact) {
            rawSkinTemperatureData.append(temp)
        }
    }
    
    // MARK: Sensor Data Checks
    func hasRawExtTemperatureData() -> Bool {
        return !rawExtTemperatureData.isEmpty
    }
    
    func hasRawTotalCaloriesData() -> Bool {
        return rawTotalCaloriesData.count >= 2
    }
    
    func hasRawGSRData() -> Bool {
        return !rawGSRData.isEmpty
    }
    
    func hasRawSkinTemperatureData() -> Bool {
        return !rawSkinTemperatureData.isEmpty
    }
    
    func clearData() {
        if (hasRawGSRData()) {
            rawGSRData.removeAll()
        }
        
        if (hasRawExtTemperatureData()) {
            rawExtTemperatureData.removeAll()
        }
        
        if (hasRawSkinTemperatureData()) {
            rawSkinTemperatureData.removeAll()
        }
        
        if (hasRawTotalCaloriesData()){
            rawTotalCaloriesData.removeAll()
        }
    }
    
    // MARK: Average Sensor Data
    func averageSensorData() {
        averageExtTemperature()
        averageRateOfCaloriesBurned()
        averageGSR()
        averageSkinTemperature()
    }
    
    private func averageExtTemperature() {
        if(self.hasRawExtTemperatureData()) {
            avgExtTemperature = average(rawExtTemperatureData)
            rawExtTemperatureData.removeAll()
        } else {
            avgExtTemperature = nil
            log.warning("No barometer data has been received")
        }
    }
    
    private func averageRateOfCaloriesBurned() {
        if(hasRawTotalCaloriesData()) {
            var rateOfCaloriesData = [Double]()
            
            // Create a rate of calories burned array from diff each reading
            for i in 0..<(rawTotalCaloriesData.count - 1) {
                let curr = rawTotalCaloriesData[i]
                let next = rawTotalCaloriesData[i + 1]
                rateOfCaloriesData.append(next - curr)
            }
            
            avgRateOfCaloriesBurned = average(rateOfCaloriesData)
            rawTotalCaloriesData.removeAll()
        } else {
            avgRateOfCaloriesBurned = nil
            log.warning("No calorie data has been received")
        }
    }
    
    private func averageGSR() {
        if(self.hasRawGSRData()) {
            avgGSR = average(rawGSRData)
            rawGSRData.removeAll()
        } else {
            avgGSR = nil
            log.warning("No GSR data has been received")
        }
    }
    
    private func averageSkinTemperature() {
        if(self.hasRawSkinTemperatureData()) {
            avgSkinTemperature = average(rawSkinTemperatureData)
            rawSkinTemperatureData.removeAll()
        } else {
            avgSkinTemperature = nil
            log.warning("No skin temperature data has been received")
        }
    }
    
    // MARK: Return Average Sensor Data
    func getAverageExtTemperature() -> Double? {
        return avgExtTemperature
    }
    
    func getAverageCalories() -> Double? {
        return avgRateOfCaloriesBurned
    }
    
    func getAverageGSR() -> Double? {
        return avgGSR
    }
    
    func getAverageSkinTemperature() -> Double? {
        return avgSkinTemperature
    }
    
    // MARK: Averaging Helper Functions
    private func average(_ sensorData: [Double]) -> Double {
        var sum = 0.0
        for data in sensorData {
            sum += data
        }
        return sum / Double(sensorData.count)
    }
}
