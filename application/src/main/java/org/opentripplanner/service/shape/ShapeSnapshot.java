package org.opentripplanner.service.shape;

import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.model.HopGeometrySequence;
import org.opentripplanner.service.shape.model.HopGeometrySequenceGroup;
import org.opentripplanner.service.shape.model.TripGeometry;

/**
 * An immutable, read-only snapshot of all transit shape geometry. A new snapshot is published each
 * time a transaction that touched the {@link ShapeRepository} commits. Request threads read a
 * snapshot resolved at the start of the request, usually through the request-scoped
 * {@link ShapeService}.
 * <p>
 * <b>Raw lookups only.</b> Every method here takes an id and matches it exactly. In particular this
 * type performs no fallback from a real-time-created pattern to the scheduled pattern it was derived
 * from, and no fallback from a trip to its pattern. Use {@link ShapeService} for that; it is the
 * layer that knows about the transit model.
 * <p>
 * A snapshot spans both storage layers described on {@link ShapeRepository}: the static layer, which
 * it shares by reference with every other snapshot built from the same graph, and the frozen
 * real-time override layer belonging to this particular commit.
 */
public interface ShapeSnapshot {
  /**
   * The geometry chosen to represent the pattern as a whole — the one used by the most of its trips.
   * <p>
   * Empty when the pattern is unknown, or when it is degenerate and has no hops.
   */
  Optional<HopGeometrySequence> findDefaultPatternGeometry(FeedScopedId patternId);

  /**
   * Every distinct geometry of the pattern, each with the trips that use it and one of them flagged
   * as the default.
   * <p>
   * This is the same data the per-trip index holds, reached by a different path: the variants point
   * at the same {@link HopGeometrySequence} instances, so listing them costs no extra memory.
   * <p>
   * Empty when the pattern is unknown.
   */
  Optional<HopGeometrySequenceGroup> findPatternGeometries(FeedScopedId patternId);

  /**
   * The geometry belonging to this specific trip, if it has one that differs from its pattern's
   * default.
   * <p>
   * This is the trip's own entry only: a real-time override if one exists for this commit, otherwise
   * a static per-trip shape. It deliberately does <em>not</em> fall back to the pattern default,
   * because the distinction is the whole point of the method — callers that want "the geometry to
   * draw for this trip" want {@link ShapeService#getTripGeometry} instead.
   * <p>
   * Empty for the large majority of trips, which follow their pattern's default.
   */
  Optional<TripGeometry> findTripGeometry(FeedScopedId tripId);

  /**
   * Whether a real-time update has supplied or derived a geometry for this trip in the transaction
   * this snapshot belongs to.
   * <p>
   * Lets callers distinguish a real-time geometry from a static per-trip shape without decoding
   * anything.
   */
  boolean hasRealTimeGeometry(FeedScopedId tripId);
}
