//
//  SurveyQuestion.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 10/24/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

public enum SurveyQuestion: String {
    case question1 = "How do you feel about your thermal environment?"
    case question2 = "Describe your top clothing:"
    case question3 = "Describe your bottom clothing:"
    case question4 = "Describe your outer clothing:"
    case question5 = "What is your activity?"
    
    static let all = [question1, question2, question3, question4, question5]
}
