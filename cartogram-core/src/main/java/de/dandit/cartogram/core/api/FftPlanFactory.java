package de.dandit.cartogram.core.api;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.stream.Collectors;

import de.dandit.cartogram.core.dft.DefaultFftPlanner;
import de.dandit.cartogram.core.dft.FftPlan2D;

public class FftPlanFactory {
  private final Fft2DPlanner planner;

  private FftPlanFactory(Fft2DPlanner planner) {
    this.planner = planner;
    validatePlannerPerformDCT2_2D(planner);
    validatePlannerPerformDCT3_2D(planner);
    validatePlannerPerformDCT3_DST3_2D(planner);
    validatePlannerPerformDST3_DCT3_2D(planner);
    validatePlannerPerformDCT3_DST3_2DNonSquare(planner);
    validatePlannerPerformDCT3_2DNonSquare(planner);
  }

  private static void validatePlannerPerformDST3_DCT3_2D(Fft2DPlanner planner) {
    double[] input = new double[] {1, 1, 1, 1};
    double[] output = new double[] {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    FftPlan2D plan = planner.createDST3_DCT3_2D(2, 2, input, output);
    plan.execute();
    boolean inputUnchanged = Arrays.equals(input, new double[] {1, 1, 1, 1});
    String baseFailure = "Given planner did not perform DST3_DCT3 as expected: ";
    if (!inputUnchanged) {
      throw new IllegalArgumentException(baseFailure + "Input was modified.");
    }
    if (plan.getOutputData() != output) {
      throw new IllegalArgumentException(baseFailure + "Output data array must be identical to the given output data array.");
    }
    double[] expectedOutput = {5.82842712474619, 1.0, -1.0, -0.17157287525380993};
    if (!arraysAlmostEqual(expectedOutput, output)) {
      throw new IllegalArgumentException(
        MessageFormat.format(baseFailure + "Expected output {0} but was {1}",
          arraysAsString(expectedOutput),
          arraysAsString(output)));
    }
  }

  private static void validatePlannerPerformDCT3_DST3_2D(Fft2DPlanner planner) {
    double[] input = new double[] {1, 1, 1, 1};
    double[] output = new double[] {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    FftPlan2D plan = planner.createDCT3_DST3_2D(2, 2, input, output);
    plan.execute();
    boolean inputUnchanged = Arrays.equals(input, new double[] {1, 1, 1, 1});
    String baseFailure = "Given planner did not perform DCT3_DST3 as expected: ";
    if (!inputUnchanged) {
      throw new IllegalArgumentException(baseFailure + "Input was modified.");
    }
    if (plan.getOutputData() != output) {
      throw new IllegalArgumentException(baseFailure + "Output data array must be identical to the given output data array.");
    }
    double[] expectedOutput = {5.82842712474619, -1.0, 1.0, -0.17157287525380993};
    if (!arraysAlmostEqual(expectedOutput, output)) {
      throw new IllegalArgumentException(
        MessageFormat.format(baseFailure + "Expected output {0} but was {1}",
          arraysAsString(expectedOutput),
          arraysAsString(output)));
    }
  }

  private static void validatePlannerPerformDCT3_DST3_2DNonSquare(Fft2DPlanner planner) {
    double[] input = new double[] {1, 1, 1, 1, 1, 1, 1, 1};
    double[] output = new double[] {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    FftPlan2D plan = planner.createDCT3_DST3_2D(2, 4, input, output);
    plan.execute();
    boolean inputUnchanged = Arrays.equals(input, new double[] {1, 1, 1, 1, 1, 1, 1, 1});
    String baseFailure = "Given planner did not perform DCT3_DST3_2D (NonSquare) as expected: ";
    if (!inputUnchanged) {
      throw new IllegalArgumentException(baseFailure + "Input was modified.");
    }
    if (plan.getOutputData() != output) {
      throw new IllegalArgumentException(baseFailure + "Output data array must be identical to the given output data array.");
    }
    double[] expectedOutput = {12.13707118454409, -3.6131259297527527, 1.6131259297527527, -0.48021693505171026, 2.0823922002923947, -0.619914404421775, 0.2767686539141552, -0.08239220029239402};
    if (!arraysAlmostEqual(expectedOutput, output)) {
      throw new IllegalArgumentException(
          MessageFormat.format(baseFailure + "Expected output {0} but was {1}",
              arraysAsString(expectedOutput),
              arraysAsString(output)));
    }
  }

  private static void validatePlannerPerformDCT2_2D(Fft2DPlanner planner) {
    double[] input = new double[] {1, 1, 1, 1};
    double[] output = new double[] {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    FftPlan2D plan = planner.createDCT2_2D(2, 2, input, output);
    plan.execute();
    boolean inputUnchanged = Arrays.equals(input, new double[] {1, 1, 1, 1});
    String baseFailure = "Given planner did not perform DCT2_2D as expected: ";
    if (!inputUnchanged) {
      throw new IllegalArgumentException(baseFailure + "Input was modified.");
    }
    if (plan.getOutputData() != output) {
      throw new IllegalArgumentException(baseFailure + "Output data array must be identical to the given output data array.");
    }
    double[] expectedOutput = {16, 0, 0, 0};
    if (!arraysAlmostEqual(expectedOutput, output)) {
      throw new IllegalArgumentException(
        MessageFormat.format(baseFailure + "Expected output {0} but was {1}",
          arraysAsString(expectedOutput),
          arraysAsString(output)));
    }
  }

  private static String arraysAsString(double[] output) {
    return Arrays.stream(output).mapToObj(Double::toString).collect(Collectors.joining(", ", "[", "]"));
  }

  private static void validatePlannerPerformDCT3_2DNonSquare(Fft2DPlanner planner) {
    double[] input = new double[] {1, 1};
    double[] output = new double[] {1, 1};
    FftPlan2D plan = planner.createDCT3_2D(1, 2, input, output);
    plan.execute();
    boolean inputUnchanged = Arrays.equals(input, new double[] {1, 1});
    String baseFailure = "Given planner did not perform DCT3_2D (NonSquare) as expected: ";
    if (!inputUnchanged) {
      throw new IllegalArgumentException(baseFailure + "Input was modified.");
    }
    if (plan.getOutputData() != output) {
      throw new IllegalArgumentException(baseFailure + "Output data array must be identical to the given output data array.");
    }
    double[] expectedOutput = {2.414213562373095, -0.41421356237309515};
    if (!arraysAlmostEqual(expectedOutput, output)) {
      throw new IllegalArgumentException(
          MessageFormat.format(baseFailure + "Expected output {0} but was {1}",
              arraysAsString(expectedOutput),
              arraysAsString(output)));
    }
  }

  private static void validatePlannerPerformDCT3_2D(Fft2DPlanner planner) {
    double[] input = new double[] {1, 1, 1, 1};
    double[] output = new double[] {1, 1, 1, 1};
    FftPlan2D plan = planner.createDCT3_2D(2, 2, input, output);
    plan.execute();
    boolean inputUnchanged = Arrays.equals(input, new double[] {1, 1, 1, 1});
    String baseFailure = "Given planner did not perform DCT3_2D as expected: ";
    if (!inputUnchanged) {
      throw new IllegalArgumentException(baseFailure + "Input was modified.");
    }
    if (plan.getOutputData() != output) {
      throw new IllegalArgumentException(baseFailure + "Output data array must be identical to the given output data array.");
    }
    double[] expectedOutput = {5.82842712474619, -1, -1, 0.17157287525380993};
    if (!arraysAlmostEqual(expectedOutput, output)) {
      throw new IllegalArgumentException(
        MessageFormat.format(baseFailure + "Expected output {0} but was {1}",
          arraysAsString(expectedOutput),
          arraysAsString(output)));
    }
  }

  private static boolean arraysAlmostEqual(double[] expected, double[] given) {
    if (expected.length != given.length) {
      return false;
    }
    for (int i = 0; i < expected.length; i++) {
      if (Math.abs(expected[i] - given[i]) > 1E-15) {
        return false;
      }
    }
    return true;
  }

  public static FftPlanFactory of(Fft2DPlanner planner) {
    return new FftPlanFactory(planner);
  }

  public static FftPlanFactory ofDefault(ParallelismConfig parallelismConfig) {
    return new FftPlanFactory(new DefaultFftPlanner(parallelismConfig));
  }

  public FftPlan2D createDCT2_2D(int width, int height, double[] inputData, double[] outputData) {
    validateIOLength(inputData, outputData, width, height);
    return planner.createDCT2_2D(width, height, inputData, outputData);
  }

  /* Corresponds to FFTW's REDFT01:
   * Up to a scale factor of N=2n this is the inverse of REDFT10, so this is also called IDCT.
   */
  public FftPlan2D createDCT3_2D(int width, int height) {
    double[] data = new double[width * height];
    return planner.createDCT3_2D(width, height, data, data);
  }

  /* Corresponds to FFTW's REDFT01:
   * Up to a scale factor of N=2n this is the inverse of REDFT10, so this is also called IDCT.
   */
  public FftPlan2D createDCT3_2D(int width, int height, double[] inputData, double[] outputData) {
    validateIOLength(inputData, outputData, width, height);
    return planner.createDCT3_2D(width, height, inputData, outputData);
  }

  /* Corresponds to FFTW's RODFT01 -> REDFT01:
   */
  public FftPlan2D createDCT3_DST3_2D(int width, int height) {
    double[] data = new double[width * height];
    return planner.createDCT3_DST3_2D(width, height, data, data);
  }

  /* Corresponds to FFTW's REDFT01 -> RODFT01:
   */
  public FftPlan2D createDST3_DCT3_2D(int width, int height) {
    double[] data = new double[width * height];
    return planner.createDST3_DCT3_2D(width, height, data, data);
  }

  private void validateIOLength(double[] inputData, double[] outputData, int width, int height) {
    if (inputData.length != width * height) {
      throw new IllegalArgumentException(
        MessageFormat.format("Size of input (length={0}) must correspond to width ({1}) * height ({2})",
          inputData.length, width, height));
    }
    if (inputData.length != outputData.length) {
      throw new IllegalArgumentException(
        MessageFormat.format("Input (length={0}) and output (length={1}) buffers must have identical length!",
          inputData.length, outputData.length));
    }
  }
}
