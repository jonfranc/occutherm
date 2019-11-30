## Thermal Comfort Study App Documentation

#### Table of Contents
- Thermal Comfort Study
- ALPS Integration
- TCS Reporting

#### Thermal Comfort Study
The Thermal Comofort Study aims to use the sensors on the Microsoft Band to be able to take into account user thermal comfort preferences when performing building actuation. The study collects data via a mobile app. The link to the android app can be found here: http://gitlab.boschsc.ddns.net/bosch-sc/occthermcomf

__Important App Parameters__  
The following parameters can be found in the MSBandService.swift file.

```swift
// Sample once per minute (average all data recieved in last minute)
private var BAND_DATA_SAMPLING_INTERVAL = 60.0
// Sample once per minute for 10 minutes
private var BAND_DATA_RECORD_STREAMING_SESSION = 600.0
// Sample for 10 minutes on, 10 minutes off
private var BAND_DATA_RECORD_STREAMING_INTERVAL = 1200.0
// A survey notification shouldn't be sent if one has been submitted within 30 minutes
private var MINIMUM_TIME_BETWEEN_SURVEY_RESPONSE = 1800.0
// Send a notification to the user every hour
private var MINIMUM_TIME_BETWEEN_NOTIFICATION = 3000.0
private var SURVEY_NOTIFICATION_DELAY = 600.0
// Attempt to publish data to Sensor Andrew every 10 minutes
private var PUBLISH_DATA_INTERVAL = 600.0
```

__Implementation Note__  
For the most part, iOS does not allow running background activities in an app. There are certain exceptions including updates from BLE devices. Within the callback of each sensor there is a call to dispatch an execution to the timer function in the MSBandService.swift.
```swift
{ // Band callback
    ...
    self.dispatchQueue.sync(execute: self.timer)
}

func timer() {
	let currentTime = Date()

	// Sampling Logic - Should sample now?
	// Survey Logic - Should schedule a survey notification?
	// Publish Logic - Should publish to Sensor Andrew now?
}
```
The function looks at the current time and based on the last time the app has sampled/scheduled a notification/published will make a decision on if to sample/schedule a notification/publish now.

__Sample Messages:__  
*Survey Data*
```xml
<iq from="machar@sensor.andrew.cmu.edu" to="pubsub.sensor.andrew.cmu.edu" type="set" id="publish">
<pubsub xmlns="http://jabber.org/protocol/pubsub">
<publish node="occthermcomf_bosch">
<item id="_SurveyData2016-13-09T11:07:25.215000-0500">
<transducerData id="occthermcomf_bosch" type="TCS" class="SurveyData" username="machar" timestamp="2016-12-09T11:07:25.215000-0500" name="ThermalComfort" value="Comfortable"/>
</item>
</publish>
</pubsub>
</iq>
```

#### ALPS Integration
ALPS is an acoustic localization technology developed at CMU. Please look at the following repo: http://gitlab.boschsc.ddns.net/jszurley/sensorLocalization and ask Joe Szurley for more information.
ALPS is also integrated into the thermal comfort study app. The class FineGrainData encapsulates ALPS and the single app instance of it is instantiated in the AppDelegate.swift file. The calls to fineGrainData.startAlps() and fineGrainData.stopAlps() are placed in the viewWillAppear function and viewWillDissapear function in the SurveyViewController.swift file. Thus a connection to ALPS is only on when the user is in the process of taking a survey. ALPS currently reports out ambient temperature, pressure, and luminosity. When the sensor reporting callbacks for the band get triggered in the MSBandService.swift file the latest values of abmient temperature, pressure and luminosity are stored in the fineGrainData instance. The Sensor Andrew client is also stored within the fineGrainData instance as well. 

#### TCS Reporting
The iOS app supports two forms of reporting:
1. Log files: The app itself stores all the band, survey, and action data in a separate file for each day within the documents folder of the app. This can be accessed by using a program such as iFunbox which can be downloaded here: http://www.i-funbox.com/. To use the program plug your iPhone in to your computer and open up the app. Navigate to Device Name -> App File Sharing -> TC Study and double click. You should see a log file for each day you have participated in the study.
2. Reporting script: The app in addition to writing the values in a log file publishes the data to Sensor Andrew. Included within this repo is a NodeJS reporting script (tcs_report.js). The script creates an XMPP client that is subscribed to all messages publiished to the thermal comfort study node (occthermcomf_bosch). The script parses all the incoming messages by date and user, and writes all the messages to the appropriate log files. The files are placed in a folder structure as below:

raw_data/  
&nbsp;&nbsp;tcs_participant1/  
&nbsp;&nbsp;&nbsp;&nbsp;tcsDataYYYY-MM-DD.dat  
&nbsp;&nbsp;tcs_participant2/  
&nbsp;&nbsp;&nbsp;&nbsp;tcsDataYYYY-MM-DD.dat  
&nbsp;&nbsp;&nbsp;&nbsp;tcsDataYYYY-MM-DD.dat  
&nbsp;&nbsp;&nbsp;&nbsp;tcsDataYYYY-MM-DD.dat  
&nbsp;&nbsp;&nbsp;&nbsp;...  
&nbsp;&nbsp;...  

__Requirements:__  
If there are issues running the script, please try upgrade node to a later version.

__Usage:__ 
```
node tcs_report.js
```
To leave the script running on a remote computer, use the screen command to create a new window and detatch the window after running the script.