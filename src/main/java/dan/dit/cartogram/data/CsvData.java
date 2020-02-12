package dan.dit.cartogram.data;

import java.util.List;

public class CsvData {
  private final List<String> names;
  private final List<Object[]> data;

  public CsvData(List<String> names, List<Object[]> data) {
    this.names = names;
    this.data = data;
  }

  public List<String> getNames() {
    return names;
  }

  public List<Object[]> getData() {
    return data;
  }
}
