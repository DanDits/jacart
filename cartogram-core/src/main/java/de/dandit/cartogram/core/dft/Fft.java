package de.dandit.cartogram.core.dft;

/*
 * Free FFT and convolution (Java)
 *
 * Copyright (c) 2017 Project Nayuki. (MIT License)
 * https://www.nayuki.io/page/free-small-fft-in-multiple-languages
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * - The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 * - The Software is provided "as is", without warranty of any kind, express or
 *   implied, including but not limited to the warranties of merchantability,
 *   fitness for a particular purpose and noninfringement. In no event shall the
 *   authors or copyright holders be liable for any claim, damages or other
 *   liability, whether in an action of contract, tort or otherwise, arising from,
 *   out of or in connection with the Software or the use or other dealings in the
 *   Software.
 */
public final class Fft {

  /*
   * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the vector.
   * The vector can have any length. This is a wrapper function.
   */
  public static void transform(double[] real, double[] imag, double[] cosTable, double[] sinTable) {
    int n = real.length;
    if (n != imag.length)
      throw new IllegalArgumentException("Mismatched lengths");
    if (n == 0) {
      return;
    }
    if ((n & (n - 1)) == 0) { // Is power of 2
      transformRadix2(real, imag, cosTable, sinTable);
    } else {
      throw new IllegalArgumentException("Input length needs to be multiple of 2 but was " + n);
    }
  }
  /*
   * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the vector.
   * The vector's length must be a power of 2. Uses the Cooley-Tukey decimation-in-time radix-2 algorithm.
   */
  public static void transformRadix2(double[] real, double[] imag, double[] cosTable, double[] sinTable) {
    // Length variables
    int n = real.length;
    if (n != imag.length)
      throw new IllegalArgumentException("Mismatched lengths");
    int levels = 31 - Integer.numberOfLeadingZeros(n);  // Equal to floor(log2(n))
    if (1 << levels != n)
      throw new IllegalArgumentException("Length is not a power of 2");

    // Bit-reversed addressing permutation
    for (int i = 0; i < n; i++) {
      int j = Integer.reverse(i) >>> (32 - levels);
      if (j > i) {
        double temp = real[i];
        real[i] = real[j];
        real[j] = temp;
        temp = imag[i];
        imag[i] = imag[j];
        imag[j] = temp;
      }
    }

    // Cooley-Tukey decimation-in-time radix-2 FFT
    for (int size = 2; size <= n; size *= 2) {
      int halfsize = size / 2;
      int tablestep = n / size;
      for (int i = 0; i < n; i += size) {
        for (int j = i, k = 0; j < i + halfsize; j++, k += tablestep) {
          int l = j + halfsize;
          double tpre = real[l] * cosTable[k] + imag[l] * sinTable[k];
          double tpim = -real[l] * sinTable[k] + imag[l] * cosTable[k];
          real[l] = real[j] - tpre;
          imag[l] = imag[j] - tpim;
          real[j] += tpre;
          imag[j] += tpim;
        }
      }
      if (size == n)  // Prevent overflow in 'size *= 2'
        break;
    }
  }
}