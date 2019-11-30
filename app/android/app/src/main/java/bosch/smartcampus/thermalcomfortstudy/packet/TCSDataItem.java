package bosch.smartcampus.thermalcomfortstudy.packet;

import org.jivesoftware.smackx.pubsub.Item;

import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * Created by rsukkerd on 7/20/16.
 */
public class TCSDataItem extends Item {
    public static final String NAMESPACE = null;
    public static final String ELEMENT = "item";

    private String mItemId;
    private Timestamp mTimestamp;
    private String mItemXml;

    public TCSDataItem(String itemId, Timestamp timestamp, String itemXml) {
        mItemId = itemId;
        mItemXml = itemXml;
        mTimestamp = timestamp;
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
        return mItemId;
    }

    @Override
    public String toXML() {
        return mItemXml;
    }

    public Timestamp getTimestamp() {
        return mTimestamp;
    }
}
