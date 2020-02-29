package dan.dit.cartogram.core.pub;

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
  private final boolean scaleToOriginalPolygonRegion;
  private final ParallelismConfig parallelismConfig;

  /**
   * Defines a threshold for the resulting cartogram areas: The maximum error of each region
   * must only differ by that percentage. The error hereby is defined by how much the cartogram region's relative area
   * differs from the region's relative target area: For example if region A should accumulate 20%
   * of the total area mass but currently accumulates 30%, the error would be 0.2/0.3-1=0.66-1=-0.33 which is 33%
   */
  private final double maxPermittedAreaError;

  public CartogramConfig(double maxPermittedAreaError, boolean usePerimeterThreshold, Logging logging, FftPlanFactory fftPlanFactory, boolean scaleToOriginalPolygonRegion, ParallelismConfig parallelismConfig) {
    this.maxPermittedAreaError = maxPermittedAreaError;
    this.usePerimeterThreshold = usePerimeterThreshold;
    this.logging = logging;
    this.fftPlanFactory = fftPlanFactory;
    this.scaleToOriginalPolygonRegion = scaleToOriginalPolygonRegion;
    this.parallelismConfig = parallelismConfig;
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

  public boolean isScaleToOriginalPolygonRegion() {
    return scaleToOriginalPolygonRegion;
  }

  public double getMaxPermittedAreaError() {
    return maxPermittedAreaError;
  }

  public ParallelismConfig getParallelismConfig() {
    return parallelismConfig;
  }
}
