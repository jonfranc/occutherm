package bosch.smartcampus.thermalcomfortstudy.packet;

import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 6/27/16.
 */
public class TemperatureItem extends BandDataItem {
    private static final String TEMPERATURE_NAME = "Temperature";

    private double temperature;

    public TemperatureItem(String username, Timestamp timestamp,
                           double temperature) {
        super(username, timestamp);
        this.temperature = temperature;
    }

    public double getTemperature() {
        return temperature;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<" + ELEMENT + " id=\"" + getId() + "\">");
        builder.append(getTransducerDataElement(TEMPERATURE_NAME, Double.toString(temperature)));
        builder.append("</" + ELEMENT + ">");
        return builder.toString();
    }
}
