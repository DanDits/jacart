package dan.dit.cartogram.core.pub;

import dan.dit.cartogram.dft.FftPlan2D;

public interface Fft2DPlanner {

  // TODO validate definition, especially after our adjustment with factors 2
  /* REDFT10:
   * real-even-discrete-fourier-transform  type2 DCT-II aka "the" fast discrete-cosine-transform (DCT)
   * Defined by Y_k = 2 SUM_{j=0}^{n-1}X_j\cos[\pi j(k+1/2)/n]
   */
  FftPlan2D createDCT2_2D(int width, int height, double[] inputData, double[] outputData);

  /* REDFT01:
   * real-even-discrete-fourier-transform type3 DCT-III aka a fast-cosine-transform
   * Defined by Y_k = X_0 + 2 SUM_{j=1}^{n-1}X_j\cos[\pi j(k+1/2)/n]
   * If n=1 then Y_0 = X_0.
   * Up to a scale factor of N=2n this is the inverse of REDFT10, so this is also called IDCT.
   */
  FftPlan2D createDCT3_2D(int width, int height, double[] input, double[] output);

  /*
   * RODFT01 (DST-III) -> REDFT01 (DCT-III)
   */
  FftPlan2D createDCT3_DST3_2D(int width, int height, double[] input, double[] output);

  /*
   * REDFT01 (DCT-III) -> RODFT01 (DST-III)
   */
  FftPlan2D createDST3_DCT3_2D(int width, int height, double[] input, double[] output);
}
