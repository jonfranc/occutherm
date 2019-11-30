//
//  LogInViewController.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/3/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import UIKit
import Toast_Swift
import XMPPFramework
import UserNotifications

class LogInViewController: UIViewController, UITextFieldDelegate {

    @IBOutlet weak var usernameTextField: UITextField!
    @IBOutlet weak var passwordTextField: UITextField!
    @IBOutlet weak var logInButton: UIButton!
    
    // UserDefault Keys
    let usernameKey = "username"
    let passwordKey = "password"
    let loggedInStatusKey = "isLoggedIn"
    
    // MARK: VC State Transition
    override func viewDidLoad() {
        super.viewDidLoad()
        
        usernameTextField.delegate = self
        usernameTextField.returnKeyType = .done
        passwordTextField.delegate = self
        passwordTextField.returnKeyType = .done
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        usernameTextField.placeholder = "machar@sensor.andrew.cmu.edu"
        usernameTextField.text = ""
        passwordTextField.text = ""
        logInButton.isEnabled = false
    }
    
    // MARK: IBActions
    @IBAction func logIn(_ sender: UIButton) {
        // Disable login button to prevent calling logIn multiple times
        logInButton.isEnabled = false
        
        let usr = usernameTextField.text!
        let pwd = passwordTextField.text!
            
        sensorAndrew.connect(withUsername: usr, withPassword: pwd) {
            self.logInButton.isEnabled = true
            
            if (!sensorAndrew.xmppStream.isAuthenticated()) {
                self.view.makeToast("Credentials incorrect. Please try again.")
            } else {
                self.saveUsername(username: usr)
                self.performSegue(withIdentifier: "goToMainActivity", sender: self)
            }
        }
    }
    
    // MARK: Text Field Protocols
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        if let usr = usernameTextField.text, let pwd = passwordTextField.text {
            if (usr != "" && pwd != "") {
                logInButton.isEnabled = true
            }
        }
        self.view.endEditing(true)
        return false
    }
    
    func saveUsername(username: String) {
        let preferences = UserDefaults()
        preferences.set(username, forKey: usernameKey)
    }
}

