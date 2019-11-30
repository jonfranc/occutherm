//
//  SurveyViewController.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/3/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import UIKit
import CoreData

class SurveyViewController: UIViewController, UIPickerViewDelegate,
UIPickerViewDataSource, UITextFieldDelegate {
    
    // MARK: IBOutlets
    @IBOutlet weak var questionLabel: UILabel!
    @IBOutlet weak var optionPicker: UIPickerView!
    @IBOutlet weak var otherLabel: UILabel!
    @IBOutlet weak var otherTextField: UITextField!
    @IBOutlet weak var nextButton: UIButton!
    @IBOutlet weak var previousButton: UIButton!
    @IBOutlet weak var submitSurveyButton: UIButton!
    
    // Seconds
    private let FIVE_MINUTES = 300.0
    
    private enum State: Int {
        case q1 = 0, q2, q3, q4, q5
    }
    
    // MARK: Class Variables
    private var question: State = State.q1
    private let pickerOptions: [[Any]] = [ThermalComfort.all,
                                      TopClothing.all,
                                      BottomClothing.all,
                                      OuterLayerClothing.all,
                                      Activity.all]
    private var answer: SurveyDataItem!
    private var answerList = [SurveyDataItem]()
    private var insulation: Double = 0
    private var totalInsulation = [Double]()
    private let usernameKey = "username"
    private var username: String!
    
    // MARK: View Transitions
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Setup up delegate and data source for picker
        optionPicker.delegate = self
        optionPicker.dataSource = self
        
        // Hide other field, other label, previous button, submit button
        otherLabel.isHidden = true
        otherTextField.isHidden = true
        previousButton.isEnabled = false
        submitSurveyButton.isEnabled = false
        
        otherTextField.delegate = self
        otherTextField.returnKeyType = .done
        
        // Set username
        username = getUsername()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        fineGrainData.startAlps()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        
        fineGrainData.stopAlps()
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    // MARK: Picker protocols
    /*** Picker data source and delegate protocols ***/
    // The number of columns of data
    func numberOfComponents(in pickerView: UIPickerView) -> Int {
        return 1
    }
    
    // The number of rows of data
    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        return pickerOptions[question.rawValue].count
    }
    
    // The data to return for the row and component (column) that's being passed in
    func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
        
        switch(question) {
        case .q1:
            return (pickerOptions[question.rawValue][row] as! ThermalComfort).rawValue
        case .q2:
            return (pickerOptions[question.rawValue][row] as! TopClothing).values().name
        case .q3:
            return (pickerOptions[question.rawValue][row] as! BottomClothing).values().name
        case .q4:
            return (pickerOptions[question.rawValue][row] as! OuterLayerClothing).values().name
        case .q5:
            return (pickerOptions[question.rawValue][row] as! Activity).rawValue
        }
    }
    
    func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
        if let activity = pickerOptions[question.rawValue][row] as? Activity {
            if (activity.rawValue == "Other") {
                otherLabel.isEnabled = true
                otherTextField.isEnabled = true
            } else {
                otherLabel.isEnabled = false
                otherTextField.isEnabled = false
            }
        }
    }
    /*************************************************/
    
    // MARK: Text Field Protocols
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        return false
    }
    
    // MARK: IBActions
    @IBAction func next(_ sender: UIButton) {
        
        var surveyDataItem = ""
        switch(question) {
        case .q1:
            surveyDataItem = (pickerOptions[question.rawValue][optionPicker!.selectedRow(inComponent: 0)] as! ThermalComfort).rawValue
            answer = ThermalComfortItem(username: username, timestamp: Date(), thermalComfort: surveyDataItem)
        case .q2:
            (insulation, surveyDataItem, _) = (pickerOptions[question.rawValue][optionPicker!.selectedRow(inComponent: 0)] as! TopClothing).values()
            answer = TopClothingItem(username: username, timestamp: Date(), topClothing: surveyDataItem)
        case .q3:
            (insulation, surveyDataItem, _) = (pickerOptions[question.rawValue][optionPicker!.selectedRow(inComponent: 0)] as! BottomClothing).values()
            answer = BottomClothingItem(username: username, timestamp: Date(), bottomClothing: surveyDataItem)
        case .q4:
            (insulation, surveyDataItem, _) = (pickerOptions[question.rawValue][optionPicker!.selectedRow(inComponent: 0)] as! OuterLayerClothing).values()
            answer = OuterLayerClothingItem(username: username, timestamp: Date(), outerLayerClothing: surveyDataItem)
        case .q5:
            // This case cannot happen
            break
        }
        
        if let state = State(rawValue: question.rawValue + 1) {
            // Save answer
            answerList.append(answer)
            
            // Add up insulation
            totalInsulation.append(insulation)
            
            // Setup next question
            question = state
            questionLabel.text = (SurveyQuestion.all)[question.rawValue].rawValue
            optionPicker.reloadAllComponents()
            
            // Setup UI
            previousButton.isEnabled = true
            
            if (question == State.q5) {
                
                // Create insulation answer
                let insulationItem = ClothingInsulationItem(
                    username: username, timestamp: Date(), clothingInsulation: totalInsulation.reduce(0, +))
                answerList.append(insulationItem)
                
                nextButton.isEnabled = false
                submitSurveyButton.isEnabled = true
                otherLabel.isHidden = false
                otherLabel.isEnabled = false
                otherTextField.text = ""
                otherTextField.isHidden = false
                otherTextField.isEnabled = false
            }
        } else {
            log.error("No more questions exist")
        }
    }
    
    @IBAction func previous(_ sender: UIButton) {
        if let state = State(rawValue: question.rawValue - 1) {
            question = state
            // Pop answer(s) (If returning to q4, pop insulation item also
            _ = answerList.popLast()
            if (question == State.q4) { _ = answerList.popLast() }
            
            // Remove insulation
            _ = totalInsulation.popLast()
            
            // Setup previous question
            questionLabel.text = (SurveyQuestion.all)[question.rawValue].rawValue
            optionPicker.reloadAllComponents()
            
            // Setup UI
            nextButton.isEnabled = true
            submitSurveyButton.isEnabled = false
            otherLabel.isHidden = true
            otherTextField.isHidden = true
            
            if (question == State.q1) {
                previousButton.isEnabled = false
            }
        } else {
            log.error("Can't go back, already at first question")
        }
    }
    
    @IBAction func submit(_ sender: UIButton) {
        // Append answer to last question to answer list
        let surveyDataItem = (pickerOptions[question.rawValue][optionPicker!.selectedRow(inComponent: 0)] as! Activity).rawValue
        answer = ActivityItem(username: username, timestamp: Date(), activity: surveyDataItem)
        
        var activityDescription = ""
        if let text = otherTextField.text, surveyDataItem == Activity.OTHER.rawValue {
            activityDescription = text
        }
        
        answerList.append(answer)
        
        // Append activity description
        let activityDescriptionItem = ActivityDescriptionItem(
            username: username, timestamp: Date(), description: activityDescription)
        answerList.append(activityDescriptionItem)
        
        let timestamp = Date()
        let localDataStorage = LocalDataStorage()
        
        // Write to local data storage
        for answer in answerList {
            // Reset timestamps for all questions
            answer.set(timestamp: timestamp)
            saveXMLToCoreData(answer.toXML())
            localDataStorage.write(data: answer.toXML())
        }
        
        for vc in (navigationController?.viewControllers)! {
            let currentTime = Date()
            if let mainActivityViewController = vc as? MainActivityViewController {
                if (currentTime.timeIntervalSince(mainActivityViewController.msband.lastSampleTime) > FIVE_MINUTES) {
                    mainActivityViewController.msband.oneOffSample = true
                    mainActivityViewController.msband.lastResponseTime = currentTime
                }
            }
        }
        
        // Go back to main menu
        _ = navigationController?.popViewController(animated: true)
    }
    
    // MARK: Helpers
    func getUsername() -> String {
        let preferences = UserDefaults()
        let username = preferences.string(forKey: usernameKey)
        return (username?.components(separatedBy: "@")[0])!
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
}
