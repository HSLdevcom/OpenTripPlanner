package org.opentripplanner.service.shape.model;

import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * The geometry that applies to one specific trip, with enough provenance to explain it.
 * <p>
 * Every trip has a geometry, but not every trip has one of its own. Most trips fall back to the
 * geometry of their pattern, and that fallback is not an error — for NeTEx it is the only
 * possibility, since ServiceLink projections belong to the JourneyPattern. {@link #source()} records
 * which case applies, so an API can say whether it is showing the trip's own shape, the pattern's,
 * or a straight-line approximation.
 * <p>
 * PROVISIONAL: this type is part of the initial {@code ShapeService} API design and may change
 * before the service is wired up.
 *
 * @param tripId   the trip this geometry was resolved for.
 * @param geometry the resolved geometry. When {@link #source()} is
 *                 {@link GeometrySource#PATTERN_DEFAULT} this is the very same instance the pattern
 *                 default holds, not a copy.
 * @param source   how the geometry was produced, and in particular whether it belongs to this trip
 *                 or was inherited from the pattern.
 * @param shapeId  the id of the shape this geometry was built from, or {@code null} when the source
 *                 data has no identified shape (NeTEx, straight-line fallback, geometry derived by
 *                 a real-time update).
 */
public record TripGeometry(
  FeedScopedId tripId,
  HopGeometrySequence geometry,
  GeometrySource source,
  @Nullable FeedScopedId shapeId
) {
  /**
   * Whether this geometry belongs to the trip itself rather than being inherited from its pattern.
   */
  public boolean isTripSpecific() {
    return source == GeometrySource.STATIC_TRIP_SHAPE || source == GeometrySource.REAL_TIME;
  }
}
