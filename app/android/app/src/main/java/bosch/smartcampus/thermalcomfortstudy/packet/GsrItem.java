package bosch.smartcampus.thermalcomfortstudy.packet;

import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 6/27/16.
 */
public class GsrItem extends BandDataItem {
    private static final String GSR_NAME = "Gsr";

    private double gsr;

    public GsrItem(String username, Timestamp timestamp,
                   double gsr) {
        super(username, timestamp);
        this.gsr = gsr;
    }

    public double getGsr() {
        return gsr;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<" + ELEMENT + " id=\"" + getId() + "\">");
        builder.append(getTransducerDataElement(GSR_NAME, Double.toString(gsr)));
        builder.append("</" + ELEMENT + ">");
        return builder.toString();
    }
}
