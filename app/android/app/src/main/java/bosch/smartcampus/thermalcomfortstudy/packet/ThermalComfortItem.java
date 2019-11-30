package bosch.smartcampus.thermalcomfortstudy.packet;

import bosch.smartcampus.thermalcomfortstudy.lib.ThermalComfort;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 8/29/16.
 */
public class ThermalComfortItem extends SurveyDataItem {
    private static final String THERMAL_COMFORT_NAME = "ThermalComfort";

    private ThermalComfort thermalComfort;

    public ThermalComfortItem(String username, Timestamp timestamp,
                              ThermalComfort thermalComfort) {
        super(username, timestamp);
        this.thermalComfort = thermalComfort;
    }

    public ThermalComfort getThermalComfort() {
        return thermalComfort;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<" + ELEMENT + " id=\"" + getId() + "\">");
        builder.append(getTransducerDataElement(THERMAL_COMFORT_NAME, thermalComfort.toString()));
        builder.append("</" + ELEMENT + ">");
        return builder.toString();
    }
}
