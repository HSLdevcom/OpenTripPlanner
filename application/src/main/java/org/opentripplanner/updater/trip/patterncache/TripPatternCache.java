package org.opentripplanner.updater.trip.patterncache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.ShapeHopGeometrySlicer;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Threadsafe mechanism for tracking any TripPatterns added to the graph via realtime messages.
 * This tracks only patterns added by realtime messages, not ones that already existed from the
 * scheduled NeTEx or GTFS. This is a "cache" in the sense that it will keep returning the same TripPattern
 * when presented with the same StopPattern, so if realtime messages add many trips passing through
 * the same sequence of stops, they will all end up on this same TripPattern.
 *  TODO RT_TG: There is no clear strategy for what should be in the cache and the transit model and the flow
 *             between them.
 *             With the increased usage of DatedServiceJourneys, this should probably
 *             be part of the main model - not a separate cache. It is possible that this class works when it comes to
 *             the thread-safety, but just by looking at a few lines of code I see problems - a strategy needs to be
 *             analysed, designed and documented.
 *  TODO RT_VP  TripPatternCache caches RT patterns keyed by StopPattern only, setting
 *              originalTripPattern from the first trip that created the entry in the cache.
 *              When a second trip on a different route produces the same modified StopPattern,
 *              the cache returns a pattern with potentially the wrong originalTripPattern.
 *              If the updater uses originalTripPattern to identify the original scheduled pattern
 *              of a modified trip, it will return the wrong result
 *              (symptom: when looking up for TripTimes in the wrong pattern's timetable,
 *              it will get null → TRIP_NOT_FOUND_IN_PATTERN).
 *
 */
public class TripPatternCache {

  /**
   * Default snapping distance (in meters) used to match stops onto a GTFS-RT {@code Shape} when
   * slicing it into per-hop geometries. This mirrors the graph builder's default
   * {@code maxStopToShapeSnapDistance}; unlike the graph builder, this value is not currently
   * configurable for real-time shapes.
   */
  private static final double DEFAULT_MAX_STOP_TO_SHAPE_SNAP_DISTANCE = 150;

  /**
   * We cache the trip pattern based on the stop pattern and, if present, the id of a GTFS-RT
   * {@code Shape} applied to it, in order to de-duplicate them. Two trips sharing the same stop
   * pattern but referencing different (or no) shapes must not collide on the same cached pattern.
   * <p>
   * Note that we don't really have a definition which properties are really part of the trip
   * pattern and several pattern keys are used in different parts of OTP.
   */
  private final Map<PatternCacheKey, TripPattern> cache = new HashMap<>();

  private final TripPatternIdGenerator tripPatternIdGenerator;

  public TripPatternCache(TripPatternIdGenerator tripPatternIdGenerator) {
    this.tripPatternIdGenerator = tripPatternIdGenerator;
  }

  /**
   * Get cached trip pattern or create one if it doesn't exist yet, without any GTFS-RT
   * {@code Shape} override. See {@link #getOrCreateTripPattern(StopPattern, Trip, TripPattern,
   * Optional)}.
   */
  public TripPattern getOrCreateTripPattern(
    final StopPattern stopPattern,
    final Trip trip,
    @Nullable final TripPattern originalTripPattern
  ) {
    return getOrCreateTripPattern(stopPattern, trip, originalTripPattern, Optional.empty());
  }

  /**
   * Get cached trip pattern or create one if it doesn't exist yet.
   * <p>
   * If {@code originalTripPattern} is non-null, its stop pattern matches {@code stopPattern}, and
   * no {@code resolvedShape} override is given, the original pattern is returned as-is — no new RT
   * pattern is created. Otherwise the cache is checked by stop pattern and shape id; if no entry
   * exists, a new realtime-modified pattern is created, stored, and returned.
   * <p>
   * The caller is responsible for resolving {@code originalTripPattern} before calling this method.
   *
   * @param stopPattern         stop pattern to retrieve/create a trip pattern for
   * @param trip                trip whose route, mode, and submode are copied when a new pattern is
   *                            created; also used to generate the new pattern's id
   * @param originalTripPattern the current pattern for {@code trip} — either the static scheduled
   *                            pattern, or a previously RT-modified pattern if the trip was already
   *                            updated. {@code null} for genuinely new added trips.
   * @param resolvedShape       a GTFS-RT {@code Shape} to use for the pattern's hop geometries, if
   *                            the trip update carried a {@code TripProperties.shape_id} that was
   *                            resolved against a {@code Shape} FeedEntity in the same message.
   *                            When empty, hop geometries fall back to the default behaviour
   *                            (straight lines for newly created patterns).
   * @return the original, cached, or newly created trip pattern
   */
  public synchronized TripPattern getOrCreateTripPattern(
    final StopPattern stopPattern,
    final Trip trip,
    @Nullable final TripPattern originalTripPattern,
    final Optional<RealtimeShapeReference> resolvedShape
  ) {
    if (
      resolvedShape.isEmpty() &&
      originalTripPattern != null &&
      originalTripPattern.getStopPattern().equals(stopPattern)
    ) {
      return originalTripPattern;
    }

    var cacheKey = new PatternCacheKey(
      stopPattern,
      resolvedShape.map(RealtimeShapeReference::shapeId).orElse(null)
    );

    // Check cache for trip pattern
    TripPattern tripPattern = cache.get(cacheKey);

    // Create TripPattern if it doesn't exist yet
    if (tripPattern == null) {
      var id = tripPatternIdGenerator.generateUniqueTripPatternId(trip);
      var builder = TripPattern.of(id)
        .withRoute(trip.getRoute())
        .withMode(trip.getMode())
        .withNetexSubmode(trip.getNetexSubMode())
        .withStopPattern(stopPattern)
        .withRealTimeStopPatternModified()
        .withOriginalTripPattern(originalTripPattern);

      resolvedShape.ifPresent(shape -> {
        List<LineString> hopGeometries = sliceHopGeometries(stopPattern, shape.geometry());
        if (hopGeometries != null) {
          builder.withHopGeometries(hopGeometries);
        }
      });

      tripPattern = builder.build();

      // Add pattern to cache
      cache.put(cacheKey, tripPattern);
    }

    return tripPattern;
  }

  /**
   * Slice {@code shape} into one hop geometry per stop pattern hop, or return {@code null} if the
   * stops could not be consistently matched to the shape (falls back to the default straight-line
   * hop geometries).
   */
  @Nullable
  private static List<LineString> sliceHopGeometries(StopPattern stopPattern, LineString shape) {
    List<Coordinate> stopCoordinates = new ArrayList<>(stopPattern.getSize());
    for (int i = 0; i < stopPattern.getSize(); i++) {
      stopCoordinates.add(stopPattern.getStop(i).getCoordinate().asJtsCoordinate());
    }
    return ShapeHopGeometrySlicer.sliceIntoHopGeometries(
      shape,
      stopCoordinates,
      DEFAULT_MAX_STOP_TO_SHAPE_SNAP_DISTANCE,
      false
    );
  }

  private record PatternCacheKey(StopPattern stopPattern, @Nullable String shapeId) {}
}
