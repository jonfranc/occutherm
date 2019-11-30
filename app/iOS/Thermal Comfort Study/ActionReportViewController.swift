//
//  ActionReportViewController.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/3/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import UIKit
import CoreData

class ActionReportViewController: UIViewController, UIPickerViewDelegate,
UITableViewDelegate, UITableViewDataSource, UIPickerViewDataSource, UITextFieldDelegate {
    
    @IBOutlet weak var actionTableView: UITableView!
    @IBOutlet weak var thermalComfortPicker: UIPickerView!
    @IBOutlet weak var thermalActionOtherTextField: UITextField!
    @IBOutlet weak var submitButton: UIButton!
    
    // Seconds
    private let FIVE_MINUTES = 300.0
    
    private var thermalComfortOptions = [ThermalComfort]()
    private var thermalActionOptions = [AdaptiveComfortAction]()
    private var thermalActionSwitches = [UISwitch]()
    private var enabledSwitchCount = 0
    
    private let usernameKey = "username"
    private var username: String!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
        // Thermal Comfort Picker setup
        self.thermalComfortPicker.delegate = self
        self.thermalComfortPicker.dataSource = self
        
        thermalComfortOptions = ThermalComfort.all
        
        // Action Table View setup
        self.actionTableView.delegate = self
        self.actionTableView.dataSource = self
        self.actionTableView.register(
            UITableViewCell.self, forCellReuseIdentifier: "cell")
        
        thermalActionOptions = AdaptiveComfortAction.all
        
        // Accessory Views setup (switches and text field)
        for i in 0..<thermalActionOptions.count {
            self.thermalActionSwitches.append(UISwitch())
            
            // For "Other" thermal action option enable callback on value change
            if (i == (thermalActionOptions.count - 1)) {
                self.thermalActionSwitches[i].addTarget(
                    self, action: #selector(ActionReportViewController.setOtherTextFieldEditable), for: .valueChanged)
            } else {
                self.thermalActionSwitches[i].addTarget(
                    self, action: #selector(ActionReportViewController.updateEnabledSwitchCount(_:)), for: .valueChanged)
            }
        }
        
        // Action Other Text Field setup
        thermalActionOtherTextField.delegate = self
        thermalActionOtherTextField.isEnabled = false
        thermalActionOtherTextField.returnKeyType = .done
        
        submitButton.isEnabled = false
        
        // Set username
        username = getUsername()
        
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    @IBAction func submitActionReport(_ sender: UIButton) {
        
        var actionList = [String]()
        var otherDescription: String?
        
        // Create action list and fill in other description
        for (index, uiSwitch) in thermalActionSwitches.enumerated() {
            if (uiSwitch.isOn) {
                actionList.append(thermalActionOptions[index].rawValue)
                if (index == (thermalActionSwitches.count - 1)) {
                    otherDescription = thermalActionOtherTextField.text
                }
            }
        }
        
        let timestamp = Date()
        
        // Write data to local storage
        let localDataStorage = LocalDataStorage()
        for action in actionList {
            var description: String? = nil
            
            if (action == AdaptiveComfortAction.OTHER.rawValue) {
                description = otherDescription
            }
            
            let actionItem = AdaptiveComfortActionItem(username: username, timestamp: timestamp, action: action, description: description)
            saveXMLToCoreData(actionItem.toXML())
            localDataStorage.write(data: actionItem.toXML())
        }
        
        let thermalComfort = thermalComfortOptions[thermalComfortPicker.selectedRow(inComponent: 0)].rawValue
        let thermalComfortItem = ThermalComfortItem(username: username, timestamp: timestamp, thermalComfort: thermalComfort)
        saveXMLToCoreData(thermalComfortItem.toXML())
        localDataStorage.write(data: thermalComfortItem.toXML())
        
        //Should one off sample
        let currentTime = Date()
        for vc in (navigationController?.viewControllers)! {
            if let mainActivityViewController = vc as? MainActivityViewController {
                if (currentTime.timeIntervalSince(mainActivityViewController.msband.lastSampleTime) > FIVE_MINUTES) {
                    mainActivityViewController.msband.oneOffSample = true
                }
            }
        }
        
        // Go back to main menu
        _ = navigationController?.popViewController(animated: true)
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
    
    /*** Picker data source and delegate protocols ***/
    // The number of columns of data
    func numberOfComponents(in pickerView: UIPickerView) -> Int {
        return 1
    }
    
    // The number of rows of data
    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        return thermalComfortOptions.count
    }
    
    // The data to return for the row and component (column) that's being passed in
    func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
        return thermalComfortOptions[row].rawValue
    }
    /*************************************************/
    
    /*** TableView data source and delegate protocols ***/
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return self.thermalActionOptions.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = actionTableView!.dequeueReusableCell(withIdentifier: "cell")
        cell?.textLabel?.text = self.thermalActionOptions[indexPath.row].rawValue
        cell?.accessoryView = thermalActionSwitches[indexPath.row]
        return cell!
    }
    /****************************************************/
    
    /***************** Switch Functions *****************/
    func setOtherTextFieldEditable(_ sender: UISwitch) {
        if (sender.isOn) {
            enabledSwitchCount += 1
            thermalActionOtherTextField!.isEnabled = true
        } else {
            enabledSwitchCount -= 1
            thermalActionOtherTextField!.isEnabled = false
            thermalActionOtherTextField!.text = ""
        }
        
        if (enabledSwitchCount == 0 || (enabledSwitchCount == 1 && sender.isOn)) {
            submitButton.isEnabled = false
        } else {
            submitButton.isEnabled = true
        }
    }
    
    func updateEnabledSwitchCount(_ sender: UISwitch) {
        if (sender.isOn) {
            enabledSwitchCount += 1
        } else {
            enabledSwitchCount -= 1
        }
        
        if (enabledSwitchCount == 0 ||
            (enabledSwitchCount == 1 && thermalActionSwitches.last!.isOn &&
                thermalActionOtherTextField!.text! == "")) {
            submitButton.isEnabled = false
        } else {
            submitButton.isEnabled = true
        }
    }
    /****************************************************/
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        
        if let text = textField.text, enabledSwitchCount == 1 {
            if (text != "") {
                submitButton.isEnabled = true
            }
        }
        return false
    }
    
    func getUsername() -> String {
        let preferences = UserDefaults()
        let username = preferences.string(forKey: usernameKey)
        return (username?.components(separatedBy: "@")[0])!
    }
}
