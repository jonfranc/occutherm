//
//  MainActivityViewController.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/3/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import UIKit

class MainActivityViewController: UIViewController {
    
    @IBOutlet weak var connectionStatusLabel: UILabel!
    @IBOutlet weak var startButton: UIButton!
    
    public let msband = MSBandService()
    
    let loggedInStatusKey = "isLoggedIn"
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        NotificationCenter.default.addObserver(self, selector: #selector(willEnterForeground), name: NSNotification.Name.UIApplicationWillEnterForeground, object: nil)
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        connectionStatusLabel.text = msband.connectionStatus.rawValue
        
        // Setup navigation item for current scene
        self.navigationItem.hidesBackButton = true
        self.navigationItem.title = "Thermal Comfort Study"
        
        // Setup navigation back button for above scenes
        let goToMainBackButton = UIBarButtonItem(title: "Go To Main",
                                                 style: .plain ,
                                                 target: self,
                                                 action: nil)
        self.navigationItem.backBarButtonItem = goToMainBackButton
    }
    
    @IBAction func logOutButton(_ sender: UIButton) {
        log.info("User is has logged out")
        // Stop all band interaction
        msband.stopStreaming()
        msband.endClientConnection()
        
        // Stop any future survey 
        msband.notificationCenter.removeAllDeliveredNotifications()
        msband.notificationCenter.removeAllPendingNotificationRequests()

        _ = navigationController?.popToRootViewController(animated: true)
    }
    
    @IBAction func startSurveyButton(_ sender: UIButton) {
        performSegue(withIdentifier: "goToSurvey", sender: self)
    }
    
    @IBAction func reportActionReportButton(_ sender: UIButton) {
        performSegue(withIdentifier: "goToActionReport", sender: self)
    }
    @IBAction func startStreamingButton(_ sender: AnyObject) {
        msband.connect(vc: self) {
            self.connectionStatusLabel.text = self.msband.connectionStatus.rawValue
            
            switch self.msband.connectionStatus {
            case .notConnected:
                self.startButton.isEnabled = true
            default:
                self.startButton.isEnabled = false
            }
        }
    }
    
    func willEnterForeground() {
        self.connectionStatusLabel.text = self.msband.connectionStatus.rawValue
    }
}
