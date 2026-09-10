package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.geometry.CompactLineStringSequence;
import org.opentripplanner.street.geometry.GeometryUtils;

/**
 * Exercises {@link TripPatternGeometryRepository} in isolation: the pattern-default map and the
 * per-trip override map are independent, and an override is only present when explicitly set.
 */
class TripPatternGeometryRepositoryTest {

  private static final FeedScopedId PATTERN_ID = FeedScopedId.of("F", "pattern-1");
  private static final FeedScopedId TRIP_ID = FeedScopedId.of("F", "trip-1");
  private static final FeedScopedId OTHER_TRIP_ID = FeedScopedId.of("F", "trip-2");

  private static CompactLineStringSequence sequence(double lat1, double lat2) {
    LineString line = GeometryUtils.makeLineString(lat1, 10.0, lat2, 10.0);
    return CompactLineStringSequence.of(java.util.List.of(line), new int[] { 0, 100 });
  }

  @Test
  void patternGeometryIsNullUntilRegistered() {
    var subject = new TripPatternGeometryRepository();
    assertNull(subject.getPatternGeometry(PATTERN_ID));

    var geometry = sequence(60.0, 60.1);
    subject.setPatternGeometry(PATTERN_ID, geometry);

    assertEquals(geometry, subject.getPatternGeometry(PATTERN_ID));
  }

  @Test
  void patternGeometryCanBeReplaced() {
    var subject = new TripPatternGeometryRepository();
    subject.setPatternGeometry(PATTERN_ID, sequence(60.0, 60.1));
    var replacement = sequence(61.0, 61.1);
    subject.setPatternGeometry(PATTERN_ID, replacement);

    assertEquals(replacement, subject.getPatternGeometry(PATTERN_ID));
  }

  @Test
  void tripOverrideIsAbsentByDefault() {
    var subject = new TripPatternGeometryRepository();
    assertNull(subject.getTripGeometryOverride(TRIP_ID));
    assertFalse(subject.hasTripGeometryOverride(TRIP_ID));
  }

  @Test
  void tripOverrideIsIndependentOfPatternGeometryAndOtherTrips() {
    var subject = new TripPatternGeometryRepository();
    subject.setPatternGeometry(PATTERN_ID, sequence(60.0, 60.1));

    var override = sequence(62.0, 62.1);
    subject.setTripGeometryOverride(TRIP_ID, override);

    assertTrue(subject.hasTripGeometryOverride(TRIP_ID));
    assertEquals(override, subject.getTripGeometryOverride(TRIP_ID));
    // Unrelated trip and the pattern default are unaffected.
    assertFalse(subject.hasTripGeometryOverride(OTHER_TRIP_ID));
    assertNull(subject.getTripGeometryOverride(OTHER_TRIP_ID));
    assertEquals(sequence(60.0, 60.1).size(), subject.getPatternGeometry(PATTERN_ID).size());
  }

  @Test
  void tripOverrideCanBeRemoved() {
    var subject = new TripPatternGeometryRepository();
    subject.setTripGeometryOverride(TRIP_ID, sequence(62.0, 62.1));
    assertTrue(subject.hasTripGeometryOverride(TRIP_ID));

    subject.removeTripGeometryOverride(TRIP_ID);

    assertFalse(subject.hasTripGeometryOverride(TRIP_ID));
    assertNull(subject.getTripGeometryOverride(TRIP_ID));
  }

  @Test
  void removingAbsentTripOverrideIsANoop() {
    var subject = new TripPatternGeometryRepository();
    subject.removeTripGeometryOverride(TRIP_ID);
    assertFalse(subject.hasTripGeometryOverride(TRIP_ID));
  }
}
