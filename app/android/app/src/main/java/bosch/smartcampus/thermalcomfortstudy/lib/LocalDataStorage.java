package bosch.smartcampus.thermalcomfortstudy.lib;

import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jivesoftware.smackx.pubsub.Item;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bosch.smartcampus.thermalcomfortstudy.packet.ActivityDescriptionItem;
import bosch.smartcampus.thermalcomfortstudy.packet.ActivityItem;
import bosch.smartcampus.thermalcomfortstudy.packet.AdaptiveComfortActionItem;
import bosch.smartcampus.thermalcomfortstudy.packet.BottomClothingItem;
import bosch.smartcampus.thermalcomfortstudy.packet.ClothingInsulationItem;
import bosch.smartcampus.thermalcomfortstudy.packet.MinuteCaloriesItem;
import bosch.smartcampus.thermalcomfortstudy.packet.OuterLayerClothingItem;
import bosch.smartcampus.thermalcomfortstudy.packet.GsrItem;
import bosch.smartcampus.thermalcomfortstudy.packet.SkinTemperatureItem;
import bosch.smartcampus.thermalcomfortstudy.packet.TCSDataItem;
import bosch.smartcampus.thermalcomfortstudy.packet.TemperatureItem;
import bosch.smartcampus.thermalcomfortstudy.packet.ThermalComfortItem;
import bosch.smartcampus.thermalcomfortstudy.packet.TopClothingItem;

/**
 * {@link LocalDataStorage} writes data items to external storage of this device.
 * It creates a new data file for each day of data collection.
 * These data items will be sent to Sensor Andrew.
 */
public class LocalDataStorage {
    private static final String LOG_TAG = LocalDataStorage.class.getSimpleName();
    private static final String DATA_STORAGE_DIRNAME = "ThermalComfortStudyData";
    private static final String DATA_FILENAME = "tcsData%s.dat";

    private File mTCSDirectory;
    private File mTCSFile;
    private FileOutputStream mOutputStream;
    private PrintWriter mWriter;
    private FileInputStream mInputStream;
    private BufferedReader mReader;

    public LocalDataStorage(boolean openForWrite) throws FileNotFoundException {
        File root = Environment.getExternalStorageDirectory();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
        String filename = String.format(DATA_FILENAME, dateStr);
        mTCSDirectory = new File(root.getAbsolutePath() + "/" + DATA_STORAGE_DIRNAME);
        mTCSFile = new File(mTCSDirectory, filename);

        if (openForWrite) {
            openForWrite();
        } else {
            openForRead();
        }
    }

    private void openForWrite() throws FileNotFoundException {
        if (isExternalStorageWritable()) {
            if (!mTCSDirectory.exists()) {
                if (mTCSDirectory.mkdirs()) {
                    Log.i(LOG_TAG, "Created " + DATA_STORAGE_DIRNAME + " directory");
                } else {
                    Log.e(LOG_TAG, "Cannot create " + DATA_STORAGE_DIRNAME + " directory");
                    return;
                }
            }

            mOutputStream = new FileOutputStream(mTCSFile, true);
            mWriter = new PrintWriter(mOutputStream);
            Log.i(LOG_TAG, "Writing TCS data to local storage " + mTCSDirectory.getAbsolutePath() + "...");
        } else {
            Log.e(LOG_TAG, "External storage is not writable");
        }
    }

    private void openForRead() throws FileNotFoundException {
        if (isExternalStorageReadable()) {
            if (mTCSFile.exists()) {
                mInputStream = new FileInputStream(mTCSFile);
                InputStreamReader inputStreamReader = new InputStreamReader(mInputStream,
                        Charset.forName("UTF-8"));
                mReader = new BufferedReader(inputStreamReader);
                Log.i(LOG_TAG,"Reading TCS data from local storage " + mTCSDirectory.getAbsolutePath() + "...");
            } else {
                Log.e(LOG_TAG, "No TCS data for today yet");
            }
        } else {
            Log.e(LOG_TAG, "External storage is not readable");
        }
    }

    /**
     * Write band data to local storage
     */
    public void writeBandData(String username, Timestamp timestamp,
                              double temperature, double minuteCalories,
                              double gsr, double skinTemperature) {
        TemperatureItem temperatureItem = new TemperatureItem(username, timestamp, temperature);
        MinuteCaloriesItem minuteCaloriesItem = new MinuteCaloriesItem(username, timestamp, minuteCalories);
        GsrItem gsrItem = new GsrItem(username, timestamp, gsr);
        SkinTemperatureItem skinTemperatureItem = new SkinTemperatureItem(username, timestamp, skinTemperature);

        writeItem(temperatureItem);
        writeItem(minuteCaloriesItem);
        writeItem(gsrItem);
        writeItem(skinTemperatureItem);
    }

    /**
     * Write survey data to local storage
     */
    public void writeSurveyData(String username, Timestamp timestamp,
                                ThermalComfort thermalComfort,
                                TopClothing topClothing, @Nullable BottomClothing bottomClothing, @Nullable OuterLayerClothing outerLayerClothing,
                                double clothingInsulation,
                                Activity activity, String activityDescription) {
        ThermalComfortItem thermalComfortItem = new ThermalComfortItem(username, timestamp, thermalComfort);
        TopClothingItem topClothingItem = new TopClothingItem(username, timestamp, topClothing);
        BottomClothingItem bottomClothingItem = new BottomClothingItem(username, timestamp, bottomClothing);
        OuterLayerClothingItem outerLayerClothingItem = new OuterLayerClothingItem(username, timestamp, outerLayerClothing);
        ClothingInsulationItem clothingInsulationItem = new ClothingInsulationItem(username, timestamp, clothingInsulation);
        ActivityItem activityItem = new ActivityItem(username, timestamp, activity);
        ActivityDescriptionItem activityDescriptionItem = new ActivityDescriptionItem(username, timestamp, activityDescription);

        writeItem(thermalComfortItem);
        writeItem(topClothingItem);
        writeItem(bottomClothingItem);
        writeItem(outerLayerClothingItem);
        writeItem(clothingInsulationItem);
        writeItem(activityItem);
        writeItem(activityDescriptionItem);
    }

    /**
     * Write thermal comfort data to local storage
     */
    public void writeThermalComfortData(String username, Timestamp timestamp, ThermalComfort thermalComfort) {
        ThermalComfortItem thermalComfortItem = new ThermalComfortItem(username, timestamp, thermalComfort);

        writeItem(thermalComfortItem);
    }

    /**
     * Write action data to local storage
     */
    public void writeActionData(String username, Timestamp timestamp,
                                List<AdaptiveComfortAction> actions, String description) {
        for (AdaptiveComfortAction action : actions) {
            AdaptiveComfortActionItem item;
            if (action == AdaptiveComfortAction.OTHER) {
                item = new AdaptiveComfortActionItem(username, timestamp, action, description);
            } else {
                item = new AdaptiveComfortActionItem(username, timestamp, action);
            }
            writeItem(item);
        }
    }

    /**
     * Read data items from a given range of time from local storage -- (from, to]
     */
    public List<TCSDataItem> readDataItems(Timestamp fromTimestamp, Timestamp toTimestamp)
            throws IOException {
        List<TCSDataItem> dataItems = new ArrayList<>();
        String line;
        while ((line = mReader.readLine()) != null) {
            TCSDataItem item = parseTCSDataItem(line);

            if (item.getTimestamp().compareTo(fromTimestamp) > 0
                    && item.getTimestamp().compareTo(toTimestamp) <= 0) {
                dataItems.add(item);
            } else if (item.getTimestamp().compareTo(toTimestamp) > 0) {
                break;
            }
        }
        return dataItems;
    }

    public void close() throws IOException {
        if (mWriter != null) {
            mWriter.close();
        }

        if (mOutputStream != null) {
            mOutputStream.close();
        }

        if (mReader != null) {
            mReader.close();
        }

        if (mInputStream != null) {
            mInputStream.close();
        }
    }

    private void writeItem(Item item) {
        Log.i(LOG_TAG, "Wrote TCS data item to local storage: " + item.toXML());
        mWriter.println(item.toXML());
        mWriter.flush();
    }

    /**
     * Parse TCSDataItem from a line
     */
    private TCSDataItem parseTCSDataItem(String line) {
        Pattern idPattern = Pattern.compile("item id=\"([^\"]+)\"");
        Pattern timestampPattern = Pattern.compile("timestamp=\"([^\"]+)\"");
        Matcher idMatcher = idPattern.matcher(line);
        Matcher timestampMatcher = timestampPattern.matcher(line);
        boolean idFound = idMatcher.find();
        boolean timestampFound = timestampMatcher.find();
        assert idFound && timestampFound;
        String id = idMatcher.group(1);
        Timestamp timestamp = new Timestamp(timestampMatcher.group(1));
        String xml = line;
        TCSDataItem item = new TCSDataItem(id, timestamp, xml);
        return item;
    }

    /**
     * Checks if external storage is available for read and write
     */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * Checks if external storage is available to at least read
     */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
