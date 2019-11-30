package bosch.smartcampus.thermalcomfortstudy.lib;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rsukkerd on 6/22/16.
 */
public class BandDataAggregate {
    private static final String LOG_TAG = BandDataAggregate.class.getSimpleName();

    // Raw sensor reading data from band
    private List<Double> rawTemperatureData = new ArrayList<>();
    private List<Long> rawTotalCaloriesData = new ArrayList<>();
    private List<Integer> rawGsrData = new ArrayList<>();
    private List<Float> rawSkinTemperatureData = new ArrayList<>();
    private boolean mHasBandContact = false;

    // Average data
    private List<Double> averageTemperatureData = new ArrayList<>();
    private List<Double> averageCaloriesData = new ArrayList<>();
    private List<Double> averageGsrData = new ArrayList<>();
    private List<Double> averageSkinTemperatureData = new ArrayList<>();

    public BandDataAggregate() {
    }

    /**
     * Put current raw temperature
     * Unit: C
     * Frequency: 1 Hz
     * @pre: BandContactState.WORN
     */
    public void putTemperature(double temperature) {
        if (mHasBandContact) {
            rawTemperatureData.add(temperature);
        }
    }

    /**
     * Put total number of calories since last factory-reset
     * Unit: kCals
     * Frequency: 1 Hz
     * @pre: BandContactState.WORN
     */
    public void putTotalCalories(long totalCalories) {
        if (mHasBandContact) {
            rawTotalCaloriesData.add(totalCalories);
        }
    }

    /**
     * Put current GSR
     * Unit: kOhms
     * Frequency: 0.2 Hz
     * @pre: BandContactState.WORN
     */
    public void putGsr(int resistance) {
        if (mHasBandContact) {
            rawGsrData.add(resistance);
        }
    }

    /**
     * Put current skin temperature (as value changes)
     * Unit: C
     * Frequency: as value changes
     * @pre: BandContactState.WORN
     */
    public void putSkinTemperature(float skinTemperature) {
        if (mHasBandContact) {
            rawSkinTemperatureData.add(skinTemperature);
        }
    }

    public void setBandContactStatus(boolean hasBandContact) {
        mHasBandContact = hasBandContact;
    }

    public boolean hasRawTemperatureData() {
        return !rawTemperatureData.isEmpty();
    }

    public boolean hasRawTotalCaloriesData() {
        return rawTotalCaloriesData.size() >= 2;
    }

    public boolean hasRawGsrData() {
        return !rawGsrData.isEmpty();
    }

    public boolean hasRawSkinTemperatureData() {
        return !rawSkinTemperatureData.isEmpty();
    }

    public void averageTemperature() {
        if (hasRawTemperatureData()) {
            average(rawTemperatureData, averageTemperatureData);
        } else {
            Log.e(LOG_TAG, "No raw temperature data has been received");
        }
    }

    public void averageCalories() {
        if (hasRawTotalCaloriesData()) {
            List<Long> rawCaloriesData = new ArrayList<>();

            for (int i = 0; i < rawTotalCaloriesData.size() - 1; i++) {
                long cur = rawTotalCaloriesData.get(i);
                long next = rawTotalCaloriesData.get(i + 1);
                rawCaloriesData.add(next - cur);
            }

            average(rawCaloriesData, averageCaloriesData);
        } else {
            Log.e(LOG_TAG, "Less than 2 raw total calories data have been received");
        }
    }

    public void averageGsr() {
        if (hasRawGsrData()) {
            average(rawGsrData, averageGsrData);
        } else {
            Log.e(LOG_TAG, "No raw GSR data has been received");
        }
    }

    public void averageSkinTemperature() {
        if (hasRawSkinTemperatureData()) {
            // Skin temperature value is only received when value changes
            float lastRawSkinTemperature = rawSkinTemperatureData.get(rawSkinTemperatureData.size() - 1);
            average(rawSkinTemperatureData, averageSkinTemperatureData);
            rawSkinTemperatureData.add(lastRawSkinTemperature);
        } else {
            Log.e(LOG_TAG, "No raw skin temperature data has been received");
        }
    }

    /**
     * @pre averageTemperatureData.size() > 0
     */
    public double getLastAverageTemperature() {
        return averageTemperatureData.get(averageTemperatureData.size() - 1);
    }

    /**
     * @pre averageCaloriesData.size() > 0
     */
    public double getLastAverageCalories() {
        return averageCaloriesData.get(averageCaloriesData.size() - 1);
    }

    /**
     * @pre averageGsrData.size() > 0
     */
    public double getLastAverageGsr() {
        return averageGsrData.get(averageGsrData.size() - 1);
    }

    /**
     * @pre averageSkinTemperatureData.size() > 0
     */
    public double getLastAverageSkinTemperature() {
        return averageSkinTemperatureData.get(averageSkinTemperatureData.size() - 1);
    }

    private void average(List<? extends Number> rawData, List<Double> averageData) {
        assert  !rawData.isEmpty();
        double average = getAverage(rawData);
        averageData.add(average);
        rawData.clear();
    }

    private double getAverage(List<? extends Number> data) {
        assert !data.isEmpty();
        double sum = 0;
        for (Number temp : data) {
            sum += temp.doubleValue();
        }
        return sum / data.size();
    }
}
