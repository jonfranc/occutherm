package bosch.smartcampus.thermalcomfortstudy.packet;

import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 6/27/16.
 */
public class MinuteCaloriesItem extends BandDataItem {
    private static final String MINUTE_CALORIES_NAME = "MinuteCalories";

    private double minuteCalories;

    public MinuteCaloriesItem(String username, Timestamp timestamp,
                              double minuteCalories) {
        super(username, timestamp);
        this.minuteCalories = minuteCalories;
    }

    public double getMinuteCalories() {
        return minuteCalories;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<" + ELEMENT + " id=\"" + getId() + "\">");
        builder.append(getTransducerDataElement(MINUTE_CALORIES_NAME, Double.toString(minuteCalories)));
        builder.append("</" + ELEMENT + ">");
        return builder.toString();
    }
}
