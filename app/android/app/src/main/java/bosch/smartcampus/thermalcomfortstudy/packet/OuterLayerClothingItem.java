package bosch.smartcampus.thermalcomfortstudy.packet;

import android.support.annotation.Nullable;

import bosch.smartcampus.thermalcomfortstudy.lib.OuterLayerClothing;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 6/27/16.
 */
public class OuterLayerClothingItem extends SurveyDataItem {
    private static final String OUTER_LAYER_CLOTHING_NAME = "OuterLayerClothing";

    @Nullable
    private OuterLayerClothing outerLayerClothing;

    public OuterLayerClothingItem(String username, Timestamp timestamp,
                                  OuterLayerClothing outerLayerClothing) {
        super(username, timestamp);
        this.outerLayerClothing = outerLayerClothing;
    }

    public OuterLayerClothing getOuterLayerClothing() {
        return outerLayerClothing;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<" + ELEMENT + " id=\"" + getId() + "\">");
        builder.append(getTransducerDataElement(OUTER_LAYER_CLOTHING_NAME, outerLayerClothing == null ? "None" : outerLayerClothing.toString()));
        builder.append("</" + ELEMENT + ">");
        return builder.toString();
    }
}
