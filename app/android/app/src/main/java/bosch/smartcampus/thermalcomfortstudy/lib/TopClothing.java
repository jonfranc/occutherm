package bosch.smartcampus.thermalcomfortstudy.lib;

/**
 * Created by rsukkerd on 6/14/16.
 */
public enum TopClothing {
    C1 (0.12, "Blouse, sleeveless", "Sleeveless, scoop-neck blouse"),
    C2 (0.17, "Short-sleeve shirt/T-shirt", "Short-sleeve knit sport shirt"),
    C4 (0.25, "Long-sleeve shirt", "Long-sleeve dress shirt"),
    C6 (0.34, "Sweater", "Long-sleeve sweatshirt"),
    E3 (0.23, "Dress, sleeveless, scoop-neck", "Sleeveless, scoop neck dress (thin)"),
    E5 (0.29, "Dress, short-sleeve", "Short-sleeve shirtdress (thin)"),
    E6 (0.33, "Dress, long-sleeve (thin)", "Long-sleeve shirtdress (thin)"),
    E7 (0.47, "Dress, long-sleeve (thick)", "Long-sleeve shirtdress (thick)");

    private final double clothingInsulation;
    private final String name;
    private final String description;

    TopClothing(double clothingInsulation, String name, String description) {
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
