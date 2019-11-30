// Author: Madhav Achar, madhav.achar@us.bosch.com
// Name: tcs_study.js
// Date: December 2016

// Purpose: Organize data reporting for study
//
// Reporting Structure:
//
//		raw_data/
//			Study_Participant1/
//				tcsDataYYYY-MM-DD.dat
//				tcsDataYYYY-MM-DD.dat
//			Study_Participant2/
//				tcsDataYYYY-MM-DD.dat
//				...
//			...

// Libraries
var XMPPClient = require('node-xmpp-client'),
	ltx = require('ltx'),
	fs = require('fs'),
	mkdirp = require('mkdirp'),
	moment = require('moment'),
	string = require('string');

// Globals
var startTime = moment()	// Start time of script
var path = './'				// location of raw_data folder 

// 1 - Create XMPP client instance
var xmppClient = new XMPPClient({
	jid: 'machar@sensor.andrew.cmu.edu/tcs_report_script',
	password: 'boschsummer2016'
})


// 2 - Log on
xmppClient.on('online', function() {
	console.log("Client is online!");

	var presence = new ltx.Element('presence', {})
		.c('show').t('chat').up()
		.c('status').t('tcs_report_script is online.')

	xmppClient.send(presence)

	// Create ./raw_data file if it does not exist
	mkdirp(path + 'raw_data', function(err) {
		if (err) 
			console.error(err)
		else 
			console.log('Created raw_data directory at path:' + path)
	})

})

// 3 - Respond to incoming messages
xmppClient.on('stanza', function(stanza) {
	
	if(stanza.is('message') && stanza.attrs.type=='headline') {
		var item = stanza.getChild('event').getChild('items').getChild('item')
	
		if (typeof item === 'undefined')
			return;	

		// Prevent writing data from old messages
		if (!string(item.attr('id')).contains('_BandData') &&
			!string(item.attr('id')).contains('_ActionData') &&
			!string(item.attr('id')).contains('_SurveyData'))
			return;	

		var date = string(item.attr('id'))
					.chompLeft('_BandData')
					.chompLeft('_ActionData')
					.chompLeft('_SurveyData').s
	
		if (startTime.isAfter(date))
			return;

		console.log('Processing message...')
			
		// Identify the sender of the message and create directory
		var jid = item.getChild('transducerData').attr('username')
		var user_dir = path + 'raw_data/' + jid	
		mkdirp(user_dir, function(err) {
				if (err)
					console.error(err)
		})

		// Parse out the date and create output filename 
		var date = item.getChild('transducerData').attr('timestamp')
		var file_name = 'tcsData' + moment(date).format('YYYY-MM-DD') + '.dat'
		var file_src = path + 'raw_data/' + jid + '/' + file_name
	
		// Create and or append data to the log file
		fs.appendFileSync(file_src, ltx.stringify(item, 0, 1) + '\n', 'utf8') 
	}
})
