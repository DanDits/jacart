package dan.dit.cartogram.main;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ExecuteCartogramTest {

    @DisplayName("Creating cartogram")
    @ParameterizedTest(name = "processing \"{0}\" with \"{1}\" must match \"{2}\"")
    @CsvSource({
            "sample1_geo.json, sample1_data.csv, result1.eps"})
    public void createCartogramAndWriteToEps(String geoJsonResource, String dataResource, String epsInResource) throws IOException {
        var epsOut = new ByteArrayOutputStream();
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (ExecuteCartogramTest.class.getResourceAsStream(epsInResource),
                        Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        ExecuteCartogram.createCartogramToEps(
                ExecuteCartogramTest.class.getResourceAsStream(geoJsonResource),
                ExecuteCartogramTest.class.getResourceAsStream(dataResource),
                epsOut);
        String epsOutData = epsOut.toString();
        String epsInData = textBuilder.toString();
        assertEquals(epsInData, epsOutData);
    }
}