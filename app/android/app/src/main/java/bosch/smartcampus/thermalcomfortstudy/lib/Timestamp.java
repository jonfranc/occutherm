package bosch.smartcampus.thermalcomfortstudy.lib;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by rsukkerd on 7/19/16.
 */
public class Timestamp implements Comparable<Timestamp> {
    private Date mDateTime;
    private String mTimestamp;

    public Timestamp() {
        mDateTime = Calendar.getInstance().getTime();
        mTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ").format(mDateTime);
    }

    public Timestamp(String timestamp) {
        mTimestamp = timestamp;

        try {
            mDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ").parse(timestamp);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Date getDateTime() {
        return mDateTime;
    }

    @Override
    public String toString() {
        return mTimestamp;
    }

    @Override
    public int compareTo(Timestamp another) {
        return mDateTime.compareTo(another.mDateTime);
    }
}
