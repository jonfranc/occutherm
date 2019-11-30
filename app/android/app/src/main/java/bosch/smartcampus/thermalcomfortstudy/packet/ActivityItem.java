package bosch.smartcampus.thermalcomfortstudy.packet;

import bosch.smartcampus.thermalcomfortstudy.lib.Activity;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 6/27/16.
 */
public class ActivityItem extends SurveyDataItem {
    private static final String ACTIVITY_NAME = "Activity";

    private Activity activity;

    public ActivityItem(String username, Timestamp timestamp,
                        Activity activity) {
        super(username, timestamp);
        this.activity = activity;
    }

    public Activity getActivity() {
        return activity;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<" + ELEMENT + " id=\"" + getId() + "\">");
        builder.append(getTransducerDataElement(ACTIVITY_NAME, activity.toString()));
        builder.append("</" + ELEMENT + ">");
        return builder.toString();
    }
}
