package dan.dit.cartogram.core.context;

import dan.dit.cartogram.core.pub.FftPlanFactory;
import dan.dit.cartogram.core.pub.Logging;

/**
 * Describes configuration for the cartogram creation like
 * which algorithms to choose or how to tune them.
 */
public class CartogramConfig {

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
