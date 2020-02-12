package dan.dit.cartogram;

/**
 * Describes configuration for the cartogram creation like
 * which algorithms to choose or how to tune them.
 */
public class CartogramConfig {

  /**
   * If true DiffIntegrate is used,
   * else Integrate (faster flow based approach) is used for the integration of motion
   */
  private final boolean diff;

  /**
   * If true then extremely small regions are scaled up
   */
  private final boolean usePerimeterThreshold;

  public CartogramConfig(boolean diff, boolean usePerimeterThreshold) {
    this.usePerimeterThreshold = usePerimeterThreshold;
    this.diff = diff;
  }

  public boolean isDiff() {
    return diff;
  }

  public boolean isUsePerimeterThreshold() {
    return usePerimeterThreshold;
  }
}
