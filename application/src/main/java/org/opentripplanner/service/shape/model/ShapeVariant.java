package org.opentripplanner.service.shape.model;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * One distinct geometry of a trip pattern, together with the trips that use it.
 * <p>
 * A trip pattern groups trips by stop pattern, route, direction and mode — not by shape. A GTFS feed
 * may therefore put trips with several different {@code shape_id}s into one pattern, for example
 * when a route is diverted for part of the day. Today all but the first of those shapes is
 * discarded. A variant is what survives of each of them.
 * <p>
 * Carrying {@link #tripIds()} alongside the geometry is what makes this type worth having: it lets
 * one lookup answer both "which distinct geometries does this pattern have" and "which trips use
 * each of them", and it makes {@link #isDefault()} — the most common geometry — a plain maximum over
 * {@code tripIds().size()} rather than something that has to be recomputed by scanning every trip.
 * <p>
 * PROVISIONAL: this type is part of the initial {@code ShapeService} API design and may change
 * before the service is wired up.
 *
 * @param geometry the geometry shared by all of {@link #tripIds()}. Shared by reference with the
 *                 repository's per-trip index, so listing variants costs no extra memory.
 * @param shapeId  the id of the shape this geometry was built from, where the source data has one.
 *                 {@code null} for geometry that was synthesized (straight lines, or derived from
 *                 the original pattern by a real-time update) and for NeTEx, whose geometry comes
 *                 from ServiceLink projections rather than an identified shape.
 * @param tripIds  the trips using this geometry, in a stable order. Never empty.
 * @param source   how this geometry was produced.
 * @param isDefault whether this is the variant chosen to represent the pattern as a whole. Exactly
 *                  one variant of a pattern has this set.
 */
public record ShapeVariant(
  HopGeometrySequence geometry,
  @Nullable FeedScopedId shapeId,
  List<FeedScopedId> tripIds,
  GeometrySource source,
  boolean isDefault
) {
  /** How many trips use this geometry. This is the count the default variant is chosen by. */
  public int tripCount() {
    return tripIds.size();
  }
}
