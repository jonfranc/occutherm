package bosch.smartcampus.thermalcomfortstudy.ui;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import bosch.smartcampus.thermalcomfortstudy.R;
import bosch.smartcampus.thermalcomfortstudy.lib.ThermalComfort;

public class ThermalComfortQuestionFragment extends Fragment {
    public static final String THERMAL_COMFORT_PARAM = "thermalComfort";

    public ThermalComfortQuestionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_thermal_comfort_question, container, false);
    }

    /**
     * Save thermal comfort to shared preferences
     */
    protected void saveThermalComfortData() {
        if (isAdded()) {
            SharedPreferences responseSharedPref = getActivity().getSharedPreferences(
                    getString(R.string.survey_response_storage),
                    Context.MODE_PRIVATE);
            RadioGroup thermalComfortInput = (RadioGroup) getActivity().findViewById(R.id.thermalComfort);
            int thermalComfortId = thermalComfortInput.getCheckedRadioButtonId();

            SharedPreferences.Editor editor = responseSharedPref.edit();
            editor.putInt(THERMAL_COMFORT_PARAM, thermalComfortId);
            editor.commit();
        }
    }

    protected ThermalComfort getThermalComfort() {
        if (isAdded()) {
            SharedPreferences sharedPref = getActivity().getSharedPreferences(
                    getString(R.string.survey_response_storage),
                    Context.MODE_PRIVATE);
            int thermalComfortId = sharedPref.getInt(THERMAL_COMFORT_PARAM, 0);
            ThermalComfort thermalComfort = convertViewIdToThermalComfort(thermalComfortId);
            return thermalComfort;
        } else {
            // This should not happen
            return null;
        }
    }

    private ThermalComfort convertViewIdToThermalComfort(int id) {
        ThermalComfort thermalComfort;
        switch (id) {
            case R.id.thermalComfort_uncomfortablyCold : thermalComfort = ThermalComfort.UNCOMFORTABLY_COLD;
                break;
            case R.id.thermalComfort_slightlyUncomfortablyCold : thermalComfort = ThermalComfort.SLIGHTLY_UNCOMFORTABLY_COLD;
                break;
            case R.id.thermalComfort_comfortable : thermalComfort = ThermalComfort.COMFORTABLE;
                break;
            case R.id.thermalComfort_slightlyUncomfortablyWarm : thermalComfort = ThermalComfort.SLIGHTLY_UNCOMFORTABLY_WARM;
                break;
            case R.id.thermalComfort_uncomfortablyWarm : thermalComfort = ThermalComfort.UNCOMFORTABLY_WARM;
                break;
            default: thermalComfort = ThermalComfort.COMFORTABLE;
                break;
        }

        return thermalComfort;
    }
}
