package org.opentripplanner.service.shape;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

/**
 * A read-only, request-scoped view over a {@link ShapeRepositorySnapshot}: the scheduled shapes
 * imported at graph build time, overridden by the shapes of real-time patterns. It serves the
 * shapes of trip patterns and trips, with one geometry for each hop between two consecutive
 * stops.
 * <p>
 * For patterns built without shape data (GTFS without {@code shapes.txt}, NeTEx without ServiceLink
 * projections, real-time added trips), the shape consists of straight lines between the stops.
 * Every scheduled trip pattern and every real-time pattern that has been processed by a real-time
 * updater has a shape. Only a new real-time pattern that is still being created inside an
 * update transaction has none; such a pattern is never visible here, because this service reads
 * committed snapshots only. Asking for the shape of an unknown pattern is therefore a bug.
 */
public interface ShapeService {
  /**
   * The shape of the trip pattern. Trips of the pattern can follow other shapes, so use
   * {@link #getTripShape} when the trip is known.
   *
   * @throws IllegalArgumentException if the committed snapshot has no scheduled or real-time shape
   *                                  for the trip pattern id
   */
  CompactLineStringSequence getTripPatternShape(FeedScopedId tripPatternId);

  /**
   * The shape of the trip when it runs on the given trip pattern. Trips of one pattern can follow
   * different shapes. A trip without a shape of its own follows the
   * {@link #getTripPatternShape trip pattern shape}.
   *
   * @throws IllegalArgumentException if there is no shape for the trip pattern
   */
  CompactLineStringSequence getTripShape(FeedScopedId tripPatternId, FeedScopedId tripId);
}
