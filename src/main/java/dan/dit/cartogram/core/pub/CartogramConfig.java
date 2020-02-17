package dan.dit.cartogram.core.pub;

/**
 * Describes configuration for the cartogram creation like
 * which algorithms to choose or how to tune them.
 */
public class CartogramConfig {

  // TODO allow configuring the parallelism, maybe just by specifying a ForkJoinPool? Or just by disabling it?
  /**
   * If true then extremely small regions are scaled up
   */
  private final boolean usePerimeterThreshold;
  private final Logging logging;
  private final FftPlanFactory fftPlanFactory;

  public CartogramConfig(boolean usePerimeterThreshold, Logging logging, FftPlanFactory fftPlanFactory) {
    this.usePerimeterThreshold = usePerimeterThreshold;
    this.logging = logging;
    this.fftPlanFactory = fftPlanFactory;
  }

  public boolean isUsePerimeterThreshold() {
    return usePerimeterThreshold;
  }

  public Logging getLogging() {
    return logging;
  }

  public FftPlanFactory getFftPlanFactory() {
    return fftPlanFactory;
  }
}
