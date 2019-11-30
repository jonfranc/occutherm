//
//  Point.swift
//  Thermal Comfort Study
//
//  Created by Madhav Achar on 11/23/16.
//  Copyright © 2016 boschsc. All rights reserved.
//

import Foundation

struct RBPoint3D {
    var x_val: Double
    var y_val: Double
    var z_val: Double

    init(x: Double, y: Double, z: Double) {
        self.x_val = x
        self.y_val = y
        self.z_val = z
    }
}
