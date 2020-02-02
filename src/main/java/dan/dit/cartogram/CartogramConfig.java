package dan.dit.cartogram;

/**
 * Describes configuration for the cartogram creation like
 * which algorithms to choose or how to tune them.
 */
public class CartogramConfig {

    /**
     * If true DiffIntegrate is used, else Integrate is used for the integration of motion
     */
    private final boolean diff;

    private final boolean eps;
    private final boolean inv;

    /**
     * If true then extremely small regions are scaled up
     */
    private final boolean usePerimeterThreshold;

    public CartogramConfig(boolean diff, boolean eps, boolean inv, boolean usePerimeterThreshold) {
        this.eps = eps;
        this.inv = inv;
        this.usePerimeterThreshold = usePerimeterThreshold;
        this.diff = diff;
    }

    public boolean isEps() {
        return eps;
    }

    public boolean isDiff() {
        return diff;
    }

    public boolean isInv() {
        return inv;
    }

    public boolean isUsePerimeterThreshold() {
        return usePerimeterThreshold;
    }
}
