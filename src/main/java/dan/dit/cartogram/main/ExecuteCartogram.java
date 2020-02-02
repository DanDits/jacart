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
import java.util.stream.Collectors;

import static org.locationtech.jts.geom.PrecisionModel.FLOATING;

public class ExecuteCartogram {

    public static void main(String[] args) throws IOException {

        InputStream geoJsonResource = ExecuteCartogram.class.getResourceAsStream("sample_usa_geo.json");
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
        List<Region> regions = data.getData().stream()
                .map(csvValues -> {
                    Integer regionId = (Integer) csvValues[regionIdColumnIndex];
                    Map.Entry<Double, Point[][]> areaAndGeo = getGeoForRegion(geo, regionId);
                    return new Region(
                            regionId,
                            (Double) csvValues[regionDataColumnIndex],
                            areaAndGeo.getValue());
                })
                .collect(Collectors.toList());
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
        CartogramContext context = new Cartogram().calculate(mapFeatureData,
                (cxt) ->
                {
                });
        new EpsWriter().ps_figure(
                epsOut,
                context.getLx(),
                context.getLy(),
                context.getN_reg(),
                context.getPolyinreg(),
                context.getRegionNa(),
                context.getCartcorn(),
                context.getProj(),
                true);
    }

    private static void outputPolycornToFile(CartogramContext context, String path) throws IOException {
        DefaultFeatureCollection resultAsGeo = new DefaultFeatureCollection();
        int dummy_id = 0;
        for (Point[] points : context.getCartcorn()) {
            List<Point[]> allPoints = new ArrayList<>();
            allPoints.add(points);
            SimpleFeature feature = createFeature(dummy_id, allPoints);
            resultAsGeo.add(feature);
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

    private static Map.Entry<Double, Point[][]> getGeoForRegion(FeatureCollection<SimpleFeatureType, SimpleFeature> geo, Integer regionId) {
        FeatureIterator<SimpleFeature> iterator = geo.features();
        while (iterator.hasNext()) {
            SimpleFeature feature = iterator.next();
            Object value = feature.getProperties("cartogram_id").iterator().next().getValue();
            if (regionId.equals(Integer.parseInt(value.toString()))) {
                Object geometry = feature.getDefaultGeometry();
                if (geometry instanceof Polygon) {
                    Point[][] points = new Point[1][];
                    points[0] = convertPolygon((Polygon) geometry);
                    return Map.entry(((Polygon) geometry).getArea(), points);
                } else if (geometry instanceof MultiPolygon) {
                    MultiPolygon multiPolygon = (MultiPolygon) geometry;
                    Point[][] points = new Point[multiPolygon.getNumGeometries()][];
                    for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                        points[i] = convertPolygon((Polygon) multiPolygon.getGeometryN(i));
                    }
                    return Map.entry(((MultiPolygon) geometry).getArea(), points);
                }
            }
        }
        throw new IllegalStateException("No feature found for id " + regionId);
    }

    private static Point[] convertPolygon(Polygon polygon) {
        // TODO for now we ignore holes, basically they need to be regarded to get the correct target area and also
        //  when we output the polygons again the holes also need to be transformed accordingly
        return Arrays.stream(polygon.getCoordinates())
                .map(coord -> new Point(coord.x, coord.y))
                .toArray(Point[]::new);
    }
}
