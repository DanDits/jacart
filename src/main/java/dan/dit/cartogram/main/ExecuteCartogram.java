package dan.dit.cartogram.main;

import dan.dit.cartogram.core.ConvergenceGoalFailedException;
import dan.dit.cartogram.core.context.*;
import dan.dit.cartogram.core.context.Point;
import dan.dit.cartogram.core.pub.*;
import dan.dit.cartogram.data.CsvData;
import dan.dit.cartogram.data.CsvDataImport;
import dan.dit.cartogram.data.GeoJsonIO;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.*;
import java.util.*;

public class ExecuteCartogram {

  public static void main(String[] args) throws IOException, ConvergenceGoalFailedException {
    Point[][] examplePolygons = new Point[1][];
    examplePolygons[0] = new Point[]{new Point(-0.5, 1), new Point(0.5, 1), new Point(0.5, -1), new Point(-0.5, -1), new Point(-0.5, 1)};

    String base = "/home/dd/Cartogram/out/";
    List<ResultPolygon> polygons = new ArrayList<>();
    for (Point[] examplePolygon : examplePolygons) {
      polygons.add(new ResultPolygon(Arrays.asList(examplePolygon), List.of()));
    }

    FeatureConverter featureConverter = new FeatureConverter(new GeometryConverter(new GeometryFactory()));
    outputPolycornToFile(featureConverter, List.of(new ResultRegion(polygons, false)), new FileOutputStream(new File(base + "example.json")));

    InputStream geoJsonResource = ExecuteCartogram.class.getResourceAsStream("reordered_geo.json");
    if (args.length > 1 && args[0].equals("-s")) {
      FileOutputStream outpFile = new FileOutputStream(args[1]);
      new GeoJsonIO().reWriteDataInIdOrder(geoJsonResource, outpFile);
      return;
    }
    InputStream dataResource = ExecuteCartogram.class.getResourceAsStream("sample_usa_data.csv");
    FileOutputStream epsOut = new FileOutputStream(new File("/home/dd/Cartogram/jacart/src/main/resources/dan/dit/cartogram/main/image.eps"));

    FileOutputStream jsonOut = new FileOutputStream(new File(base + "transformed.json"));
    createCartogramToEps(createDefaultConfig(), geoJsonResource, dataResource, epsOut);
  }

  private static CartogramConfig createDefaultConfig() {
    return new CartogramConfig(
      0.01,
      true,
      Logging.ofStandardOutput(),
      FftPlanFactory.ofDefault(),
      false);
  }

  // TODO create a separate project which is the only one having dependencies on geotools to perform geotools IO
  public static void createCartogramToEps(CartogramConfig config, InputStream geoJsonResource, InputStream dataResource,
                                          OutputStream epsOut) throws IOException, ConvergenceGoalFailedException {
    FeatureConverter featureConverter = new FeatureConverter(new GeometryConverter(new GeometryFactory()));
    CartogramResult result = createMapFeatureData(config, featureConverter, geoJsonResource, dataResource);
    new EpsWriter().ps_figure(
      epsOut,
      result.getGridSizeX(),
      result.getGridSizeY(),
      result.getResultRegions(),
      result.getGridProjection(),
      true);
  }

  public static void createCartogramToGeoJson(CartogramConfig config, InputStream geoJsonResource, InputStream dataResource,
                                          OutputStream jsonOut) throws IOException, ConvergenceGoalFailedException {
    FeatureConverter featureConverter = new FeatureConverter(new GeometryConverter(new GeometryFactory()));
    CartogramResult result = createMapFeatureData(config, featureConverter, geoJsonResource, dataResource);
    outputPolycornToFile(featureConverter, result.getResultRegions(), jsonOut);
  }

  private static Double extractData(CsvData data, int regionIdColumnIndex, int regionDataColumnIndex, Integer id) {
    for (int i = 0; i < data.getData().size(); i++) {
      Object[] csvValues = data.getData().get(i);
      if (csvValues[regionIdColumnIndex].equals(id)) {
        if (csvValues[regionDataColumnIndex] == null) {
          return Double.NaN;
        }
        return (Double) csvValues[regionDataColumnIndex];
      }
    }
    throw new IllegalStateException("Did not find value for region " + id);
  }

  private static CartogramResult createMapFeatureData(CartogramConfig config, FeatureConverter featureConverter, InputStream geoJsonResource, InputStream dataResource) throws IOException, ConvergenceGoalFailedException {
    FeatureCollection<SimpleFeatureType, SimpleFeature> geo = new GeoJsonIO().importData(geoJsonResource);
    CsvData data = new CsvDataImport().importCsv(dataResource);
    ReferencedEnvelope bounds = geo.getBounds();
    int regionIdColumnIndex = data.getNames().indexOf("Region.Id");
    int regionDataColumnIndex = data.getNames().indexOf("Region.Data");
    List<Region> regions = featureConverter.createRegions(asIterable(geo),
      ExecuteCartogram::extractFeatureId,
      id -> extractData(data, regionIdColumnIndex, regionDataColumnIndex, id));
    double[] targetAreaPerRegion = new double[regions.size()];
    for (int i = 0; i < regions.size(); i++) {
      Region region = regions.get(i);
      targetAreaPerRegion[i] = region.getData();
    }
    MapFeatureData mapFeatureData = new MapFeatureData(
      bounds.getMinX(),
      bounds.getMinY(),
      bounds.getMaxX(),
      bounds.getMaxY(),
      regions,
      targetAreaPerRegion);
    return new CartogramApi().execute(mapFeatureData, config);
  }

  private static int extractFeatureId(SimpleFeature feature) {
    Object value = feature.getProperties("cartogram_id").iterator().next().getValue();
    return Integer.parseInt(value.toString());
  }

  private static Iterable<? extends SimpleFeature> asIterable(FeatureCollection<SimpleFeatureType, SimpleFeature> geo) {
    return (Iterable<SimpleFeature>) () -> {
      FeatureIterator<SimpleFeature> featureIterator = geo.features();
      return new Iterator<>() {
        @Override
        public boolean hasNext() {
          return featureIterator.hasNext();
        }

        @Override
        public SimpleFeature next() {
          return featureIterator.next();
        }
      };
    };
  }

  private static void outputPolycornToFile(FeatureConverter featureConverter, List<ResultRegion> polygons, OutputStream jsonOut) throws IOException {
    DefaultFeatureCollection resultAsGeo = featureConverter.convertToFeatureCollection(polygons);
    new GeoJsonIO().exportData(
      resultAsGeo,
      jsonOut);
  }
}
