package bosch.smartcampus.thermalcomfortstudy.lib;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Adaptive comfort action.
 */
public enum AdaptiveComfortAction implements Parcelable {
    THERMOSTAT_WARMER,
    THERMOSTAT_COOLER,
    SPACE_HEATER_ON,
    SPACE_HEATER_OFF,
    FAN_ON,
    FAN_OFF,
    LAYER_ON,
    LAYER_OFF,
    OTHER;

    public static final Parcelable.Creator<AdaptiveComfortAction> CREATOR = new Creator<AdaptiveComfortAction>() {

        @Override
        public AdaptiveComfortAction[] newArray(int size) {
            return new AdaptiveComfortAction[size];
        }

        @Override
        public AdaptiveComfortAction createFromParcel(Parcel source) {
            return AdaptiveComfortAction.values()[source.readInt()];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.ordinal());
    }
}
