package bosch.smartcampus.thermalcomfortstudy.packet;

import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 6/27/16.
 */
public class ActivityDescriptionItem extends SurveyDataItem {
    private static final String ACTIVITY_DESCRIPTION_NAME = "ActivityDescription";

    private String description;

    public ActivityDescriptionItem(String username, Timestamp timestamp,
                                   String description) {
        super(username, timestamp);
        this.description = description;
    }

    public String getActivityDescription() {
        return description;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<" + ELEMENT + " id=\"" + getId() + "\">");
        builder.append(getTransducerDataElement(ACTIVITY_DESCRIPTION_NAME, description));
        builder.append("</" + ELEMENT + ">");
        return builder.toString();
    }
}
