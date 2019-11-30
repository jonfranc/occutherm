package bosch.smartcampus.thermalcomfortstudy.lib;

/**
 * Created by rsukkerd on 6/14/16.
 */
public enum BottomClothing {
    D2 (0.08, "Shorts", "Walking shorts"),
    D3 (0.15, "Long pants (thin)", "Straight trousers (thin)"),
    D4 (0.24, "Long pants (thick)", "Straight trousers (thick)"),
    E1 (0.14, "Skirt (thin)", "Skirt (thin)"),
    E2 (0.23, "Skirt (thick)", "Skirt (thick)");

    private final double clothingInsulation;
    private final String name;
    private final String description;

    BottomClothing(double clothingInsulation, String name, String description) {
        this.clothingInsulation = clothingInsulation;
        this.name = name;
        this.description = description;
    }

    public double getClothingInsulation() {
        return clothingInsulation;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
