package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.CompactLineStringSequence;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Exercises {@link DefaultTripPatternGeometryService}: pattern-default lookup, per-trip override
 * resolution (override wins when present, falls back to the pattern default otherwise), and the
 * straight-line synthesis fallback for patterns with no registered geometry at all.
 */
class DefaultTripPatternGeometryServiceTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final RegularStop STOP_A = TEST_MODEL.stop("DTPGS-A")
    .withCoordinate(60.0000, 10.0000)
    .build();
  private static final RegularStop STOP_B = TEST_MODEL.stop("DTPGS-B")
    .withCoordinate(60.0180, 10.0000)
    .build();

  private static CompactLineStringSequence sequence(double lat1, double lat2) {
    return sequence(lat1, lat2, 2000);
  }

  private static CompactLineStringSequence sequence(double lat1, double lat2, int distanceMeters) {
    LineString line = GeometryUtils.makeLineString(lat1, 10.0, lat2, 10.0);
    return CompactLineStringSequence.of(java.util.List.of(line), new int[] { 0, distanceMeters });
  }

  private static TripPattern tripPattern(String id) {
    var route = TransitRepositoryForTest.route("R-" + id).build();
    var stopPattern = TransitRepositoryForTest.stopPattern(STOP_A, STOP_B);
    return TripPattern.of(org.opentripplanner.core.model.id.FeedScopedId.of("F", id))
      .withRoute(route)
      .withStopPattern(stopPattern)
      .build();
  }

  private static Trip trip(String id) {
    return TransitRepositoryForTest.trip(id).build();
  }

  @Test
  void patternGeometryFallsBackToStraightLineWhenNoneRegistered() {
    var repository = new TripPatternGeometryRepository();
    var service = new DefaultTripPatternGeometryService(repository);
    var pattern = tripPattern("pattern-1");

    LineString geometry = service.getPatternGeometry(pattern);

    assertNotNull(geometry);
    assertEquals(2, geometry.getNumPoints());
    assertEquals(STOP_A.getLat(), geometry.getCoordinateN(0).y);
    assertEquals(STOP_B.getLat(), geometry.getCoordinateN(1).y);
  }

  @Test
  void patternGeometryUsesRegisteredDefaultWhenPresent() {
    var repository = new TripPatternGeometryRepository();
    var service = new DefaultTripPatternGeometryService(repository);
    var pattern = tripPattern("pattern-2");
    repository.setPatternGeometry(pattern.getId(), sequence(61.0, 61.1));

    LineString geometry = service.getPatternGeometry(pattern);

    assertEquals(2, geometry.getNumPoints());
    assertEquals(61.0, geometry.getCoordinateN(0).x);
  }

  @Test
  void tripGeometryFallsBackToPatternDefaultWhenNoOverrideRegistered() {
    var repository = new TripPatternGeometryRepository();
    var service = new DefaultTripPatternGeometryService(repository);
    var pattern = tripPattern("pattern-3");
    var trip = trip("trip-3");
    repository.setPatternGeometry(pattern.getId(), sequence(62.0, 62.1));

    LineString tripGeometry = service.getTripGeometry(pattern, trip);
    LineString patternGeometry = service.getPatternGeometry(pattern);

    assertEquals(patternGeometry.getCoordinateN(0), tripGeometry.getCoordinateN(0));
  }

  @Test
  void tripGeometryUsesOverrideWhenRegistered() {
    var repository = new TripPatternGeometryRepository();
    var service = new DefaultTripPatternGeometryService(repository);
    var pattern = tripPattern("pattern-4");
    var trip = trip("trip-4");
    repository.setPatternGeometry(pattern.getId(), sequence(63.0, 63.1));
    repository.setTripGeometryOverride(trip.getId(), sequence(64.0, 64.1));

    LineString tripGeometry = service.getTripGeometry(pattern, trip);
    LineString patternGeometry = service.getPatternGeometry(pattern);

    assertEquals(64.0, tripGeometry.getCoordinateN(0).x);
    assertNotSame(patternGeometry.getCoordinateN(0), tripGeometry.getCoordinateN(0));
  }

  @Test
  void distanceAndGeometryBetweenAreConsistentAcrossPatternAndTripOverloads() {
    var repository = new TripPatternGeometryRepository();
    var service = new DefaultTripPatternGeometryService(repository);
    var pattern = tripPattern("pattern-5");
    var trip = trip("trip-5");
    repository.setPatternGeometry(pattern.getId(), sequence(60.0, 60.02));

    // No override registered: trip-aware overload matches the pattern-only overload.
    assertEquals(
      service.distanceBetween(pattern, 0, 1),
      service.distanceBetween(pattern, trip, 0, 1)
    );
    assertEquals(
      service.geometryBetween(pattern, 0, 1).getNumPoints(),
      service.geometryBetween(pattern, trip, 0, 1).getNumPoints()
    );

    repository.setTripGeometryOverride(trip.getId(), sequence(70.0, 70.5, 5000));

    // Once an override is registered, the trip-aware overload no longer matches the pattern one.
    assertNotEqualsDistance(
      service.distanceBetween(pattern, 0, 1),
      service.distanceBetween(pattern, trip, 0, 1)
    );
  }

  private static void assertNotEqualsDistance(int patternDistance, int tripDistance) {
    org.junit.jupiter.api.Assertions.assertNotEquals(patternDistance, tripDistance);
  }
}
