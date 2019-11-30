package bosch.smartcampus.thermalcomfortstudy.packet;

import android.support.annotation.NonNull;

import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;
import bosch.smartcampus.thermalcomfortstudy.lib.TopClothing;

/**
 * Created by rsukkerd on 6/27/16.
 */
public class TopClothingItem extends SurveyDataItem {
    private static final String TOP_CLOTHING_NAME = "TopClothing";

    @NonNull
    private TopClothing topClothing;

    public TopClothingItem(String username, Timestamp timestamp,
                           TopClothing topClothing) {
        super(username, timestamp);
        this.topClothing = topClothing;
    }

    public TopClothing getTopClothing() {
        return topClothing;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<" + ELEMENT + " id=\"" + getId() + "\">");
        builder.append(getTransducerDataElement(TOP_CLOTHING_NAME, topClothing.toString()));
        builder.append("</" + ELEMENT + ">");
        return builder.toString();
    }
}
