package de.dandit.cartogram.core.api;

/**
 * Describes configuration for the cartogram creation like
 * which algorithms to choose or how to tune them.
 */
public class CartogramConfig {

  /**
   * If true then extremely small regions are scaled up
   */
  private final boolean usePerimeterThreshold;

  /**
   * Allows different logging behaviors and offers an entry point to plug in an existing logging
   * framework without ourselves depending on a specific log4j version.
   */
  private final Logging logging;

  /**
   * Allows configuring a FftPlan factory to allow different implementations. At the moment
   * Fft is not the bottleneck of execution, so it's not required for performance tuning.
   */
  private final FftPlanFactory fftPlanFactory;

  /**
   * If true the resulting regions (the polygonal geometries that they are built of) are scaled
   * and transformed to best match the initial size and position. This is likely wanted when
   * using the result as actual geometries in a map. If the result is just used in a chart or image
   * then this scaling is unnecessary.
   */
  private final boolean scaleToOriginalPolygonRegion;

  /**
   * Allows tuning the parallelism: e.g. turning parallel streams on and off
   */
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
