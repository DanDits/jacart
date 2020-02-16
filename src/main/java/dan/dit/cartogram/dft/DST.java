package dan.dit.cartogram.dft;

/**
 * Allows calculation of the discrete sine transformation utilizing the fact that
 * it can be calculated using the discrete cosine transformation with O(n) pre and post processing.
 */
public class DST {

  public static void inverseTransform(double[] vector, double[] cosTable, double[] sinTable) {
    int len = vector.length;
    // revert order of input
    for (int i = 0; i < len / 2; i++) {
      double temp = vector[i];
      vector[i] = vector[len - i - 1];
      vector[len - i - 1] = temp;
    }
    DCT.inverseTransform(vector, cosTable, sinTable);
    // flip sign of every other output
    for (int i = 1; i < len; i+=2) {
      vector[i] *= -1;
    }
  }
}
