package org.opentripplanner.street.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

public class PolylineEncoderTest {

  @Test
  public void testCreateEncodingsOfCoordinateArray() {
    // test taken from example usage
    List<Coordinate> points = new ArrayList<>();
    points.add(new Coordinate(-73.85062, 40.903125, Double.NaN));
    points.add(new Coordinate(-73.85136, 40.902261, Double.NaN));
    points.add(new Coordinate(-73.85151, 40.902066, Double.NaN));
    var eplb = PolylineEncoder.encodeCoordinates(points.toArray(new Coordinate[0]));
    assertEquals("o{sxFl}vaMjDpCf@\\", eplb.points());
    assertEquals(3, eplb.length());
  }

  @Test
  public void testPolygon() {
    var polygon = GeometryUtils.getGeometryFactory().createPolygon(
      new Coordinate[] {
        new Coordinate(0, 0),
        new Coordinate(1, 1),
        new Coordinate(2, 2),
        new Coordinate(0, 0),
      }
    );
    var polyline = PolylineEncoder.encodeGeometry(polygon);

    assertEquals("??_ibE_ibE_ibE_ibE~reK~reK", polyline.points());
  }

  @Test
  public void testPoint() {
    var point = GeometryUtils.getGeometryFactory().createPoint(new Coordinate(100, 100));
    var polyline = PolylineEncoder.encodeGeometry(point);

    assertEquals("_gjaR_gjaR", polyline.points());
  }

  @Test
  public void testDecodeKnownEncoding() {
    // taken from https://developers.google.com/maps/documentation/utilities/polylinealgorithm
    var decoded = PolylineEncoder.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
    var expected = List.of(
      new Coordinate(-120.2, 38.5),
      new Coordinate(-120.95, 40.7),
      new Coordinate(-126.453, 43.252)
    );
    assertEquals(expected.size(), decoded.size());
    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i).x, decoded.get(i).x, 1e-4);
      assertEquals(expected.get(i).y, decoded.get(i).y, 1e-4);
    }
  }

  @Test
  public void testDecodeIsInverseOfEncode() {
    List<Coordinate> points = new ArrayList<>();
    points.add(new Coordinate(-73.85062, 40.903125));
    points.add(new Coordinate(-73.85136, 40.902261));
    points.add(new Coordinate(-73.85151, 40.902066));
    var encoded = PolylineEncoder.encodeCoordinates(points.toArray(new Coordinate[0]));

    var decoded = PolylineEncoder.decode(encoded.points());

    assertEquals(points.size(), decoded.size());
    for (int i = 0; i < points.size(); i++) {
      assertEquals(points.get(i).x, decoded.get(i).x, 1e-4);
      assertEquals(points.get(i).y, decoded.get(i).y, 1e-4);
    }
  }

  @Test
  public void testDecodeMalformedInputThrows() {
    // valid latitude only, no matching longitude
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
      PolylineEncoder.decode("_p~iF")
    );
  }
}
