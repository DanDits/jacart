package dan.dit.cartogram.dft;

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
public class FftPlanFactory {

    /* REDFT10:
     * real-even-discrete-fourier-transform  type2 DCT-II aka "the" fast discrete-cosine-transform (DCT)
     * Defined by Y_k = 2 SUM_{j=0}^{n-1}X_j\cos[\pi j(k+1/2)/n]
     */
    public FftPlan2D createDCT2_2D(int width, int height, double[] data) {
        // TODO make sure the sin/cos table are re-used and cached (see Fft.java)
        return new FftPlan2D(width, height, data, data, DCT::transform);
    }

    public FftPlan2D createDCT2_2D(int width, int height, double[] inputData, double[] outputData) {
        if (inputData.length != outputData.length) {
            throw new IllegalArgumentException("Input and output buffers must have identical length");
        }
        System.arraycopy(inputData, 0, outputData, 0, inputData.length);
        return new FftPlan2D(width, height, inputData, outputData, DCT::transform);
    }

    /* REDFT01:
     * real-even-discrete-fourier-transform type3 DCT-III aka a fast-cosine-transform
     * Defined by Y_k = X_0 + 2 SUM_{j=1}^{n-1}X_j\cos[\pi j(k+1/2)/n]
     * If n=1 then Y_0 = X_0.
     * Up to a scale factor of N=2n this is the inverse of REDFT10, so this is also called IDCT.
     */
    public FftPlan2D createDCT3_2D(int width, int height) {
        double[] data = new double[width * height];
        return new FftPlan2D(width, height, data, data, DCT::inverseTransform);
    }

    public FftPlan2D createDCT3_2D(int width, int height, double[] inputData, double[] outputData) {
        return new FftPlan2D(width, height, inputData, outputData, DCT::inverseTransform);
    }
}
