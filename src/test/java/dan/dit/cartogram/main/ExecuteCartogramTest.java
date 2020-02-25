package dan.dit.cartogram.main;

import dan.dit.cartogram.core.ConvergenceGoalFailedException;
import dan.dit.cartogram.core.pub.CartogramConfig;
import dan.dit.cartogram.core.pub.FftPlanFactory;
import dan.dit.cartogram.core.pub.Logging;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExecuteCartogramTest {

  @DisplayName("Creating cartogram to EPS")
  @ParameterizedTest(name = "processing \"{0}\" with \"{1}\" must match \"{2}\"")
  @CsvSource({
      "sample1_geo.json, sample1_data.csv, result1.eps", // USA: texas big and california big
      "sample1_geo.json, sample2_data.csv, result2.eps", // USA: texas big and california 0
      "sample3_geo.json, sample3_data.csv, result3.eps", // three regions non shared corners
      "sample4_geo.json, sample3_data.csv, result4.eps", // three regions wild transformations
      "sample5_geo.json, sample5_data.csv, result5.eps", // single region: no transformation
      "sample6_geo.json, sample6_data.csv, result6.eps", // testing handling of two missing values
      "sample6_geo.json, sample7_data.csv, result7.eps", // testing handling of single missing value
      "sample8_geo.json, sample9_data.csv, result9.eps", // portrait mode, testing non square mode with lx!=ly
      "sample10_geo.json, sample10_data.csv, result10.eps", // regions with very small areas that should be enhanced
      "sample11_geo.json, sample11_data.csv, result11.eps", // multipolygon with holes
      "sample11_geo.json, sample12_data.csv, result12.eps", // multipolygon with holes
  })
  public void createCartogramAndWriteToEps(
      String geoJsonResource,
      String dataResource,
      String epsInResource) throws IOException, ConvergenceGoalFailedException {
    var epsOut = new ByteArrayOutputStream();
    var nullOutput = new ByteArrayOutputStream();
    StringBuilder textBuilder = new StringBuilder();
    try (Reader reader = new BufferedReader(new InputStreamReader(
        ExecuteCartogramTest.class.getResourceAsStream(epsInResource),
        Charset.forName(StandardCharsets.UTF_8.name())))) {
      int c;
      while ((c = reader.read()) != -1) {
        textBuilder.append((char) c);
      }
    }
    CartogramConfig config = new CartogramConfig(
      0.01,
      true,
      Logging.ofStandardOutput(),
      FftPlanFactory.ofDefault(),
      false);
    ExecuteCartogram.createCartogramToEps(
      config, ExecuteCartogramTest.class.getResourceAsStream(geoJsonResource),
        ExecuteCartogramTest.class.getResourceAsStream(dataResource),
        epsOut,
        nullOutput);
    String epsOutData = epsOut.toString();
    String epsInData = textBuilder.toString();
    assertEquals(epsInData, epsOutData);
  }


  @DisplayName("Divergence creating cartogram to EPS")
  @ParameterizedTest(name = "processing \"{0}\" with \"{1}\" is expect to not converge")
  @CsvSource({
    "sample8_geo.json, sample8_data.csv", // portrait mode, testing non square mode with lx!=ly, does not converge // TODO test 8+9 in gocart and see how the eps file looks like
  })
  public void creatingCartogramFailsConvergenceGoal(
    String geoJsonResource,
    String dataResource) {
    var epsOut = new ByteArrayOutputStream();
    var nullOutput = new ByteArrayOutputStream();
    CartogramConfig config = new CartogramConfig(
      0.01,
      true,
      Logging.ofStandardOutput(),
      FftPlanFactory.ofDefault(),
      false);
    Assertions.assertThrows(ConvergenceGoalFailedException.class,
      () -> ExecuteCartogram.createCartogramToEps(
        config, ExecuteCartogramTest.class.getResourceAsStream(geoJsonResource),
        ExecuteCartogramTest.class.getResourceAsStream(dataResource),
        epsOut,
        nullOutput));
  }
}