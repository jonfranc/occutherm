package bosch.smartcampus.thermalcomfortstudy.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import bosch.smartcampus.thermalcomfortstudy.R;
import bosch.smartcampus.thermalcomfortstudy.lib.Activity;
import bosch.smartcampus.thermalcomfortstudy.lib.BottomClothing;
import bosch.smartcampus.thermalcomfortstudy.lib.LocalDataStorage;
import bosch.smartcampus.thermalcomfortstudy.lib.OuterLayerClothing;
import bosch.smartcampus.thermalcomfortstudy.lib.ThermalComfort;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;
import bosch.smartcampus.thermalcomfortstudy.lib.TopClothing;

/**
 * {@link SurveyActivity} is the thermal comfort survey component of the Experience Sampling.
 * It is responsible for:
 *  (1) capturing the user's comfort survey response and saving it to the local data storage via {@link LocalDataStorage}, and
 *  (2) recording the most recent time that the user submits a survey response.
 */
public class SurveyActivity extends AppCompatActivity {
    private static final String LOG_TAG = SurveyActivity.class.getSimpleName();

    public static final String TOP_CLOTHING_PARAM = "topClothing";
    public static final String BOTTOM_CLOTHING_PARAM = "bottomClothing";
    public static final String OUTER_LAYER_CLOTHING_PARAM = "outerLayerClothing";
    public static final String ACTIVITY_PARAM = "activity";
    public static final String ACTIVITY_DESCRIPTION_PARAM = "activityDescription";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);
        setUpUI();
    }

    /**
     * Submit survey response
     */
    public void submitSurveyResponse(View view) {
        ThermalComfortQuestionFragment thermalComfortQuestionFragment = (ThermalComfortQuestionFragment)
                getSupportFragmentManager().findFragmentById(R.id.thermalComfort_question_fragment);
        thermalComfortQuestionFragment.saveThermalComfortData();
        saveClothingReportData();
        saveActivityReportData();
        setPrevSurveyResponseAvailable();
        recordLastSurveyResponseTime();

        writeSurveyDataToLocalStorage();

        String confirmMessage = getString(R.string.submission_confirm);
        Toast.makeText(this, confirmMessage, Toast.LENGTH_LONG).show();
        gotoMain();
    }

    /**
     * Back to Main page (allowing user to go back to Main if they don't submit survey response)
     */
    public void backToMain(View view) {
        gotoMain();
    }

    private void setUpUI() {
        // Top clothing
        Spinner clothingTopSpinner = (Spinner) findViewById(R.id.clothing_top_list);
        ArrayAdapter<CharSequence> clothingTopAdapter = ArrayAdapter.createFromResource(this,
                R.array.clothing_top_array, R.layout.support_simple_spinner_dropdown_item);
        clothingTopAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        clothingTopSpinner.setAdapter(clothingTopAdapter);

        // Bottom clothing
        final Spinner clothingBottomSpinner = (Spinner) findViewById(R.id.clothing_bottom_list);
        ArrayAdapter<CharSequence> clothingBottomAdapter = ArrayAdapter.createFromResource(this,
                R.array.clothing_bottom_array, R.layout.support_simple_spinner_dropdown_item);
        clothingBottomAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        clothingBottomSpinner.setAdapter(clothingBottomAdapter);

        clothingTopSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] topClothingList = getResources().getStringArray(R.array.clothing_top_array);
                String[] bottomClothingList = getResources().getStringArray(R.array.clothing_bottom_array);
                String topClothingName = topClothingList[position];

                if (topClothingName.startsWith("Dress")) {
                    int noneIndex = Arrays.asList(bottomClothingList).indexOf("None");
                    clothingBottomSpinner.setSelection(noneIndex);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Outer layer clothing
        Spinner clothingOuterLayerSpinner = (Spinner) findViewById(R.id.clothing_outerLayer_list);
        ArrayAdapter<CharSequence> clothingOuterLayerAdapter = ArrayAdapter.createFromResource(this,
                R.array.clothing_outerLayer_array, R.layout.support_simple_spinner_dropdown_item);
        clothingOuterLayerAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        clothingOuterLayerSpinner.setAdapter(clothingOuterLayerAdapter);

        // Activity
        Spinner activitySpinner = (Spinner) findViewById(R.id.activity_list);
        ArrayAdapter<CharSequence> activityAdapter = ArrayAdapter.createFromResource(this,
                R.array.activity_array, R.layout.support_simple_spinner_dropdown_item);
        activityAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        activitySpinner.setAdapter(activityAdapter);

        fillOutClothingWithPreviousResponse();
    }

    /**
     * Fill out clothing questions with previous response, if exists
     */
    private void fillOutClothingWithPreviousResponse() {
        SharedPreferences responseSharedPref = getSharedPreferences(getString(R.string.survey_response_storage),
                Context.MODE_PRIVATE);

        if (responseSharedPref.contains(getString(R.string.prev_survey_response_available_key))) {
            int clothingTopPos = responseSharedPref.getInt(TOP_CLOTHING_PARAM, 0);
            int clothingBottomPos = responseSharedPref.getInt(BOTTOM_CLOTHING_PARAM, 0);
            int clothingOuterLayerPos = responseSharedPref.getInt(OUTER_LAYER_CLOTHING_PARAM, 0);

            Spinner clothingTopInput = (Spinner) findViewById(R.id.clothing_top_list);
            Spinner clothingBottomInput = (Spinner) findViewById(R.id.clothing_bottom_list);
            Spinner clothingOuterLayerInput = (Spinner) findViewById(R.id.clothing_outerLayer_list);
            clothingTopInput.setSelection(clothingTopPos);
            clothingBottomInput.setSelection(clothingBottomPos);
            clothingOuterLayerInput.setSelection(clothingOuterLayerPos);
        }
    }

    /**
     * Save clothing report to shared preferences
     */
    private void saveClothingReportData() {
        SharedPreferences responseSharedPref = getSharedPreferences(getString(R.string.survey_response_storage),
                Context.MODE_PRIVATE);

        Spinner clothingTopInput = (Spinner) findViewById(R.id.clothing_top_list);
        Spinner clothingBottomInput = (Spinner) findViewById(R.id.clothing_bottom_list);
        Spinner clothingOuterLayerInput = (Spinner) findViewById(R.id.clothing_outerLayer_list);
        int clothingTopPos = clothingTopInput.getSelectedItemPosition();
        int clothingBottomPos = clothingBottomInput.getSelectedItemPosition();
        int clothingOuterLayerPos = clothingOuterLayerInput.getSelectedItemPosition();

        SharedPreferences.Editor editor = responseSharedPref.edit();
        editor.putInt(TOP_CLOTHING_PARAM, clothingTopPos);
        editor.putInt(BOTTOM_CLOTHING_PARAM, clothingBottomPos);
        editor.putInt(OUTER_LAYER_CLOTHING_PARAM, clothingOuterLayerPos);
        editor.commit();
    }

    /**
     * Save activity report to shared preferences
     */
    private void saveActivityReportData() {
        SharedPreferences responseSharedPref = getSharedPreferences(getString(R.string.survey_response_storage),
                Context.MODE_PRIVATE);

        Spinner activityInput = (Spinner) findViewById(R.id.activity_list);
        EditText descriptionInput = (EditText) findViewById(R.id.activity_description);
        int activityPos = activityInput.getSelectedItemPosition();
        String description = descriptionInput.getText().toString();

        SharedPreferences.Editor editor = responseSharedPref.edit();
        editor.putInt(ACTIVITY_PARAM, activityPos);
        editor.putString(ACTIVITY_DESCRIPTION_PARAM, description);
        editor.commit();
    }

    /**
     * Indicate that user has submitted a survey response, and it can be used for submitPreviousSurveyResponse()
     */
    private void setPrevSurveyResponseAvailable() {
        SharedPreferences responseSharedPref = getSharedPreferences(getString(R.string.survey_response_storage),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = responseSharedPref.edit();
        editor.putBoolean(getString(R.string.prev_survey_response_available_key), true);
        editor.commit();
    }

    /**
     * Record the most recent time that user submits a survey response
     */
    private void recordLastSurveyResponseTime() {
        Timestamp now = new Timestamp();
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = loginSharedPref.edit();
        editor.putString(getString(R.string.last_survey_response_time_key), now.toString());
        editor.commit();
    }

    /**
     * Write survey data to local storage
     */
    private void writeSurveyDataToLocalStorage() {
        String username = getUsername();
        Timestamp timestamp = new Timestamp();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.survey_response_storage),
                Context.MODE_PRIVATE);
        int clothingTopPos = sharedPref.getInt(TOP_CLOTHING_PARAM, 0);
        int clothingBottomPos = sharedPref.getInt(BOTTOM_CLOTHING_PARAM, 0);
        int clothingOuterLayerPos = sharedPref.getInt(OUTER_LAYER_CLOTHING_PARAM, 0);
        int activityPos = sharedPref.getInt(ACTIVITY_PARAM, 0);

        ThermalComfortQuestionFragment thermalComfortQuestionFragment = (ThermalComfortQuestionFragment)
                getSupportFragmentManager().findFragmentById(R.id.thermalComfort_question_fragment);
        ThermalComfort thermalComfort = thermalComfortQuestionFragment.getThermalComfort();

        TopClothing topClothing = convertListPosToTopClothing(clothingTopPos);
        BottomClothing bottomClothing = convertListPosToBottomClothing(clothingBottomPos);
        OuterLayerClothing outerLayerClothing = convertListPosToOuterLayerClothing(clothingOuterLayerPos);
        double clothingInsulation = calculateEnsembleClothingInsulation(topClothing, bottomClothing, outerLayerClothing);
        Activity activity = convertListPosToActivity(activityPos);
        String activityDescription = activity == Activity.OTHER ? sharedPref.getString(ACTIVITY_DESCRIPTION_PARAM, "") : "";

        try {
            LocalDataStorage storage = new LocalDataStorage(true);
            storage.writeSurveyData(username, timestamp,
                    thermalComfort,
                    topClothing, bottomClothing, outerLayerClothing,
                    clothingInsulation,
                    activity, activityDescription);
            storage.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TopClothing convertListPosToTopClothing(int pos) {
        String[] topClothingList = getResources().getStringArray(R.array.clothing_top_array);
        String name = topClothingList[pos];

        for (TopClothing topClothing : TopClothing.values()) {
            if (name.equals(topClothing.getName())) {
                return topClothing;
            }
        }

        return TopClothing.C1;
    }

    private BottomClothing convertListPosToBottomClothing(int pos) {
        String[] bottomClothingList = getResources().getStringArray(R.array.clothing_bottom_array);
        String name = bottomClothingList[pos];

        for (BottomClothing bottomClothing : BottomClothing.values()) {
            if (name.equals(bottomClothing.getName())) {
                return bottomClothing;
            }
        }

        return null;
    }

    private OuterLayerClothing convertListPosToOuterLayerClothing(int pos) {
        String[] outerLayerClothingList = getResources().getStringArray(R.array.clothing_outerLayer_array);
        String name = outerLayerClothingList[pos];

        for (OuterLayerClothing outerLayerClothing : OuterLayerClothing.values()) {
            if (name.equals(outerLayerClothing.getName())) {
                return outerLayerClothing;
            }
        }

        return null;
    }

    private double calculateEnsembleClothingInsulation(@NonNull  TopClothing topClothing,
                                                       @Nullable BottomClothing bottomClothing,
                                                       @Nullable OuterLayerClothing outerLayerClothing) {
        double topClothingInsulation = topClothing.getClothingInsulation();
        double bottomClothingInsulation = bottomClothing == null ? 0 : bottomClothing.getClothingInsulation();
        double outerLayerClothingInsulation = outerLayerClothing == null ? 0 : outerLayerClothing.getClothingInsulation();
        return topClothingInsulation + bottomClothingInsulation + outerLayerClothingInsulation;
    }

    private Activity convertListPosToActivity(int pos) {
        Activity activity;
        String[] activityList = getResources().getStringArray(R.array.activity_array);
        String name = activityList[pos];
        switch (name) {
            case "Working":
                activity = Activity.WORKING;
                break;
            case "Meeting":
                activity = Activity.MEETING;
                break;
            case "Giving a presentation":
                activity = Activity.PRESENTING;
                break;
            case "Lunchtime activity":
                activity = Activity.LUNCHTIME_ACTIVITY;
                break;
            case "Socializing":
                activity = Activity.SOCIALIZING;
                break;
            case "Recreation":
                activity = Activity.RECREATION;
                break;
            case "Other":
                activity = Activity.OTHER;
                break;
            default:
                activity = Activity.WORKING;
        }

        return activity;
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
