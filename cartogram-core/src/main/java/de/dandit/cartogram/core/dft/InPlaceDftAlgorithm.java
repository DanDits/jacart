package de.dandit.cartogram.core.dft;

/**
 * Offers a executable in place algorithm that transforms input data using some specified
 * discrete fourier transformation variant.
 */
public interface InPlaceDftAlgorithm {

  /**
   * Executes the algorithm by transforming the input data in place.
   * @param data The input data that will be modified in the process to later hold the result.
   * @param cosTable The cosine table: cos(2 * PI * i / n) for i=0..data.length. Is precomputed and stored
   * for execution performance.
   * @param sinTable The sine table: sin(2 * PI * i / n) for i=0..data.length. See cosTable.
   */
  void execute(double[] data, double[] cosTable, double[] sinTable);
}
