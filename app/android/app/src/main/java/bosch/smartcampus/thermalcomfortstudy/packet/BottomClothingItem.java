package bosch.smartcampus.thermalcomfortstudy.packet;

import android.support.annotation.Nullable;

import bosch.smartcampus.thermalcomfortstudy.lib.BottomClothing;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 6/27/16.
 */
public class BottomClothingItem extends SurveyDataItem {
    private static final String BOTTOM_CLOTHING_NAME = "BottomClothing";

    @Nullable
    private BottomClothing bottomClothing;

    public BottomClothingItem(String username, Timestamp timestamp,
                              BottomClothing bottomClothing) {
        super(username, timestamp);
        this.bottomClothing = bottomClothing;
    }

    public BottomClothing getBottomClothing() {
        return bottomClothing;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<" + ELEMENT + " id=\"" + getId() + "\">");
        builder.append(getTransducerDataElement(BOTTOM_CLOTHING_NAME, bottomClothing == null ? "None" : bottomClothing.toString()));
        builder.append("</" + ELEMENT + ">");
        return builder.toString();
    }
}
