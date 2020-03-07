package de.dandit.cartogram.core.dft;

import de.dandit.cartogram.core.api.Fft2DPlanner;
import de.dandit.cartogram.core.api.ParallelismConfig;

/**
 * For computing FFTs we have quite a range of possibilities:
 * A) https://sites.google.com/site/piotrwendykier/software/jtransforms
 * B) https://commons.apache.org/proper/commons-math/javadocs/api-3.4/org/apache/commons/math3/transform/FastFourierTransformer.html:
 * Problem: doesn't offer DCT types II or II. No optimized multidimensional implementations. Simple API but seems not perfectly optimized.
 * C) FFTW (with JNI) - used by gastner in C. Comes with optimized multidimensional implementations and offers
 * all types of DFTs, but requires C code. Claims to be fastest fourier transform in the west...
 * D) https://www.nayuki.io/page/free-small-fft-in-multiple-languages
 * Offers DCT in types II and III but no optimized multidimensional implementation.
 * <p>
 * Multidimensional support can be added simply by applying the DFT along each dimension: If there are two dimensions
 * of size lx and ly then we perform lx DFTs along the rows and ly DFTs along the columns (or first along columns and
 * then along rows). Note: This is not the most optimal solution!
 */
public class DefaultFftPlanner implements Fft2DPlanner {

  private final ParallelismConfig parallelismConfig;

  public DefaultFftPlanner(ParallelismConfig parallelismConfig) {
    this.parallelismConfig = parallelismConfig;
  }

  @Override
  public FftPlan2D createDCT2_2D(int width, int height, double[] inputData, double[] outputData) {
    return new FftPlan2D(parallelismConfig, width, height, inputData, outputData, DCT::transform, DCT::transform);
  }

  @Override
  public FftPlan2D createDCT3_2D(int width, int height, double[] inputData, double[] outputData) {
    return new FftPlan2D(parallelismConfig, width, height, inputData, outputData, DCT::inverseTransform, DCT::inverseTransform);
  }

  @Override
  public FftPlan2D createDCT3_DST3_2D(int width, int height, double[] inputData, double[] outputData) {
    return new FftPlan2D(parallelismConfig, width, height, inputData, outputData, DCT::inverseTransform, DST::inverseTransform);
  }

  @Override
  public FftPlan2D createDST3_DCT3_2D(int width, int height, double[] inputData, double[] outputData) {
    return new FftPlan2D(parallelismConfig, width, height, inputData, outputData, DST::inverseTransform, DCT::inverseTransform);
  }
}
