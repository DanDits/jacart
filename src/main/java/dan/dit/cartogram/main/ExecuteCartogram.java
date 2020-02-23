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
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.*;
import java.util.*;
import java.util.function.Function;

import static org.locationtech.jts.geom.PrecisionModel.FLOATING;

public class ExecuteCartogram {

  public static void main(String[] args) throws IOException, ConvergenceGoalFailedException {
    Point[][] examplePolygons = new Point[1][];
    examplePolygons[0] = new Point[]{new Point(-0.5, 1), new Point(0.5, 1), new Point(0.5, -1), new Point(-0.5, -1), new Point(-0.5, 1)};

    String base = "/home/dd/Cartogram/out/";
    List<ResultPolygon> polygons = new ArrayList<>();
    for (Point[] examplePolygon : examplePolygons) {
      polygons.add(new ResultPolygon(Arrays.asList(examplePolygon), List.of()));
    }

    outputPolycornToFile(List.of(new ResultRegion(polygons, false)), new FileOutputStream(new File(base + "example.json")));

    InputStream geoJsonResource = ExecuteCartogram.class.getResourceAsStream("reordered_geo.json");
    if (args.length > 1 && args[0].equals("-s")) {
      FileOutputStream outpFile = new FileOutputStream(args[1]);
      new GeoJsonIO().reWriteDataInIdOrder(geoJsonResource, outpFile);
      return;
    }
    InputStream dataResource = ExecuteCartogram.class.getResourceAsStream("sample_usa_data.csv");
    FileOutputStream epsOut = new FileOutputStream(new File("/home/dd/Cartogram/jacart/src/main/resources/dan/dit/cartogram/main/image.eps"));

    FileOutputStream jsonOut = new FileOutputStream(new File(base + "transformed.json"));
    createCartogramToEps(geoJsonResource, dataResource, epsOut, jsonOut);
  }

  // TODO create a separate project which is the only one having dependencies on geotools to perform geotools IO
  public static void createCartogramToEps(InputStream geoJsonResource, InputStream dataResource,
      OutputStream epsOut,
      OutputStream jsonOut) throws IOException, ConvergenceGoalFailedException {
    FeatureCollection<SimpleFeatureType, SimpleFeature> geo = new GeoJsonIO().importData(geoJsonResource);
    CsvData data = new CsvDataImport().importCsv(dataResource);
    ReferencedEnvelope bounds = geo.getBounds();
    int regionIdColumnIndex = data.getNames().indexOf("Region.Id");
    int regionDataColumnIndex = data.getNames().indexOf("Region.Data");
    List<Region> regions = createRegions(geo, id -> {
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
    });
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
    CartogramConfig config = new CartogramConfig(
      // TODO make max area error configurable
      true,
      Logging.ofStandardOutput(),
      FftPlanFactory.ofDefault());
    CartogramResult result = new CartogramApi().execute(mapFeatureData, config);
    new EpsWriter().ps_figure(
      epsOut,
      result.getGridSizeX(),
      result.getGridSizeY(),
      result.getResultRegions(),
      result.getGridProjection(),
      true);
    outputPolycornToFile(result.getResultRegions(), jsonOut);
  }

  private static void outputPolycornToFile(List<ResultRegion> polygons, OutputStream jsonOut) throws IOException {
    DefaultFeatureCollection resultAsGeo = new DefaultFeatureCollection();
    int dummy_id = 0;
    for (ResultRegion region : polygons) {
      SimpleFeature feature = createFeature(dummy_id, region.getPolygons());
      resultAsGeo.add(feature);
      dummy_id++;
    }
    new GeoJsonIO().exportData(
      resultAsGeo,
      jsonOut);
  }

  private static SimpleFeature createFeature(int id, List<ResultPolygon> resultPolygons) {
    SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
    b.setName("CartPoly");
    // TODO output in input CRS and make sure that they are transformed back from Lspace to normal space!
    b.setCRS(DefaultGeographicCRS.WGS84); // set crs first
    b.add("geo", Polygon.class); // then add geometry

    final SimpleFeatureType simpleFeatureType = b.buildFeatureType();
    if (resultPolygons.size() == 0) {
      return null;
    } else if (resultPolygons.size() == 1) {
      ResultPolygon polygon = resultPolygons.get(0);
      return new SimpleFeatureBuilder(simpleFeatureType)
        .buildFeature(Integer.toString(id), new Object[]{asPolygon(polygon.getExteriorRing(), polygon.getInteriorRings())});
    } else {
      Polygon[] polygons = new Polygon[resultPolygons.size()];
      for (int i = 0; i < resultPolygons.size(); i++) {
        ResultPolygon p = resultPolygons.get(i);
        Polygon polygon = asPolygon(p.getExteriorRing(), p.getInteriorRings());
        polygons[i] = polygon;
      }
      return new SimpleFeatureBuilder(simpleFeatureType)
        .buildFeature(Integer.toString(id), new Object[]{
          new MultiPolygon(polygons, new PrecisionModel(FLOATING), 0)});
    }
  }

  private static Polygon asPolygon(List<Point> points, List<List<Point>> holes) {
    PrecisionModel precision = new PrecisionModel(FLOATING);
    int srid = 0;
    LinearRing outerRing = asRing(points, precision, srid);
    LinearRing[] jtsHoles = new LinearRing[holes.size()];
    for (int i = 0; i < holes.size(); i++) {
      jtsHoles[i] = asRing(holes.get(i), precision, srid);
    }
    return new Polygon(outerRing,
      jtsHoles,
      precision,
      srid);
  }

  private static LinearRing asRing(List<Point> points, PrecisionModel precision, int srid) {
    Coordinate[] coords = new Coordinate[points.size()];
    for (int i = 0; i < points.size(); i++) {
      Point p = points.get(i);
      coords[i] = new Coordinate(p.x, p.y);
    }
    return new LinearRing(coords, precision, srid);
  }


  private static List<Region> createRegions(FeatureCollection<SimpleFeatureType, SimpleFeature> geo,
                                            Function<Integer, Double> valueProvider) {
    FeatureIterator<SimpleFeature> iterator = geo.features();
    List<Region> regions = new ArrayList<>();
    while (iterator.hasNext()) {
      SimpleFeature feature = iterator.next();
      Object value = feature.getProperties("cartogram_id").iterator().next().getValue();
      int regionId = Integer.parseInt(value.toString());
      Object geometry = feature.getDefaultGeometry();
      if (geometry instanceof Polygon) {
        Polygon polygon = (Polygon) geometry;
        polygon.normalize();
        Point[][] points = new Point[1 + polygon.getNumInteriorRing()][];
        points[0] = convertCoordinates(polygon.getExteriorRing());
        List<Integer> ringsInPolygons = new ArrayList<>();
        ringsInPolygons.add(-1);
        for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
          points[1 + j] = convertCoordinates(polygon.getInteriorRingN(j));
          ringsInPolygons.add(0);
        }
        regions.add(new Region(regionId,
          valueProvider.apply(regionId),
          points,
          ringsInPolygons.stream().mapToInt(a -> a).toArray()));
      } else if (geometry instanceof MultiPolygon) {
        MultiPolygon multiPolygon = (MultiPolygon) geometry;
        multiPolygon.normalize();
        List<Point[]> points = new ArrayList<>();
        List<Integer> ringsInPolygons = new ArrayList<>();
        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
          Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
          points.add(convertCoordinates(polygon.getExteriorRing()));
          ringsInPolygons.add(-i - 1);
          for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
            points.add(convertCoordinates(polygon.getInteriorRingN(j)));
            ringsInPolygons.add(i);
          }
        }
        regions.add(new Region(regionId,
          valueProvider.apply(regionId),
          points.toArray(Point[][]::new),
          ringsInPolygons.stream().mapToInt(a -> a).toArray()));
      }
    }
    return regions;
  }

  private static Point[] convertCoordinates(LineString lineString) {
    // TODO for now we ignore holes, basically they need to be regarded to get the correct target area and also
    //  when we output the polygons again the holes also need to be transformed accordingly
    return Arrays.stream(lineString.getCoordinates())
      .map(coord -> new Point(coord.x, coord.y))
      .toArray(Point[]::new);
  }
}
