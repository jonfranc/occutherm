package bosch.smartcampus.thermalcomfortstudy.packet;

import org.jivesoftware.smackx.pubsub.Item;

import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 6/14/16.
 */
public class SurveyDataItem extends Item {
    public static final String NAMESPACE = null;
    public static final String ELEMENT = "item";
    public static final String ITEM_ID_PREFIX = "_SurveyData";

    public static final String TRANSDUCER_DATA_ID = "occthermcomf_bosch";
    public static final String TRANSDUCER_DATA_TYPE = "TCS";
    public static final String TRANSDUCER_DATA_CLASS = "SurveyData";

    private String username;
    private Timestamp timestamp;

    public SurveyDataItem(String username, Timestamp timestamp) {
        this.username = username;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public Timestamp getTimestamp() {
        return timestamp;
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
