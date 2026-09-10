package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.street.geometry.CompactLineStringSequence;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Exercises {@link TripPatternGeometryFactory#buildHopGeometries} and the resulting
 * {@link CompactLineStringSequence} (the per-pattern hop geometries + cumulative distance table).
 * <p>
 * This mirrors {@code TripPatternGeometryTest}, which exercises the same contract via
 * {@code TripPatternBuilder.buildHopGeometries} (the pre-extraction call site). The two test
 * classes will be consolidated once {@code TripPatternBuilder} is updated to delegate to this
 * factory.
 */
class TripPatternGeometryFactoryTest {

  private static final TransitTestEnvironmentBuilder TEST_ENV = TransitTestEnvironment.of();

  private static final RegularStop STOP_A = TEST_ENV.stop("TPGF-A", b ->
    b.withCoordinate(60.0000, 10.0000)
  );
  private static final RegularStop STOP_B = TEST_ENV.stop("TPGF-B", b ->
    b.withCoordinate(60.0180, 10.0000)
  );
  private static final RegularStop STOP_C = TEST_ENV.stop("TPGF-C", b ->
    b.withCoordinate(60.0360, 10.0000)
  );

  private static final RegularStop STOP_MID_AB = TEST_ENV.stop("TPGF-MID", b ->
    b.withCoordinate(60.0090, 10.0000)
  );

  private static final StopPattern STOP_PATTERN = stopPattern(STOP_A, STOP_B, STOP_C);

  private static final List<LineString> HOP_GEOMETRIES = List.of(
    makeLineString(STOP_A.getCoordinate(), STOP_MID_AB.getCoordinate(), STOP_B.getCoordinate()),
    makeLineString(STOP_B.getCoordinate(), STOP_C.getCoordinate())
  );

  private static StopPattern stopPattern(StopLocation... stops) {
    var builder = StopPattern.create(stops.length);
    for (int i = 0; i < stops.length; i++) {
      builder.stops.with(i, stops[i]);
      builder.pickups.with(i, PickDrop.SCHEDULED);
      builder.dropoffs.with(i, PickDrop.SCHEDULED);
    }
    return builder.build();
  }

  private static double exactDistance(RegularStop from, RegularStop to) {
    return SphericalDistanceLibrary.distance(
      from.getLat(),
      from.getLon(),
      to.getLat(),
      to.getLon()
    );
  }

  @Test
  void distanceBetweenWithHopGeometriesMatchesHaversineWithinOneMeter() {
    var subject = TripPatternGeometryFactory.buildHopGeometries(STOP_PATTERN, HOP_GEOMETRIES);

    assertEquals(0, subject.distanceBetween(0, 0));

    double exactAtoB = exactDistance(STOP_A, STOP_B);
    double exactBtoC = exactDistance(STOP_B, STOP_C);
    double exactAtoC = exactAtoB + exactBtoC;

    assertEquals(exactAtoB, subject.distanceBetween(0, 1), 1.0);
    assertEquals(exactBtoC, subject.distanceBetween(1, 2), 1.0);
    assertEquals(exactAtoC, subject.distanceBetween(0, 2), 1.0);
  }

  @Test
  void distanceBetweenFallsBackToStraightLineWhenHopGeometriesAreNull() {
    var subject = TripPatternGeometryFactory.buildHopGeometries(STOP_PATTERN, null);

    assertEquals(0, subject.distanceBetween(0, 0));

    double exactAtoB = exactDistance(STOP_A, STOP_B);
    double exactBtoC = exactDistance(STOP_B, STOP_C);
    assertEquals(exactAtoB, subject.distanceBetween(0, 1), 1.0);
    assertEquals(exactBtoC, subject.distanceBetween(1, 2), 1.0);
  }

  @Test
  void cumulativeRoundingErrorIsBoundedByOneMeter() {
    int numberOfStops = 20;
    RegularStop[] stops = new RegularStop[numberOfStops];
    for (int i = 0; i < numberOfStops; i++) {
      double lat = 60.0 + i * 0.001;
      stops[i] = TEST_ENV.stop("TPGF-long-" + i, b -> b.withCoordinate(lat, 10.0));
    }
    StopPattern pattern = stopPattern(stops);

    var subject = TripPatternGeometryFactory.buildHopGeometries(pattern, null);

    double exactSum = 0;
    for (int i = 0; i < numberOfStops - 1; i++) {
      exactSum += exactDistance(stops[i], stops[i + 1]);
    }
    assertEquals(exactSum, subject.distanceBetween(0, numberOfStops - 1), 1.0);
  }

  @Test
  void distanceBetweenIsAdditive() {
    var subject = TripPatternGeometryFactory.buildHopGeometries(STOP_PATTERN, HOP_GEOMETRIES);

    int partA = subject.distanceBetween(0, 1);
    int partB = subject.distanceBetween(1, 2);
    int whole = subject.distanceBetween(0, 2);

    assertEquals(whole, partA + partB);
  }

  @Test
  void hopGeometryReturnsCompressedRoundTrip() {
    var subject = TripPatternGeometryFactory.buildHopGeometries(STOP_PATTERN, HOP_GEOMETRIES);

    LineString hop0 = subject.get(0);
    assertEquals(3, hop0.getNumPoints());
    assertEquals(STOP_A.getLat(), hop0.getCoordinateN(0).y, 1e-4);
    assertEquals(STOP_A.getLon(), hop0.getCoordinateN(0).x, 1e-4);
    assertEquals(STOP_B.getLat(), hop0.getCoordinateN(2).y, 1e-4);
    assertEquals(STOP_B.getLon(), hop0.getCoordinateN(2).x, 1e-4);

    LineString hop1 = subject.get(1);
    assertEquals(2, hop1.getNumPoints());
    assertEquals(STOP_B.getLat(), hop1.getCoordinateN(0).y, 1e-4);
    assertEquals(STOP_C.getLat(), hop1.getCoordinateN(1).y, 1e-4);
  }

  @Test
  void hopGeometrySynthesizesStraightLineWhenHopGeometriesAreNull() {
    var subject = TripPatternGeometryFactory.buildHopGeometries(STOP_PATTERN, null);

    LineString hop0 = subject.get(0);
    assertEquals(2, hop0.getNumPoints());
    assertEquals(STOP_A.getLat(), hop0.getCoordinateN(0).y);
    assertEquals(STOP_B.getLat(), hop0.getCoordinateN(1).y);
  }

  @Test
  void concatenateConcatenatesHopsInRange() {
    var subject = TripPatternGeometryFactory.buildHopGeometries(STOP_PATTERN, HOP_GEOMETRIES);

    LineString full = subject.concatenate(0, 2);
    assertEquals(4, full.getNumPoints());
    assertEquals(STOP_A.getLat(), full.getCoordinateN(0).y, 1e-4);
    assertEquals(STOP_C.getLat(), full.getCoordinateN(3).y, 1e-4);

    LineString firstHop = subject.concatenate(0, 1);
    assertEquals(3, firstHop.getNumPoints());

    LineString secondHop = subject.concatenate(1, 2);
    assertEquals(2, secondHop.getNumPoints());
  }

  @Test
  void concatenateAllReturnsFullPatternWhenShapeAvailable() {
    var subject = TripPatternGeometryFactory.buildHopGeometries(STOP_PATTERN, HOP_GEOMETRIES);

    LineString full = subject.concatenate(0, subject.size());
    assertNotNull(full);
    assertEquals(4, full.getNumPoints());
  }

  @Test
  void concatenateAllSynthesizesStraightLineWhenShapeMissing() {
    var subject = TripPatternGeometryFactory.buildHopGeometries(STOP_PATTERN, null);

    LineString full = subject.concatenate(0, subject.size());
    assertNotNull(full);
    assertEquals(3, full.getNumPoints());
    assertEquals(STOP_A.getLat(), full.getCoordinateN(0).y, 1e-4);
    assertEquals(STOP_C.getLat(), full.getCoordinateN(2).y, 1e-4);
  }

  @Test
  void factoryAcceptsZeroStopPattern() {
    StopPattern empty = StopPattern.create(0).build();
    var subject = TripPatternGeometryFactory.buildHopGeometries(empty, null);

    assertEquals(0, subject.size());
    assertEquals(0, subject.distanceBetween(0, 0));
    assertTrue(subject.concatenate(0, subject.size()).isEmpty());
  }

  @Test
  void factoryAcceptsSingleStopPattern() {
    StopPattern singleStop = stopPattern(STOP_A);
    var subject = TripPatternGeometryFactory.buildHopGeometries(singleStop, null);

    assertEquals(0, subject.distanceBetween(0, 0));
    LineString empty = subject.concatenate(0, subject.size());
    assertNotNull(empty);
    assertTrue(empty.isEmpty());
  }

  @Test
  void factoryAcceptsEmptyHopGeometries() {
    StopPattern singleStop = stopPattern(STOP_A);
    var subject = TripPatternGeometryFactory.buildHopGeometries(singleStop, List.of());

    assertEquals(0, subject.distanceBetween(0, 0));
    LineString empty = subject.concatenate(0, subject.size());
    assertNotNull(empty);
    assertTrue(empty.isEmpty());
  }

  @Test
  void factoryRejectsHopGeometriesWithWrongSize() {
    assertThrows(IllegalArgumentException.class, () ->
      TripPatternGeometryFactory.buildHopGeometries(STOP_PATTERN, List.of(HOP_GEOMETRIES.get(0)))
    );
    assertThrows(IllegalArgumentException.class, () ->
      TripPatternGeometryFactory.buildHopGeometries(STOP_PATTERN, List.of())
    );
  }

  @Test
  void concatenateWithSameBoardAndAlightReturnsEmptyGeometry() {
    var subject = TripPatternGeometryFactory.buildHopGeometries(STOP_PATTERN, HOP_GEOMETRIES);

    LineString empty = subject.concatenate(1, 1);
    assertNotNull(empty);
    assertTrue(empty.isEmpty());
  }
}
