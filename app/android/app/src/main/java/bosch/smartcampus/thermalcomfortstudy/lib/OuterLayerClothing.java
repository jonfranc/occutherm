package bosch.smartcampus.thermalcomfortstudy.lib;

/**
 * Created by rsukkerd on 8/30/16.
 */
public enum OuterLayerClothing {
    F1 (0.13, "Vest (thin)", "Sleeveless vest (thin)"),
    F2 (0.22, "Vest (thick)", "Sleeveless vest (thick)"),
    F3 (0.25, "Sweater/Jacket (thin)", "Long-sleeve sweater (thin)"),
    F4 (0.36, "Sweater/Jacket (thick)", "Long-sleeve sweater (thick)"),
    G4 (0.44, "Coat/Suit jacket (thick)", "Single-breasted suit jacket (thick)");

    private final double clothingInsulation;
    private final String name;
    private final String description;

    OuterLayerClothing(double clothingInsulation, String name, String description) {
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
