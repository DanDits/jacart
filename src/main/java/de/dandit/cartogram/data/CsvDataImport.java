package de.dandit.cartogram.data;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CsvDataImport {

  public CsvData importCsv(InputStream dataResource) throws IOException {
    try (var reader = new BufferedReader(new InputStreamReader(dataResource))) {
      String line;
      boolean isHeader = true;
      List<String> headerNames = null;
      List<String> headerTypes = null;
      List<Object[]> data = new ArrayList<>();
      while ((line = reader.readLine()) != null) {
        String[] splitData = line.split(",");
        if (isHeader) {
          headerNames = Arrays.stream(splitData)
            .map(nameAndType -> nameAndType.split(":")[0])
            .collect(Collectors.toList());
          headerTypes = Arrays.stream(splitData)
            .map(nameAndType -> nameAndType.split(":")[1])
            .collect(Collectors.toList());
          isHeader = false;
        } else {
          Object[] currData = new Object[splitData.length];
          if (currData.length != headerNames.size()) {
            throw new IllegalArgumentException("Line data size does not match number of header columns");
          }
          for (int i = 0; i < splitData.length; i++) {
            currData[i] = convertValueToType(headerTypes.get(i), splitData[i]);
          }
          data.add(currData);
        }
      }
      return new CsvData(headerNames, data);
    }
  }

  private Object convertValueToType(String type, String readData) {
    if (readData == null || readData.length() == 0) {
      return null;
    }
    switch (type) {
      case "Integer":
        return Integer.parseInt(readData);
      case "Double":
        return Double.parseDouble(readData);
      default:
      case "String":
        return readData;
    }
  }
}
