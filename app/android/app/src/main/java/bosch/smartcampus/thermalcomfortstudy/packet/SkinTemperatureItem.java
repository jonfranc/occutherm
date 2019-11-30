package bosch.smartcampus.thermalcomfortstudy.packet;

import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 6/27/16.
 */
public class SkinTemperatureItem extends BandDataItem {
    private static final String SKIN_TEMPERATURE_NAME = "SkinTemperature";

    private double skinTemperature;

    public SkinTemperatureItem(String username, Timestamp timestamp,
                               double skinTemperature) {
        super(username, timestamp);
        this.skinTemperature = skinTemperature;
    }

    public double getSkinTemperature() {
        return skinTemperature;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<" + ELEMENT + " id=\"" + getId() + "\">");
        builder.append(getTransducerDataElement(SKIN_TEMPERATURE_NAME, Double.toString(skinTemperature)));
        builder.append("</" + ELEMENT + ">");
        return builder.toString();
    }
}
