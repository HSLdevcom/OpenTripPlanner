package org.opentripplanner.service.shape;

import org.opentripplanner.core.model.basic.Distance;
import org.opentripplanner.service.shape.model.HopGeometrySequence;
import org.opentripplanner.service.shape.model.HopGeometrySequenceGroup;
import org.opentripplanner.service.shape.model.TripGeometry;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * A read-only, request-scoped view over a {@link ShapeSnapshot} that resolves transit model objects
 * to the geometry that applies to them.
 * <p>
 * This is the API that takes over from the geometry accessors on {@code TripPattern}. Where the
 * snapshot does raw lookups by id, this service knows the transit model: it falls back from a
 * pattern created by a real-time update to the scheduled pattern it was derived from, and from a
 * trip to its pattern's default, so callers get a sensible answer without having to know which
 * layer their geometry came from.
 * <p>
 * A new instance should be created for each request, over a snapshot resolved from the same
 * transaction scope, so that the whole request sees one consistent view.
 *
 * <h2>Return types</h2>
 * The whole-geometry accessors return {@link HopGeometrySequence} rather than a decoded
 * {@code LineString}. The sequence keeps its hops in compacted form and decodes on demand, which is
 * the entire reason the compact representation exists; returning a decoded geometry from every
 * accessor would decode shapes that most callers never draw. Callers that do want one line string
 * call {@link HopGeometrySequence#concatenate()}.
 *
 * <h2>Defaults</h2>
 * The methods with "default" in the name answer for the pattern as a whole, using the geometry that
 * the most of its trips follow. They exist for the many callers — pattern-level API fields, map
 * layers, debugging tools — that want one representative geometry and have no particular trip in
 * mind. "Default" here means representative, not fallback.
 */
public interface ShapeService {
  /**
   * The geometry representing the pattern as a whole, i.e. the one used by the most of its trips.
   * <p>
   * This is the successor to {@code TripPattern.getGeometry()}. For a pattern created by a real-time
   * update the scheduled pattern it was derived from is consulted instead. The result is empty, not
   * null, for a degenerate pattern with fewer than two stops.
   */
  HopGeometrySequence getDefaultPatternGeometry(TripPattern pattern);

  /**
   * Every distinct geometry of the pattern, each with the trips that use it, ordered most-used
   * first.
   * <p>
   * For the common case of a pattern whose trips all follow one shape this holds a single variant
   * and is no more expensive than {@link #getDefaultPatternGeometry}. It is worth asking for when
   * the caller needs to show or reason about a route that is diverted for part of the day.
   */
  HopGeometrySequenceGroup getPatternGeometries(TripPattern pattern);

  /**
   * The geometry that applies to this trip, together with where it came from.
   * <p>
   * Resolved in order: a real-time override for this trip, then a static per-trip shape, then the
   * trip pattern's default. Every trip therefore gets a geometry, and
   * {@link TripGeometry#source()} says which of those cases applied.
   * <p>
   * Note that a per-trip static shape is a GTFS-only possibility. NeTEx attaches its ServiceLink
   * projections to the JourneyPattern, so every ServiceJourney on a journey pattern shares one
   * geometry and this method always resolves to the pattern default for NeTEx data.
   */
  TripGeometry getTripGeometry(Trip trip);

  /**
   * The part of the pattern's default geometry between two stop positions.
   * <p>
   * This is the successor to {@code TripPattern.geometryBetween(int, int)} and is what a transit
   * leg's geometry is built from. The returned sequence is a view over the same interned hops rather
   * than a copy of them.
   */
  HopGeometrySequence defaultGeometryBetween(
    TripPattern pattern,
    int boardingStopPosition,
    int alightingStopPosition
  );

  /**
   * Every distinct geometry of the pattern between two stop positions.
   * <p>
   * Restricting the range can merge variants: trips that diverge elsewhere on the pattern but agree
   * over this stretch collapse into one variant here. That is the useful behaviour for a caller
   * asking "how do vehicles get from A to B on this pattern", which may well have a single answer
   * even when the pattern as a whole has several.
   */
  HopGeometrySequenceGroup geometriesBetween(
    TripPattern pattern,
    int boardingStopPosition,
    int alightingStopPosition
  );

  /**
   * The part of this specific trip's geometry between two stop positions, resolved with the same
   * precedence as {@link #getTripGeometry}.
   */
  HopGeometrySequence geometryBetween(
    Trip trip,
    int boardingStopPosition,
    int alightingStopPosition
  );

  /**
   * How far this trip travels between two stop positions, measured along its geometry.
   * <p>
   * Read from a cumulative distance table in constant time, not by decoding and measuring the
   * geometry.
   */
  Distance distanceBetween(Trip trip, int boardingStopPosition, int alightingStopPosition);

  /**
   * The same measurement as {@link #distanceBetween} in whole meters.
   * <p>
   * This exists alongside the {@link Distance} accessor for the routing-result hot path: every
   * transit leg of every itinerary needs its distance, and legs are produced in large numbers, so
   * the accessor used there should not allocate. Prefer {@link #distanceBetween} everywhere else.
   */
  int distanceMetersBetween(Trip trip, int boardingStopPosition, int alightingStopPosition);

  /**
   * How far the pattern's default geometry travels between two stop positions, in whole meters.
   * <p>
   * The successor to {@code TripPattern.distanceBetween(int, int)}, for callers that have a pattern
   * and a stop range but no particular trip.
   */
  int defaultDistanceMetersBetween(
    TripPattern pattern,
    int boardingStopPosition,
    int alightingStopPosition
  );
}
