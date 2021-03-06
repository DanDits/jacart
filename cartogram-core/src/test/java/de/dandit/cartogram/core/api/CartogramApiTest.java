package de.dandit.cartogram.core.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.dandit.cartogram.core.PolygonUtilities;

public class CartogramApiTest {

  @Test
  public void handlesNaNValueWithOnlyOneValidRegion() throws ConvergenceGoalFailedException {
    Region region1 = new Region(
        13,
        5,
        List.of(new LightPolygon(
          new double[] {2,5,5,2,2},
          new double[] {8,8,4,4,8},
          List.of(),
          List.of())));
    Region region2 = new Region(
        1337,
        10,
      List.of(new LightPolygon(
        new double[] {5,6,6,5,5},
        new double[] {8,8,4,4,8},
        List.of(),
        List.of())));
    double[] targetAreas = new double[] {Double.NaN, 9000};
    double originalBoxMinX = 1;
    double originalBoxMinY = 3;
    double originalBoxMaxX = 6;
    double originalBoxMaxY = 9;
    MapFeatureData mapFeatureData = new MapFeatureData(originalBoxMinX, originalBoxMinY, originalBoxMaxX, originalBoxMaxY,
        List.of(region1, region2),
        targetAreas);
    double givenMaximumAreaError = 0.1;
    CartogramConfig config = new CartogramConfig(
        givenMaximumAreaError,
        true,
        Logging.ofStandardOutput(),
        FftPlanFactory.ofDefault(ParallelismConfig.ofCommonPool()),
        true,
        ParallelismConfig.ofCommonPool());

    CartogramResult result = new CartogramApi().calculateGaSeMo(mapFeatureData, config);

    assertEquals(0., result.getMaximumAreaError());
    List<ResultRegion> resultRegions = result.getResultRegions();
    assertEquals(2, resultRegions.size());
    assertTrue(resultRegions.get(0).isNaN());
    assertFalse(resultRegions.get(1).isNaN());
    List<LightPolygon> polygons = resultRegions.get(0).getPolygons();
    assertEquals(1, polygons.size());
    LightPolygon polygon1 = polygons.get(0);
    assertTrue(polygon1.getInteriorRingsX().isEmpty());
    assertTrue(polygon1.getInteriorRingsY().isEmpty());
    assertArrayEquals(new double[] {2,5,5,2,2}, resultRegions.get(0).getPolygons().get(0).getExteriorRingX(), 0.);
    assertArrayEquals(new double[] {8,8,4,4,8}, resultRegions.get(0).getPolygons().get(0).getExteriorRingY(), 0.);
    assertArrayEquals(new double[] {5,6,6,5,5}, resultRegions.get(1).getPolygons().get(0).getExteriorRingX(), 0.);
    assertArrayEquals(new double[] {8,8,4,4,8}, resultRegions.get(1).getPolygons().get(0).getExteriorRingY(), 0.);
  }

  @Test
  public void handlesNaNValueWithTwoValidRegions() throws ConvergenceGoalFailedException {
    // area is 2.5*4=10
    Region region1 = new Region(
        13,
        5,
      List.of(new LightPolygon(
        new double[] {2,4.5,4.5,2,2},
        new double[] {8,8,4,4,8},
        List.of(),
        List.of())));
    // area is 1.5*4=6
    Region region2 = new Region(
        1337,
        10,
      List.of(new LightPolygon(
        new double[] {4.5,6,6,4.5,4.5},
        new double[] {8,8,4,4,8},
        List.of(),
        List.of())));
    // area is 1*4=4
    Region region3 = new Region(
        133742,
        15,
      List.of(new LightPolygon(
        new double[] {6,7,7,6,6},
        new double[] {8,8,4,4,8},
        List.of(),
        List.of())));
    double factorOfArea3 = 3.;
    double[] targetAreas = new double[] {5, Double.NaN, 5 * factorOfArea3};
    double originalBoxMinX = 1;
    double originalBoxMinY = 3;
    double originalBoxMaxX = 7;
    double originalBoxMaxY = 9;
    MapFeatureData mapFeatureData = new MapFeatureData(originalBoxMinX, originalBoxMinY, originalBoxMaxX, originalBoxMaxY,
        List.of(region1, region2, region3),
        targetAreas);
    double givenMaximumAreaError = 0.1;
    CartogramConfig config = new CartogramConfig(
        givenMaximumAreaError,
        true,
        Logging.ofStandardOutput(),
        FftPlanFactory.ofDefault(ParallelismConfig.ofCommonPool()),
        true,
        ParallelismConfig.ofCommonPool());

    CartogramResult result = new CartogramApi().calculateGaSeMo(mapFeatureData, config);

    assertTrue(result.getMaximumAreaError() <= givenMaximumAreaError);
    List<ResultRegion> resultRegions = result.getResultRegions();
    assertEquals(3, resultRegions.size());
    assertFalse(resultRegions.get(0).isNaN());
    assertTrue(resultRegions.get(1).isNaN());
    assertFalse(resultRegions.get(2).isNaN());

    LightPolygon polygon1 = resultRegions.get(0).getPolygons().get(0);
    double area1 = PolygonUtilities.calculateOrientedArea(
        polygon1.getExteriorRingX(),
        polygon1.getExteriorRingY());
    LightPolygon polygon2 = resultRegions.get(1).getPolygons().get(0);
    double area2 = PolygonUtilities.calculateOrientedArea(
        polygon2.getExteriorRingX(),
        polygon2.getExteriorRingY());
    LightPolygon polygon3 = resultRegions.get(2).getPolygons().get(0);
    double area3 = PolygonUtilities.calculateOrientedArea(
        polygon3.getExteriorRingX(),
        polygon3.getExteriorRingY());
    assertTrue(Math.abs(area3 / area1 - factorOfArea3) <= 0.2);
    assertTrue(Math.abs(area2 - 6.3) <= 0.01);
    assertInBounds(originalBoxMinX, originalBoxMinY, originalBoxMaxX, originalBoxMaxY, polygon1);
    assertInBounds(originalBoxMinX, originalBoxMinY, originalBoxMaxX, originalBoxMaxY, polygon2);
    assertInBounds(originalBoxMinX, originalBoxMinY, originalBoxMaxX, originalBoxMaxY, polygon3);
  }


  @Test
  public void handleRemovingTinyRings() throws ConvergenceGoalFailedException {
    // area is 2.5*4=10 (non tiny)
    Region region1 = new Region(
      13,
      5,
      List.of(new LightPolygon(
        new double[] {2,4.5,4.5,2,2},
        new double[] {8,8,4,4,8},
        List.of(),
        List.of())));
    // two polygons:
    // first: area is 1.5*4=6 (non tiny) but has a tiny interior ring and a non tiny interior ring
    // second: area is tiny
    double tinyDelta = 0.0000001;
    Region region2 = new Region(
      1337,
      10,
      List.of(new LightPolygon(
        new double[] {4.5,6,6,4.5,4.5},
        new double[] {8,8,4,4,8},
        List.of(new double[] {4.6,4.6+tinyDelta,4.6+tinyDelta,4.6,4.6}, new double[] {4.7, 5, 5, 4.7,4.7}),
        List.of(new double[] {4.9+tinyDelta,4.9+tinyDelta,4.9,4.9,4.9+tinyDelta}, new double[] {6,6,5,5,6})),
        new LightPolygon(
          new double[] {4.5,4.5+tinyDelta,4.5+tinyDelta,4.5,4.5},
          new double[] {4+tinyDelta,4+tinyDelta,4,4,4+tinyDelta},
          List.of(new double[] {4.5,4.5+tinyDelta,4.5+tinyDelta,4.5,4.5}),
          List.of(new double[] {4+tinyDelta,4+tinyDelta,4,4,4+tinyDelta}))));
    // area is tiny
    Region region3 = new Region(
      133742,
      15,
      List.of(new LightPolygon(
        new double[] {6,6+tinyDelta,6+tinyDelta,6,6},
        new double[] {4+tinyDelta,4+tinyDelta,4,4,4+tinyDelta},
        List.of(),
        List.of())));
    double factorOfArea3 = 3.;
    double[] targetAreas = new double[] {5, 1, 5 * factorOfArea3};
    double originalBoxMinX = 1;
    double originalBoxMinY = 3;
    double originalBoxMaxX = 7;
    double originalBoxMaxY = 9;
    MapFeatureData mapFeatureData = new MapFeatureData(originalBoxMinX, originalBoxMinY, originalBoxMaxX, originalBoxMaxY,
      List.of(region1, region2, region3),
      targetAreas);
    double givenMaximumAreaError = 0.1;
    CartogramConfig config = new CartogramConfig(
      givenMaximumAreaError,
      true,
      Logging.ofStandardOutput(),
      FftPlanFactory.ofDefault(ParallelismConfig.ofCommonPool()),
      true,
      ParallelismConfig.ofCommonPool());

    CartogramResult result = new CartogramApi().calculateGaSeMo(mapFeatureData, config);

    assertTrue(result.getMaximumAreaError() <= givenMaximumAreaError);
    List<ResultRegion> resultRegions = result.getResultRegions();
    assertEquals(2, resultRegions.size());
    assertFalse(resultRegions.get(0).isNaN());
    assertFalse(resultRegions.get(1).isNaN());
    assertEquals(13, resultRegions.get(0).getRegionId());
    assertEquals(1337, resultRegions.get(1).getRegionId());

    assertEquals(1, resultRegions.get(0).getPolygons().size());
    assertEquals(1, resultRegions.get(1).getPolygons().size());
    assertEquals(0, resultRegions.get(0).getPolygons().get(0).getInteriorRingsX().size());
    assertEquals(1, resultRegions.get(1).getPolygons().get(0).getInteriorRingsX().size());
  }

  @DisplayName("Converges to precision")
  @ParameterizedTest(name = " with maximum area error below {0}")
  @ValueSource(doubles = {1.,0.9,0.8,0.7,0.6,0.5,0.4,0.3,0.2,0.1,0.01,0.001,0.0001,0.00001,0.000001})
  public void simpleGaSeMoConverges(double givenMaximumAreaError) throws ConvergenceGoalFailedException {
    // has area (5-2)*(8-4) = 12
    Region region1 = new Region(
        13,
        5,
      List.of(new LightPolygon(
        new double[] {2,5,5,2,2},
        new double[] {8,8,4,4,8},
        List.of(),
        List.of())));
    // has area (6-5)*(8-4)=4
    Region region2 = new Region(
        1337,
        10,
      List.of(new LightPolygon(
        new double[] {5,6,6,5,5},
        new double[] {8,8,4,4,8},
        List.of(),
        List.of())));
    // meaning: region1 initially has triple the size of region2 but should only be half the size of region2
    double[] targetAreas = new double[] {4500, 9000};
    double originalBoxMinX = 1;
    double originalBoxMinY = 3;
    double originalBoxMaxX = 6;
    double originalBoxMaxY = 9;
    MapFeatureData mapFeatureData = new MapFeatureData(originalBoxMinX, originalBoxMinY, originalBoxMaxX, originalBoxMaxY,
        List.of(region1, region2),
        targetAreas);
    CartogramConfig config = new CartogramConfig(
        givenMaximumAreaError,
        true,
        Logging.ofStandardOutput(),
        FftPlanFactory.ofDefault(ParallelismConfig.ofCommonPool()),
        true,
        ParallelismConfig.ofCommonPool());

    CartogramResult result = new CartogramApi().calculateGaSeMo(mapFeatureData, config);

    assertTrue(result.getMaximumAreaError() <= givenMaximumAreaError);
    List<ResultRegion> resultRegions = result.getResultRegions();
    assertEquals(2, resultRegions.size());
    assertFalse(resultRegions.get(0).isNaN());
    assertFalse(resultRegions.get(1).isNaN());
    List<LightPolygon> polygons = resultRegions.get(0).getPolygons();
    assertEquals(1, polygons.size());
    LightPolygon polygon1 = polygons.get(0);
    assertTrue(polygon1.getInteriorRingsX().isEmpty());
    assertTrue(polygon1.getInteriorRingsY().isEmpty());
    double area1 = PolygonUtilities.calculateOrientedArea(
        polygon1.getExteriorRingX(),
        polygon1.getExteriorRingY());
    LightPolygon polygon2 = resultRegions.get(1).getPolygons().get(0);
    double area2 = PolygonUtilities.calculateOrientedArea(
        polygon2.getExteriorRingX(),
        polygon2.getExteriorRingY());
    assertTrue(Math.abs(area1 / area2 - 0.5) < givenMaximumAreaError);
    assertInBounds(originalBoxMinX, originalBoxMinY, originalBoxMaxX, originalBoxMaxY, polygon1);
    assertInBounds(originalBoxMinX, originalBoxMinY, originalBoxMaxX, originalBoxMaxY, polygon2);
  }

  private void assertInBounds(
      double originalBoxMinX,
      double originalBoxMinY,
      double originalBoxMaxX,
      double originalBoxMaxY,
      LightPolygon polygon1) {
    assertTrue(Arrays.stream(polygon1.getExteriorRingX()).min().orElseThrow() >= originalBoxMinX);
    assertTrue(Arrays.stream(polygon1.getExteriorRingX()).max().orElseThrow() <= originalBoxMaxX);
    assertTrue(Arrays.stream(polygon1.getExteriorRingY()).min().orElseThrow() >= originalBoxMinY);
    assertTrue(Arrays.stream(polygon1.getExteriorRingY()).max().orElseThrow() <= originalBoxMaxY);
  }

}