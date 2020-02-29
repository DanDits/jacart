package de.dandit.cartogram.dft;

import de.dandit.cartogram.core.pub.Fft2DPlanner;
import de.dandit.cartogram.core.pub.Logging;
import de.dandit.cartogram.core.pub.ParallelismConfig;

import java.util.Arrays;

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

  public static void main(String[] args) {
    Logging logging = Logging.ofStandardOutput();
    logging.debug("DCT3_2D:");

    ParallelismConfig parallelismConfig = ParallelismConfig.ofCommonPool();
    for (int i = 2; i <= 1024; i *= 2) {
      double[] test = new double[i * i];
      double[] target = new double[i * i];
      Arrays.fill(test, 1.);
      logging.displayDoubleArray(i + " test (before)", test);
      var plan_bwd = new DefaultFftPlanner(parallelismConfig).createDCT3_2D(i, i, test, target);
      plan_bwd.execute();
      logging.displayDoubleArray( i + " target (after)", target);
      logging.debug("");
    }


    logging.debug("createDCT3_DST3_2D:");

    int i = 2;
    for (int j = 0; j < i * i; j++) {
      double[] test = new double[i * i];
      double[] target = new double[i * i];
      test[j] = 2;
      logging.displayDoubleArray(i + " test (before)", test);
      var plan_bwd = new DefaultFftPlanner(parallelismConfig).createDCT3_DST3_2D(i, i, test, target);
      plan_bwd.execute();
      logging.displayDoubleArray(i + " target (after)", target);
      logging.debug("");
    }

    logging.debug("createDST3_DCT3_2D:");

    i = 2;
    for (int j = 0; j < i * i; j++) {
      double[] test = new double[i * i];
      double[] target = new double[i * i];
      test[j] = 2;
      logging.displayDoubleArray(i + " test (before)", test);
      var plan_bwd = new DefaultFftPlanner(parallelismConfig).createDST3_DCT3_2D(i, i, test, target);
      plan_bwd.execute();
      logging.displayDoubleArray(i + " target (after)", target);
      logging.debug("");
    }

    logging.debug("DST-III");

    i = 2;
    for (int j = 0; j < i * i; j++) {
      double[] test = new double[i * i];
      test[j] = 2;
      logging.displayDoubleArray(i + " test (before)", test);
      DST.inverseTransform(test, initCosTable(test.length), initSinTable(test.length));
      logging.displayDoubleArray(i + " target (after)", test);
      logging.debug("");
    }
  }

  private static double[] initCosTable(int n) {
    double[] cosTable = new double[n / 2];
    for (int i = 0; i < n / 2; i++) {
      cosTable[i] = Math.cos(2 * Math.PI * i / n);
    }
    return cosTable;
  }

  private static double[] initSinTable(int n) {
    double[] sinTable = new double[n / 2];
    for (int i = 0; i < n / 2; i++) {
      sinTable[i] = Math.sin(2 * Math.PI * i / n);
    }
    return sinTable;
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
