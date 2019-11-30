package bosch.smartcampus.thermalcomfortstudy.packet;

import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 8/31/16.
 */
public class ClothingInsulationItem extends SurveyDataItem {
    private static final String CLOTHING_INSULATION_NAME = "ClothingInsulation";

    private double clothingInsulation;

    public ClothingInsulationItem(String username, Timestamp timestamp,
                                  double clothingInsulation) {
        super(username, timestamp);
        this.clothingInsulation = clothingInsulation;
    }

    public double getClothingInsulation() {
        return clothingInsulation;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<" + ELEMENT + " id=\"" + getId() + "\">");
        builder.append(getTransducerDataElement(CLOTHING_INSULATION_NAME, Double.toString(clothingInsulation)));
        builder.append("</" + ELEMENT + ">");
        return builder.toString();
    }
}
