package bosch.smartcampus.thermalcomfortstudy.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bosch.smartcampus.thermalcomfortstudy.R;
import bosch.smartcampus.thermalcomfortstudy.lib.AdaptiveComfortAction;
import bosch.smartcampus.thermalcomfortstudy.lib.LocalDataStorage;
import bosch.smartcampus.thermalcomfortstudy.lib.ThermalComfort;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * {@link ActionReportActivity} is the self-initiated action report component of the Experience Sampling.
 * It is responsible for capturing the user's adaptive comfort action report and saving it to the
 * local data storage via {@link LocalDataStorage}.
 */
public class ActionReportActivity extends AppCompatActivity {
    private static final String LOG_TAG = ActionReportActivity.class.getSimpleName();

    private class ActionSelectionListener implements CompoundButton.OnCheckedChangeListener {
        private View inputView;

        public ActionSelectionListener(View inputView) {
            this.inputView = inputView;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            inputView.setEnabled(isChecked);
        }
    }

    private class DirectionLabelListener implements CompoundButton.OnCheckedChangeListener {
        private String checkedLabel;
        private String uncheckedLable;

        public DirectionLabelListener(String checkedLabel, String uncheckedLable) {
            this.checkedLabel = checkedLabel;
            this.uncheckedLable = uncheckedLable;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            String label = isChecked ? checkedLabel : uncheckedLable;
            buttonView.setText(label);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_report);

        setUpUI();
    }

    private void setUpUI() {
        CheckBox thermostatCheckBox = (CheckBox) findViewById(R.id.action_thermostat);
        CheckBox spaceHeaterCheckBox = (CheckBox) findViewById(R.id.action_spaceHeater);
        CheckBox fanCheckBox = (CheckBox) findViewById(R.id.action_fan);
        CheckBox layerCheckBox = (CheckBox) findViewById(R.id.action_layer);
        CheckBox otherCheckBox = (CheckBox) findViewById(R.id.action_other);

        Switch thermostatSwitch = (Switch) findViewById(R.id.action_thermostat_direction);
        Switch spaceHeaterSwitch = (Switch) findViewById(R.id.action_spaceHeater_direction);
        Switch fanSwitch = (Switch) findViewById(R.id.action_fan_direction);
        Switch layerSwitch = (Switch) findViewById(R.id.action_layer_direction);
        EditText descriptionEditText = (EditText) findViewById(R.id.action_description);

        thermostatCheckBox.setOnCheckedChangeListener(new ActionSelectionListener(thermostatSwitch));
        spaceHeaterCheckBox.setOnCheckedChangeListener(new ActionSelectionListener(spaceHeaterSwitch));
        fanCheckBox.setOnCheckedChangeListener(new ActionSelectionListener(fanSwitch));
        layerCheckBox.setOnCheckedChangeListener(new ActionSelectionListener(layerSwitch));
        otherCheckBox.setOnCheckedChangeListener(new ActionSelectionListener(descriptionEditText));

        thermostatSwitch.setOnCheckedChangeListener(new DirectionLabelListener("Warmer", "Cooler"));
        spaceHeaterSwitch.setOnCheckedChangeListener(new DirectionLabelListener("Turn on", "Turn off"));
        fanSwitch.setOnCheckedChangeListener(new DirectionLabelListener("Turn on", "Turn off"));
        layerSwitch.setOnCheckedChangeListener(new DirectionLabelListener("Put on", "Take off"));
    }

    /**
     * Submit action report
     */
    public void submitActionReport(View view) {
        writeActionDataToLocalStorage();

        String confirmMessage = getString(R.string.submission_confirm);
        Toast.makeText(this, confirmMessage, Toast.LENGTH_LONG).show();
        gotoMain();
    }

    /**
     * Back to Main page (allowing user to go back to Main if they don't submit action report)
     */
    public void backToMain(View view) {
        gotoMain();
    }

    /**
     * Write action data to local storage
     */
    private void writeActionDataToLocalStorage() {
        String username = getUsername();
        Timestamp timestamp = new Timestamp();

        List<AdaptiveComfortAction> actions = new ArrayList<>();
        String description = "";

        CheckBox thermostatCB = (CheckBox) findViewById(R.id.action_thermostat);
        CheckBox spaceHeaterCB = (CheckBox) findViewById(R.id.action_spaceHeater);
        CheckBox fanCB = (CheckBox) findViewById(R.id.action_fan);
        CheckBox layerCB = (CheckBox) findViewById(R.id.action_layer);
        CheckBox otherCB = (CheckBox) findViewById(R.id.action_other);

        if (thermostatCB.isChecked()) {
            Switch thermostatSW = (Switch) findViewById(R.id.action_thermostat_direction);

            if (thermostatSW.isChecked()) {
                actions.add(AdaptiveComfortAction.THERMOSTAT_WARMER);
            } else {
                actions.add(AdaptiveComfortAction.THERMOSTAT_COOLER);
            }
        }

        if (spaceHeaterCB.isChecked()) {
            Switch spaceHeaterSW = (Switch) findViewById(R.id.action_spaceHeater_direction);

            if (spaceHeaterSW.isChecked()) {
                actions.add(AdaptiveComfortAction.SPACE_HEATER_ON);
            } else {
                actions.add(AdaptiveComfortAction.SPACE_HEATER_OFF);
            }
        }

        if (fanCB.isChecked()) {
            Switch fanSW = (Switch) findViewById(R.id.action_fan_direction);

            if (fanSW.isChecked()) {
                actions.add(AdaptiveComfortAction.FAN_ON);
            } else {
                actions.add(AdaptiveComfortAction.FAN_OFF);
            }
        }

        if (layerCB.isChecked()) {
            Switch layerSW = (Switch) findViewById(R.id.action_layer_direction);
            boolean extraLayer = layerSW.isChecked();

            if (extraLayer) {
                actions.add(AdaptiveComfortAction.LAYER_ON);
            } else {
                actions.add(AdaptiveComfortAction.LAYER_OFF);
            }

            // Edit EXTRA_CLOTHING value in survey response shared preferences
            SharedPreferences responseSharedPref = getSharedPreferences(getString(R.string.survey_response_storage),
                    Context.MODE_PRIVATE);
            boolean prevResponseAvailable = responseSharedPref
                    .getBoolean(getString(R.string.prev_survey_response_available_key), false);
            if (prevResponseAvailable) {
                SharedPreferences.Editor editor = responseSharedPref.edit();
                editor.putBoolean(SurveyActivity.OUTER_LAYER_CLOTHING_PARAM, extraLayer);
                editor.commit();
            }
        }

        if (otherCB.isChecked()) {
            actions.add(AdaptiveComfortAction.OTHER);
            EditText actionDescriptionET = (EditText) findViewById(R.id.action_description);
            description = actionDescriptionET.getText().toString();
        }

        ThermalComfortQuestionFragment thermalComfortQuestionFragment = (ThermalComfortQuestionFragment)
                getSupportFragmentManager().findFragmentById(R.id.sensation_questions_fragment);
        thermalComfortQuestionFragment.saveThermalComfortData();
        ThermalComfort thermalComfort = thermalComfortQuestionFragment.getThermalComfort();

        try {
            LocalDataStorage storage = new LocalDataStorage(true);
            storage.writeActionData(username, timestamp,
                    actions, description);
            storage.writeThermalComfortData(username, timestamp,
                    thermalComfort);
            storage.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getUsername() {
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        String username = loginSharedPref.getString(getString(R.string.username_key), null);
        assert username != null;
        return username;
    }

    /**
     * Go to Main page
     */
    private void gotoMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
