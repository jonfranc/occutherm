//
//  MSBandService.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/14/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation
import Dispatch
import UserNotifications
import CoreData
import XMPPFramework

class MSBandService: NSObject, MSBClientManagerDelegate {
    
    private var clientManager = MSBClientManager.shared()
    private var client: MSBClient?
    
    private var samplingStartTime = Date.distantPast
    public var lastSampleTime =  Date.distantPast
    public var lastResponseTime =  Date.distantPast
    public var lastNotificationTime = Date.distantPast
    public var lastPublishTime = Date.distantPast
    public var oneOffSample = false
    private var isSampling = false
    
    // Seconds
    private var BAND_DATA_SAMPLING_INTERVAL = 60.0
    private var BAND_DATA_RECORD_STREAMING_SESSION = 600.0
    private var BAND_DATA_RECORD_STREAMING_INTERVAL = 1200.0
    private var MINIMUM_TIME_BETWEEN_SURVEY_RESPONSE = 1800.0
    private var MINIMUM_TIME_BETWEEN_NOTIFICATION = 3000.0
    private var SURVEY_NOTIFICATION_DELAY = 600.0
    private var PUBLISH_DATA_INTERVAL = 600.0
    
    public var notificationCenter: UNUserNotificationCenter!
    private var notificationRequest: UNNotificationRequest!
    
    private var baseXMPPMessage: XMPPElement!
    private var nodeName = "occthermcomf_bosch"
    
    private var usernameKey = "username"
    private var username: String!
    
    public enum ConnectionState: String {
        case notConnected = "Connection Status: Not Connected"
        case connecting = "Connection Status: Connecting"
        case connected = "Connection Status: Connected"
    }
    
    public var connectionStatus = ConnectionState.notConnected
    
    private var bandDataAggregate = BandDataAggregate()
    private var mainViewController: UIViewController!
    private var updateConnectionLabel: (() -> Void)! = nil
    
    /* Serial dispatch queue to handle bandcallbacks. This queue is
     * is used to make sure that stop streaming, and thus aggregate
     * band data is called only once. This is to handle the case where
     * stopStreaming could be called by GSRUpdate function, but before
     * stopStreaming call finishes, GSRUpdate hanlder is called a few
     * more teams, each calling stopStreaming again.
     */
    private let dispatchQueue = DispatchQueue(label: "com.msband.msbandservice")
    
    override init() {
        super.init()
        
        // Create survey notification
        let content = UNMutableNotificationContent()
        content.title = "Survey Notification"
        content.body = "Please submit a survey at this time"
        content.sound = UNNotificationSound.default()
        
        // Set trigger
        let trigger = UNTimeIntervalNotificationTrigger.init(timeInterval: SURVEY_NOTIFICATION_DELAY, repeats: false)
        
        notificationRequest = UNNotificationRequest.init(identifier: "SurveyNotification", content: content, trigger: trigger)
        notificationCenter = UNUserNotificationCenter.current()
        
        // Create barebones of XMPP message
        createBaseXMPPMessage()
    }
    
    func connect(vc: UIViewController, closure: @escaping () -> Void) {
        mainViewController = vc
        self.updateConnectionLabel = closure
        
        connectionStatus = .connecting
        self.updateConnectionLabel()
        
        // Setup Microsoft Band
        // 1. Set up client manager and its delegate
        clientManager?.delegate = self
        
        // 2. Get a list of attached clients and get first client
        if let band = clientManager?.attachedClients().first as! MSBClient? {
            self.client = band
            
            mainViewController.view.makeToast("Please wait... connecting to band")
            
            // 3. Connect to client
            clientManager?.connect(client)
            log.verbose("Please wait! ... connecting to MBand " +
                "<\((self.client!.name)!)>")
        } else {
            
            // TODO: Send a notification asking user to wear band
            connectionStatus = .notConnected
            self.self.updateConnectionLabel()
            mainViewController.view.makeToast("Failed to connect to band")
            log.error("Client manager has failed to find a band")
            return
        }
    }
    
    func startStreaming() {
        // Check if client exists, then register
        if let client = self.client {
            isSampling = true
            registerForContactUpdates(from: client)
            registerForBaromaterUpdates(from: client)
            registerForCalorieUpdates(from: client)
            registerForSkinTemperatureUpdates(from: client)
            registerForGSRUpdates(from: client)
            registerForAmbientLightUpdates(from: client)
        } else {
            isSampling = false
            connectionStatus = .notConnected
            self.updateConnectionLabel()
            mainViewController.view.makeToast("Failed to connect to band")
            log.error("Client not connected, please try again")
        }
    }
    
    func stopStreaming() {
        if let client = self.client {
            do {
                try client.sensorManager.stopCaloriesUpdatesErrorRef()
                try client.sensorManager.stopGSRUpdatesErrorRef()
                try client.sensorManager.stopBarometerUpdatesErrorRef()
                try client.sensorManager.stopSkinTempUpdatesErrorRef()
                try client.sensorManager.stopBandContactUpdatesErrorRef()
                try client.sensorManager.stopAmbientLightUpdatesErrorRef()
            } catch {
                log.error("Failed to communicate with band")
            }
            log.info("Band has stopped streaming successfully")
        }
        
        // Average and write data to file
        bandDataAggregate.averageSensorData()
        writeBandDataToFile()
        connectionStatus = .notConnected
        
        // Publish any remaining data
        publishData()
        
        if self.updateConnectionLabel != nil {
            self.updateConnectionLabel()
        }
    }
    
    func sampleBandData() {
        // Average data and write data to file
        bandDataAggregate.averageSensorData()
        writeBandDataToFile()
    }
    
    func writeBandDataToFile() {
        let localDataStorage = LocalDataStorage()
        let storageDate = Date()
        
        var lastAvgExtTemp: ExtTemperatureItem!
        var lastAvgMinuteCalories: MinuteCaloriesItem!
        var lastAvgGSR: GSRItem!
        var lastAvgSkinTemp: SkinTemperatureItem!
        
        self.username = getUsername()
        
        // If ext temp data exists, average and write to file
        if let avg = bandDataAggregate.getAverageExtTemperature() {
            lastAvgExtTemp = ExtTemperatureItem(username: self.username,
                timestamp: storageDate, externalTemperature: avg)
            saveXMLToCoreData(lastAvgExtTemp.toXML())
            localDataStorage.write(data: lastAvgExtTemp.toXML())
        }
        
        // If calories data exists, get average / min and write to file
        if let avg = bandDataAggregate.getAverageCalories() {
            lastAvgMinuteCalories = MinuteCaloriesItem(
                username: self.username, timestamp: storageDate,
                minuteCalories: avg * BAND_DATA_SAMPLING_INTERVAL)
            saveXMLToCoreData(lastAvgMinuteCalories.toXML())
            localDataStorage.write(data: lastAvgMinuteCalories.toXML())
        }
        
        // If GSR data exists, average and write to file
        if let avg = bandDataAggregate.getAverageGSR() {
            lastAvgGSR = GSRItem(username: self.username,
                                 timestamp: storageDate, gsr: avg)
            saveXMLToCoreData(lastAvgGSR.toXML())
            localDataStorage.write(data: lastAvgGSR.toXML())
        }
        
        // If skin temp data exists, average and write to file
        if let avg = bandDataAggregate.getAverageSkinTemperature() {
            lastAvgSkinTemp = SkinTemperatureItem(username: self.username,
                timestamp: storageDate, skinTemperature: avg)
            saveXMLToCoreData(lastAvgSkinTemp.toXML())
            localDataStorage.write(data: lastAvgSkinTemp.toXML())
        }
    }
    
    func saveXMLToCoreData(_ xmlstring: String) {
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else {
            return
        }
        
        // 1 - Get the managed context from the appDelegate
        let managedContext = appDelegate.persistentContainer.viewContext
        
        // 2 - Create an entity (payload) based on the data model
        let entity = NSEntityDescription.entity(forEntityName: "Message", in: managedContext)
        let message = NSManagedObject(entity: entity!, insertInto: managedContext)
        
        // 3 - Set the attribute payload to the xml string
        message.setValue(xmlstring, forKeyPath: "payload")
        
        // 4 - Save the entity into iOS core data
        do {
            try managedContext.save()
        } catch let error as NSError {
            log.error("Could not save. \(error), \(error.userInfo)")
        }
    }
    
    // MARK: Sensor Registeration Helper Functions
    func registerForBaromaterUpdates(from client: MSBClient) {
        do {
            try client.sensorManager.startBarometerUpdates(to: nil, withHandler: {
                (barometerData: MSBSensorBarometerData?, error: Error?) in
                
                if let temp = barometerData?.temperature {
                    log.verbose("External temperature reading: \(temp)")
                    
                    if (self.isSampling) {
                        self.bandDataAggregate.addExtTemperatureReading(temp)
                    }
                    
                    // Update reading for fine grain data collection
                    fineGrainData.ambientTemperatureString = String(format: "%2.1f", temp)
                }
                
                if let pressure = barometerData?.airPressure {
                    log.verbose("Air Pressure reading: \(pressure)")
                    fineGrainData.pressureString = String(format: "%5.2f", pressure)
                }
                
                self.dispatchQueue.sync(execute: self.timer)
            })
        } catch {
            log.error("Unable to get external temp from barometer update")
        }
    }
    
    func registerForCalorieUpdates(from client: MSBClient) {
        do {
            try client.sensorManager.startCaloriesUpdates(to: nil, withHandler: {
                (caloriesData: MSBSensorCaloriesData?, error: Error?) in
                
                if let calories = caloriesData?.caloriesToday {
                    log.verbose("Total Calories today: \(calories)")
                    
                    if (self.isSampling) {
                        self.bandDataAggregate.addTotalCaloriesReading(calories)
                    }
                }
                
                self.dispatchQueue.sync(execute: self.timer)
            })
        } catch {
            log.error("Unable to get calorie from calorie update")
        }
    }
    
    func registerForSkinTemperatureUpdates(from client: MSBClient) {
        do {
            try client.sensorManager.startSkinTempUpdates(to: nil, withHandler: {
                (skinTempData: MSBSensorSkinTemperatureData?, error: Error?) in
                
                if let temp = skinTempData?.temperature {
                    log.verbose("Skin Temperature reading: \(temp)")
                    
                    if (self.isSampling) {
                        self.bandDataAggregate.addSkinTemperatureReading(temp)
                    }
                }
                
                self.dispatchQueue.sync(execute: self.timer)
            })
        } catch {
            log.error("Unable to get skin temp from skin temp update")
        }
    }
    
    func registerForGSRUpdates(from client: MSBClient) {
        do {
            try client.sensorManager.startGSRUpdates(to: nil, withHandler: {
                (gsrData: MSBSensorGSRData?, error: Error?) in
                
                if let gsr = gsrData?.resistance {
                    log.verbose("Resistance reading: \(gsr)")
                    
                    if (self.isSampling) {
                        self.bandDataAggregate.addGSRReading(gsr)
                    }
                }
                
                self.dispatchQueue.sync(execute: self.timer)
            })
        } catch {
            log.error("Unable to get resistance from GSR update")
        }
    }
    
    func registerForAmbientLightUpdates(from client: MSBClient) {
        do {
            try client.sensorManager.startAmbientLightUpdates(to: nil, withHandler: {
                (lightData: MSBSensorAmbientLightData?, error: Error?) in
            
                if let lumens = lightData?.brightness {
                    log.verbose("Light reading: \(lumens)")
                    fineGrainData.luminosityString = String(format: "%5d", lumens)
                }
                
                self.dispatchQueue.sync(execute: self.timer)
            })
        } catch {
            log.error("Unable to get brighness from ambient light update")
        }
    }
    
    func registerForContactUpdates(from client: MSBClient) {
        do {
            try client.sensorManager.startBandContactUpdates(to: nil, withHandler: {
                (contactState: MSBSensorBandContactData?, error: Error?) in
                
                if let state = contactState?.wornState {
                    if (self.isSampling) {
                        if state == MSBSensorBandContactState.worn {
                            log.verbose("Band Contact Status: worn")
                            self.bandDataAggregate.setBand(contactStatus: true)
                        } else {
                            log.verbose("Band Contact Status: unknown/not worn")
                            self.bandDataAggregate.setBand(contactStatus: false)
                        }
                    }
                }
                
                self.dispatchQueue.sync(execute: self.timer)
            })
        } catch {
            log.error("Unable to get contact state")
        }
    }
    
    private func stopStreamingIfItHasBeenMoreThan(seconds: Double) {
        // timeIntervalSinceNow returns is negative if date is in the past
        if((samplingStartTime.timeIntervalSinceNow) * -1 > seconds) {
            // Synchronously add the stopStreaming func to the serial queue
            log.verbose("Call back has tried to stop streaming")
            dispatchQueue.sync(execute: stopStreaming)
        }
    }
    
    // MARK: MSBClientManagerDelegate Methods
    func clientManager(_ clientManager: MSBClientManager!,
                       clientDidConnect client: MSBClient!)
    {
        // TODO: Add registration to sensors here
        connectionStatus = .connected
        self.updateConnectionLabel()
        startStreaming()
        mainViewController.view.makeToast("Connected to band")
        log.info("Connected to MBand <\((client.name)!)>")
    }
    
    func clientManager(_ clientManager: MSBClientManager!,
                       clientDidDisconnect client: MSBClient!)
    {
        let content = UNMutableNotificationContent()
        content.title = "Microsoft Band Disconnect"
        content.body = "Please start streaming again"
        content.sound = UNNotificationSound.default()
        
        // Deliver the notification in five seconds.
        let trigger = UNTimeIntervalNotificationTrigger.init(timeInterval: 5, repeats: false)
        let request = UNNotificationRequest.init(identifier: "BandDisconnect", content: content, trigger: trigger)
        
        // Schedule the notification.
        let center = UNUserNotificationCenter.current()
        center.add(request) { (error) in
            log.error(error)
        }
        log.info("Reconnect band notification scheduled")
        
        connectionStatus = .notConnected
        self.updateConnectionLabel()
        log.info("Disconnected from MBand <\((client.name)!)>")
    }
    
    func clientManager(_ clientManager: MSBClientManager!,
                       client: MSBClient!,
                       didFailToConnectWithError error: Error!)
    {
        connectionStatus = .notConnected
        self.updateConnectionLabel()
        mainViewController.view.makeToast("Failed to connect to band")
        log.error("Failed to connect to MBand <\((client.name)!)>")
    }
    
    // MARK: Helpers
    func getUsername() -> String {
        let preferences = UserDefaults()
        let username = preferences.string(forKey: usernameKey)
        return (username?.components(separatedBy: "@")[0])!
    }
    
    func timer() {
        let currentTime = Date()
        
        // Sampling Logic
        if (currentTime.timeIntervalSince(samplingStartTime) < self.BAND_DATA_RECORD_STREAMING_SESSION) {
            isSampling = true
            
            if(currentTime.timeIntervalSince(self.lastSampleTime) > self.BAND_DATA_SAMPLING_INTERVAL) {
                oneOffSample = false
                log.info("Sampled data")
                sampleBandData()
                lastSampleTime = currentTime
            }
        } else if (currentTime.timeIntervalSince(samplingStartTime) < self.BAND_DATA_RECORD_STREAMING_INTERVAL) {
            /* One off sample will only be set to true if a survey / activity
            report was started while 5 min after last sample */
            if (oneOffSample) {
                isSampling = true
                if (currentTime.timeIntervalSince(self.lastSampleTime) > self.BAND_DATA_SAMPLING_INTERVAL) {
                    log.info("One off data sample")
                    sampleBandData()
                    lastSampleTime = currentTime
                }
            } else {
                isSampling = false
                bandDataAggregate.clearData()
            }
        } else {
            log.info("Started sampling band data")
            isSampling = true
            samplingStartTime = currentTime
            sampleBandData()
            lastSampleTime = currentTime
        }
        
        // Survey Logic
        if ((currentTime.timeIntervalSince(lastResponseTime) > self.MINIMUM_TIME_BETWEEN_SURVEY_RESPONSE) &&
            (currentTime.timeIntervalSince(lastNotificationTime) > self.MINIMUM_TIME_BETWEEN_NOTIFICATION)) {
            notificationCenter.add(notificationRequest) { (error) in
                log.error(error)
            }
            log.info("Scheduled a notification")
            lastNotificationTime = currentTime + SURVEY_NOTIFICATION_DELAY
        }
        
        // Publish Logic
        if (currentTime.timeIntervalSince(lastPublishTime) > self.PUBLISH_DATA_INTERVAL) {
            publishData()
            lastPublishTime = currentTime
        }
    }
    
    func publishData() {
        var messages: [NSManagedObject] = []
        
        // 1 - Get a reference to the managed context form the app delegate
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else {
            return
        }
        let managedContext = appDelegate.persistentContainer.viewContext
        
        // 2 - Create a fetch request to retrieve all stored messages
        let fetchRequest = NSFetchRequest<NSManagedObject>(entityName: "Message")
        
        // 3 - Fetech the messages with the managed context
        do {
            messages = try managedContext.fetch(fetchRequest)
        } catch let error as NSError {
            print("Could not fetch. \(error), \(error.userInfo)")
        }
        
        // Loop through and send all messages
        var publishedItemCount = 0
        while(sensorAndrew.xmppStream.isAuthenticated() && messages.count > 0) {
            // Send message to Sensor Andrew
            do {
                // Copy barebones
                let message = baseXMPPMessage.copy() as! XMPPElement
                
                // Create XML Element of payload
                let xmlString = messages.last?.value(forKey: "payload") as? String
                let payloadXML = try DDXMLElement.init(xmlString: xmlString!)
                (message.child(at: 0)?.child(at: 0) as? DDXMLElement)?.addChild(payloadXML)
                
                // Publish message
                sensorAndrew.xmppStream.send(message)
                
                publishedItemCount = publishedItemCount + 1
            } catch let error as NSError {
                log.error("Could not create XML node. \(error)")
            }
            
            // Remove message from Core Data and messages list
            do {
                managedContext.delete(messages.last!)
                try managedContext.save()
                messages.removeLast()
            } catch let error as NSError {
                log.error("Error saving to Core Data. \(error)")
            }
        }
        
        log.info("Published \(publishedItemCount) items")
    }
    
    func createBaseXMPPMessage() {
        baseXMPPMessage = XMPPElement(name: "iq")
        baseXMPPMessage.addAttribute(withName: "from", stringValue: sensorAndrew.credentials.username)
        baseXMPPMessage.addAttribute(withName: "to", stringValue: "pubsub.sensor.andrew.cmu.edu")
        baseXMPPMessage.addAttribute(withName: "type", stringValue: "set")
        baseXMPPMessage.addAttribute(withName: "id", stringValue: "publish")
        
        let pubsub = XMPPElement(name: "pubsub")
        pubsub.addAttribute(withName: "xmlns", stringValue: "http://jabber.org/protocol/pubsub")
        baseXMPPMessage.addChild(pubsub)
        
        let publish = XMPPElement(name: "publish")
        publish.addAttribute(withName: "node", stringValue: nodeName)
        pubsub.addChild(publish)
    }
    
    func endClientConnection() {
        clientManager?.cancelClientConnection(client)
        log.info("Client Manager has canceled client connection")
    }
}
