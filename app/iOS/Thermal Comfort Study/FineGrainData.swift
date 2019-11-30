//
//  FineGrainData.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 11/23/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation
import XMPPFramework

class FineGrainData {
    
    private var username = "jszurley@sensor.andrew.cmu.edu"
    private var password = "boschsc15"
    private var pubsubServer = ""
    private var nodeName = "IPhoneNode"
    private var hostName = "sensor.andrew.cmu.edu"
    private var hostPort = 5222
    private var timer: Timer!
    
    public var isALPSRunning = false
    public var alpsManager: ALPS!
    
    private var data: [String: NSString]? = [:]
    public var point: RBPoint3D = RBPoint3D(x: 0.0, y: 0.0, z: 0.0)
    
    // Data Variables
    public var xLoc: String = ""
    public var yLoc: String = ""
    public  var ambientTemperatureString = ""
    public  var pressureString = ""
    public  var luminosityString = ""

    private var sensorAndrew = SensorAndrew()
    
    init() {
        // Create an ALPS Manager
        let callback: (([AnyHashable: Any]?) -> Void)! = setData
        alpsManager = ALPS.init(callback: callback)

        // Initialize XMPPStream Delegate
        self.sensorAndrew.connect(withUsername: username, withPassword: password) {
            if(self.sensorAndrew.xmppStream.isAuthenticated()) {
                let presence = XMPPPresence()
                self.sensorAndrew.xmppStream.send(presence)
            }
        }
    }

    func setData(pointDict: [AnyHashable : Any]?) -> Void {
        if let data = pointDict {
            self.point = RBPoint3D(x: Double(data["x"]! as! NSNumber) + 3,
                              y: Double(data["y"]! as! NSNumber) + 7,
                              z: 0.0)

            // Set coordinates
            self.xLoc = String(format: "%.1f", point.x_val)
            self.yLoc = String(format: "%.1f", point.y_val)
            
            //print(self.xLoc)
            //print(self.yLoc)
        }
    }
    
    func startAlps() {
        // Timer used to stagnate XMPP Publishing
        self.timer = Timer.init(fireAt: Date(), interval: 1, target: self, selector: #selector(constructPublish), userInfo: nil, repeats: true)
        RunLoop.current.add(self.timer, forMode: .defaultRunLoopMode)
        
        self.alpsManager.start(nil)
    }
    
    func stopAlps() {
        self.timer.invalidate()
        self.alpsManager.stop(nil)
    }
    
    @objc func constructPublish() -> Void {
        log.info("Published fine grain data to Sensor Andrew")
        
        // Get current time
        let df = DateFormatter()
        df.dateFormat = "YYYY-MM-dd'T'HH:mm:ss.SSS"
        
        let date = Date()
        let formattedDate = df.string(from: date)
        
        // Construct ALPS coordinate string
        let ALPSCoordinates = self.xLoc + ", " + self.yLoc
        
        // Construct XML message
        let message = XMPPElement(name: "iq")
        message.addAttribute(withName: "from", stringValue: "jszurley@sensor.andrew.cmu.edu")
        message.addAttribute(withName: "to", stringValue: "pubsub.sensor.andrew.cmu.edu")
        message.addAttribute(withName: "type", stringValue: "set")
        message.addAttribute(withName: "id", stringValue: "publish")
        
        let pubsub = XMPPElement(name: "pubsub")
        pubsub.addAttribute(withName: "xmlns", stringValue: "http://jabber.org/protocol/pubsub")
        message.addChild(pubsub)
        
        let publish = XMPPElement(name: "publish")
        publish.addAttribute(withName: "node", stringValue: nodeName)
        pubsub.addChild(publish)
        
        let item = XMPPElement(name: "item")
        item.addAttribute(withName: "id", stringValue: "_mobile_sensor")
        publish.addChild(item)
        
        let transducerData = XMPPElement(name: "transducerData")
        transducerData.addAttribute(withName: "deviceID", stringValue: (UIDevice.current.identifierForVendor?.uuidString)!)
        transducerData.addAttribute(withName: "location", stringValue: ALPSCoordinates)
        transducerData.addAttribute(withName: "temperature", stringValue: self.ambientTemperatureString)
        transducerData.addAttribute(withName: "humidity", stringValue: "")
        transducerData.addAttribute(withName: "vibration", stringValue: "")
        transducerData.addAttribute(withName: "light", stringValue: self.luminosityString)
        transducerData.addAttribute(withName: "pressure", stringValue: self.pressureString)
        transducerData.addAttribute(withName: "microphone", stringValue: "")
        transducerData.addAttribute(withName: "name", stringValue: "mobile_sensor")
        transducerData.addAttribute(withName: "timestamp", stringValue: formattedDate)
        transducerData.addAttribute(withName: "xmlns", stringValue: "http://www.w3.org/2005/Atom")
        item.addChild(transducerData)
        
        self.sensorAndrew.xmppStream.send(message)
    }
}
