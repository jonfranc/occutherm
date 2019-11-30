//
//  SensorAndrew.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/7/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation
import XMPPFramework
import Toast_Swift

class SensorAndrew : NSObject, XMPPStreamDelegate, XMPPReconnectDelegate {

    let hostName = "sensor.andrew.cmu.edu"
    let hostPort = UInt16(5222)
    var xmppStream: XMPPStream!
    var afterConnectionAttempt: (() -> Void)! = nil
    var credentials = (username: "", password: "")
    var xmppReconnect = XMPPReconnect()
    
    func connect(withUsername username: String,
                 withPassword password: String,
                 closure: @escaping () -> Void) {

        credentials.username = username
        credentials.password = password
        afterConnectionAttempt = closure
        
        xmppStream = XMPPStream()
        xmppStream.hostName = hostName
        xmppStream.hostPort = hostPort
        xmppStream.addDelegate(self, delegateQueue: DispatchQueue.main)
        xmppStream.myJID = XMPPJID(string: credentials.username)
        
        xmppReconnect?.activate(xmppStream)
        xmppReconnect?.addDelegate(self, delegateQueue: DispatchQueue.main)
        
        do {
            try xmppStream.connect(withTimeout: XMPPStreamTimeoutNone)
        } catch {
            print("Error connecting")
        }
    }
    
    func xmppStreamDidConnect(_ sender: XMPPStream) {
        log.verbose("Connected to Sensor Andrew")
        do {
            try sender.authenticate(withPassword: credentials.password)
        } catch {
            log.error("Error authenticating")
        }
    }
    
    func xmppStreamDidAuthenticate(_ sender: XMPPStream!) {
        log.info("Authenticated credentials successfully to SA")
        afterConnectionAttempt()
    }
    
    func xmppStream(_ sender: XMPPStream!, didNotAuthenticate error: DDXMLElement!) {
        log.info("Could not authenticate \(self.credentials.username)" +
            " with password \(self.credentials.password)")
        afterConnectionAttempt()
    }
    
    func xmppStream(_ sender: XMPPStream!, didReceiveError error: DDXMLElement!) {
        log.info("XMPP Stream received an error")
        afterConnectionAttempt()
    }
}
