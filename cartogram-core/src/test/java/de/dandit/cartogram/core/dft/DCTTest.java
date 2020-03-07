package de.dandit.cartogram.core.dft;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DCTTest {

  @Test
  public void dct2() {
    double[] input = new double[] {1, 1};

    DCT.transform(input, cosTable(2), sinTable(2));

    assertArrayEquals(new double[] {4,0}, input);
  }

  @Test
  public void dct3() {
    double[] input = new double[] {1, 1};

    DCT.inverseTransform(input, cosTable(2), sinTable(2));

    assertArrayEquals(new double[] {
        (0.5 + Math.cos(Math.PI / 4)) * 2,
        (0.5 + Math.cos(Math.PI / 4 * 3)) * 2},
        input,
        1E-14);
  }

  @Test
  public void dct3IsInverseOfDct2UpToFactor2n() {
    double[] input = new double[] {1, 1};

    DCT.transform(input, cosTable(2), sinTable(2));
    DCT.inverseTransform(input, cosTable(2), sinTable(2));

    assertArrayEquals(new double[] {4, 4}, input);
  }

  private static double[] cosTable(int n) {
    double[] cosTable = new double[n / 2];
    for (int i = 0; i < n / 2; i++) {
      cosTable[i] = Math.cos(2 * Math.PI * i / n);
    }
    return cosTable;
  }

  private static double[] sinTable(int n) {
    double[] sinTable = new double[n / 2];
    for (int i = 0; i < n / 2; i++) {
      sinTable[i] = Math.sin(2 * Math.PI * i / n);
    }
    return sinTable;
  }
}