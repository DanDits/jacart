package dan.dit.cartogram;

public class Region {
    private final int regionId;
    private final double data;
    private final Point[][] hullCoordinates;

    public Region(int regionId, double data, Point[][] hullCoordinates) {
        this.regionId = regionId;
        this.data = data;
        this.hullCoordinates = hullCoordinates;
    }

    public Point[][] getHullCoordinates() {
        return hullCoordinates;
    }

    public double getData() {
        return data;
    }

    public int getId() {
        return regionId;
    }
}
