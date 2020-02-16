package dan.dit.cartogram.main;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

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
      "sample8_geo.json, sample8_data.csv, result8.eps", // portrait mode, testing non square mode with lx!=ly, does not converge // TODO test 8+9 in gocart and see how the eps file looks like
      "sample8_geo.json, sample9_data.csv, result9.eps", // portrait mode, testing non square mode with lx!=ly
      // TODO regions with small areas
  })
  public void createCartogramAndWriteToEps(
      String geoJsonResource,
      String dataResource,
      String epsInResource) throws IOException {
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
    ExecuteCartogram.createCartogramToEps(
        ExecuteCartogramTest.class.getResourceAsStream(geoJsonResource),
        ExecuteCartogramTest.class.getResourceAsStream(dataResource),
        epsOut,
        nullOutput);
    String epsOutData = epsOut.toString();
    String epsInData = textBuilder.toString();
    assertEquals(epsInData, epsOutData);
  }
}