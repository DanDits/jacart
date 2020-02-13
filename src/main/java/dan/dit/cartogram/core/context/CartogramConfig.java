package dan.dit.cartogram.core.context;

import dan.dit.cartogram.core.pub.FftPlanFactory;
import dan.dit.cartogram.core.pub.Logging;

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
  private final Logging logging;
  private final FftPlanFactory fftPlanFactory;

  public CartogramConfig(boolean diff, boolean usePerimeterThreshold, Logging logging, FftPlanFactory fftPlanFactory) {
    this.usePerimeterThreshold = usePerimeterThreshold;
    this.diff = diff;
    this.logging = logging;
    this.fftPlanFactory = fftPlanFactory;
  }

  public boolean isDiff() {
    return diff;
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
