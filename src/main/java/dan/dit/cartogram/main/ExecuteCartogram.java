package dan.dit.cartogram.main;

import dan.dit.cartogram.*;
import dan.dit.cartogram.Point;
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
import java.util.stream.Collectors;

import static org.locationtech.jts.geom.PrecisionModel.FLOATING;

public class ExecuteCartogram {

    public static void main(String[] args) throws IOException {
        Point[][] examplePolygons = new Point[3][];
        examplePolygons[0] = new Point[] {new Point(-0.5, 1), new Point(0.5, 1), new Point(0.5, -1), new Point(-0.5, -1), new Point(-0.5, 1)};
        examplePolygons[1] = new Point[] {new Point(0.5, 1), new Point(1.5, 1), new Point(1.5, -1), new Point(0.5, -1), new Point(0.5, 1)};
        examplePolygons[2] = new Point[] {new Point(-0.25, -1), new Point(0.75, -1), new Point(0.75, -1.5), new Point(-0.25, -1.5), new Point(-0.25, -1)};

        outputPolycornToFile(examplePolygons, "/home/daniel/cartogram/test/example.json");

        InputStream geoJsonResource = ExecuteCartogram.class.getResourceAsStream("reordered_geo.json");
        if (args.length > 1 && args[0].equals("-s")) {
            FileOutputStream outpFile = new FileOutputStream(args[1]);
            new GeoJsonIO().reWriteDataInIdOrder(geoJsonResource, outpFile);
            return;
        }
        InputStream dataResource = ExecuteCartogram.class.getResourceAsStream("sample_usa_data.csv");
        FileOutputStream epsOut = new FileOutputStream(new File("/home/daniel/cartogram/java/src/main/resources/dan/dit/cartogram/main/image.eps"));

        createCartogramToEps(geoJsonResource, dataResource, epsOut);
    }

    public static void createCartogramToEps(InputStream geoJsonResource, InputStream dataResource, OutputStream epsOut) throws IOException {
        FeatureCollection<SimpleFeatureType, SimpleFeature> geo = new GeoJsonIO().importData(geoJsonResource);
        CsvData data = new CsvDataImport().importCsv(dataResource);
        ReferencedEnvelope bounds = geo.getBounds();
        int regionIdColumnIndex = data.getNames().indexOf("Region.Id");
        int regionDataColumnIndex = data.getNames().indexOf("Region.Data");
        List<Region> regions = createRegions(geo, id -> {
            for (int i = 0; i < data.getData().size(); i++) {
                Object[] csvValues = data.getData().get(i);
                if (csvValues[regionIdColumnIndex].equals(id)) {
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
        CartogramContext context = new Cartogram()
                .calculate(mapFeatureData, cxt -> {
                });
        new EpsWriter().ps_figure(
                epsOut,
                context.getLx(),
                context.getLy(),
                context.getPolyinreg(),
                context.getRegionNa(),
                context.getCartcorn(),
                context.getProj(),
                true);
        outputPolycornToFile(context.getCartcorn(), "/home/daniel/cartogram/test/transformed.json");
    }

    private static void outputPolycornToFile(Point[][] polygons, String path) throws IOException {
        DefaultFeatureCollection resultAsGeo = new DefaultFeatureCollection();
        int dummy_id = 0;
        for (Point[] points : polygons) {
            List<Point[]> allPoints = new ArrayList<>();
            allPoints.add(points);
            SimpleFeature feature = createFeature(dummy_id, allPoints);
            resultAsGeo.add(feature);
            dummy_id++;
        }
        new GeoJsonIO().exportData(
                resultAsGeo,
                new FileOutputStream(new File(path)));
    }

    private static SimpleFeature createFeature(int id, List<Point[]> points) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("CartPoly");
        b.setCRS(DefaultGeographicCRS.WGS84); // set crs first
        b.add("geo", Polygon.class); // then add geometry

        final SimpleFeatureType simpleFeatureType = b.buildFeatureType();
        if (points.size() == 0) {
            return null;
        } else if (points.size() == 1) {
            SimpleFeature feature = new SimpleFeatureBuilder(simpleFeatureType)
                    .buildFeature(Integer.toString(id), new Object[]{asPolygon(points.get(0))});
            return feature;
        } else {
            SimpleFeature feature = new SimpleFeatureBuilder(simpleFeatureType)
                    .buildFeature(Integer.toString(id), new Object[]{
                            new MultiPolygon(points.stream()
                                    .map(ExecuteCartogram::asPolygon)
                                    .toArray(Polygon[]::new), new PrecisionModel(FLOATING), 0)});
            return feature;
        }
    }

    private static Polygon asPolygon(Point[] points) {
        PrecisionModel precision = new PrecisionModel(FLOATING);
        Coordinate[] coords = new Coordinate[points.length];
        for (int i = 0; i < points.length; i++) {
            coords[i] = new Coordinate(points[i].x, points[i].y);
        }
        return new Polygon(new LinearRing(coords, precision, 0), precision, 0);
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
                Point[][] points = new Point[1][];
                points[0] = convertPolygon((Polygon) geometry);
                regions.add(new Region(regionId,
                        valueProvider.apply(regionId),
                        points));
            } else if (geometry instanceof MultiPolygon) {
                MultiPolygon multiPolygon = (MultiPolygon) geometry;
                Point[][] points = new Point[multiPolygon.getNumGeometries()][];
                for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                    points[i] = convertPolygon((Polygon) multiPolygon.getGeometryN(i));
                }
                regions.add(new Region(regionId,
                        valueProvider.apply(regionId),
                        points));
            }
        }
        return regions;
    }

    private static Point[] convertPolygon(Polygon polygon) {
        // TODO for now we ignore holes, basically they need to be regarded to get the correct target area and also
        //  when we output the polygons again the holes also need to be transformed accordingly
        return Arrays.stream(polygon.getCoordinates())
                .map(coord -> new Point(coord.x, coord.y))
                .toArray(Point[]::new);
    }
}
