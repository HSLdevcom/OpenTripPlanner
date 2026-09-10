package org.opentripplanner.transit.service;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.CompactLineString;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Provides read access to the stop-to-stop geometry of trip patterns, with the possibility of a
 * per-trip override.
 * <p>
 * Every {@link TripPattern} has a default geometry, used for API listing and per-leg distance
 * computation when no more specific geometry is available. Individual {@link Trip}s within a
 * pattern may additionally have their own override geometry (for example, GTFS trips that share a
 * stop pattern but reference a different {@code shape_id}); the {@code Trip}-aware methods here
 * return that override when present and fall back to the pattern's default otherwise.
 * <p>
 * Geometry is purely informational: it is exposed through the APIs (GraphQL) and used to compute
 * itinerary leg distances/shapes, but it plays no part in the transit routing search itself.
 */
public interface TripPatternGeometryService {
  /**
   * The default geometry for the whole pattern, obtained by concatenating its hop geometries.
   * Never {@code null}; empty for degenerate patterns with fewer than two stops.
   */
  LineString getPatternGeometry(TripPattern pattern);

  /**
   * The default geometry of a single hop (the segment between two consecutive stops) in the
   * pattern.
   */
  LineString getHopGeometry(TripPattern pattern, int stopPosInPattern);

  /**
   * Geometry of the pattern segment between the boarding and alighting stop positions, using the
   * pattern's default geometry.
   */
  LineString geometryBetween(
    TripPattern pattern,
    int boardingStopPosition,
    int alightingStopPosition
  );

  /**
   * Distance in meters along the pattern between the boarding and alighting stop positions, using
   * the pattern's default geometry.
   */
  int distanceBetween(TripPattern pattern, int boardingStopPosition, int alightingStopPosition);

  /**
   * The geometry for the given trip: its own override geometry if one has been registered,
   * otherwise the pattern's default geometry. Never {@code null}.
   */
  LineString getTripGeometry(TripPattern pattern, Trip trip);

  /**
   * Geometry of the segment between the boarding and alighting stop positions for the given trip:
   * uses the trip's own override geometry if one has been registered, otherwise the pattern's
   * default geometry.
   */
  LineString geometryBetween(
    TripPattern pattern,
    Trip trip,
    int boardingStopPosition,
    int alightingStopPosition
  );

  /**
   * Distance in meters along the trip between the boarding and alighting stop positions: uses the
   * trip's own override geometry if one has been registered, otherwise the pattern's default
   * geometry.
   */
  int distanceBetween(
    TripPattern pattern,
    Trip trip,
    int boardingStopPosition,
    int alightingStopPosition
  );

  /**
   * A service instance with no registered pattern/trip geometry, always falling back to
   * synthesizing straight-line geometry between consecutive stops. Useful as a default for
   * contexts (e.g. unit tests, or legs built outside of a fully wired graph) that don't have
   * access to a populated {@link TripPatternGeometryRepository}.
   */
  static TripPatternGeometryService noop() {
    return new DefaultTripPatternGeometryService(new TripPatternGeometryRepository());
  }
  public record HopGeometry() {};
  public record HopGeometrySequence() {};
  public record HopGeometrySequenceGroup() {};
  public record TripGeometry() {};
  public record TripGeometryList() {};

  LineString getDefaultPatternGeometry(TripPattern pattern);

  TripGeometryList getPatternGeometries(TripPattern pattern);

  TripGeometry getTripGeometry(Trip trip);

  LineString defaultGeometryBetween(
    TripPattern pattern,
    int boardingStopPosition,
    int alightingStopPosition
  );
  
  HopGeometrySequenceGroup geometriesBetween(
    TripPattern pattern,
    int boardingStopPosition,
    int alightingStopPosition
  );

  HopGeometrySequence geometryBetween(
    Trip trip,
    int boardingStopPosition,
    int alightingStopPosition
  );
  
  Distance distanceBetween(Trip trip, int boardingStopPosition, int alightingStopPosition);

}
