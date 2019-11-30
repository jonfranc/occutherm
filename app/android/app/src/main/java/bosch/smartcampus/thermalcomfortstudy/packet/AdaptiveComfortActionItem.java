package bosch.smartcampus.thermalcomfortstudy.packet;

import org.jivesoftware.smackx.pubsub.Item;

import bosch.smartcampus.thermalcomfortstudy.lib.AdaptiveComfortAction;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 7/5/16.
 */
public class AdaptiveComfortActionItem extends Item {
    public static final String NAMESPACE = null;
    public static final String ELEMENT = "item";
    public static final String ITEM_ID_PREFIX = "_ActionData";

    public static final String TRANSDUCER_DATA_ID = "occthermcomf_bosch";
    public static final String TRANSDUCER_DATA_TYPE = "TCS";
    public static final String TRANSDUCER_DATA_CLASS = "ActionData";

    private static final String ADAPTIVE_COMFORT_ACTION_NAME = "AdaptiveComfortAction";

    private String username;
    private Timestamp timestamp;
    private AdaptiveComfortAction action;
    private String actionDescription;

    public AdaptiveComfortActionItem(String username, Timestamp timestamp,
                                     AdaptiveComfortAction action) {
        this(username, timestamp, action, "");
    }

    public AdaptiveComfortActionItem(String username, Timestamp timestamp,
                                     AdaptiveComfortAction action, String actionDescription) {
        this.username = username;
        this.timestamp = timestamp;
        this.action = action;
        this.actionDescription = actionDescription;
    }

    public String getUsername() {
        return username;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public AdaptiveComfortAction getAdaptiveComfortAction() {
        return action;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getId() {
        return ITEM_ID_PREFIX + timestamp;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<" + ELEMENT + " id=\"" + getId() + "\">");

        if (action == AdaptiveComfortAction.OTHER) {
            builder.append(getTransducerDataElement(ADAPTIVE_COMFORT_ACTION_NAME, actionDescription));
        } else {
            builder.append(getTransducerDataElement(ADAPTIVE_COMFORT_ACTION_NAME, action.toString()));
        }

        builder.append("</" + ELEMENT + ">");
        return builder.toString();
    }

    protected String getTransducerDataElement(String name, String value) {
        String element = String.format("<transducerData id=\"%s\" type=\"%s\" class=\"%s\" ",
                TRANSDUCER_DATA_ID, TRANSDUCER_DATA_TYPE, TRANSDUCER_DATA_CLASS);
        element += "username=\"" + username + "\" ";
        element += "timestamp=\"" + timestamp + "\" ";
        element += "name=\"" + name + "\" ";
        element += "value=\"" + value + "\"/>";
        return element;
    }
}
