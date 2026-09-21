package org.opentripplanner.service.shape.internal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.model.HopGeometry;
import org.opentripplanner.service.shape.model.HopGeometrySequence;
import org.opentripplanner.service.shape.model.ShapeVariant;

/**
 * The immutable static half of the shape repository: everything that comes out of graph build and is
 * serialized with the graph.
 * <p>
 * Pulling this out into its own type is what makes the copy-on-write lifecycle cheap. A repository
 * copy made for a write transaction, and every snapshot frozen from one, holds a reference to the
 * <em>same</em> instance of this class; only the small real-time override map is actually copied.
 * With a hundred thousand patterns in a national feed and a commit on every periodic tick, copying
 * these indexes per transaction would not be viable.
 * <p>
 * It is also the serialization boundary: this is the part of the repository that
 * {@code SerializedGraphObject} would persist, and the real-time overrides are simply absent from
 * the serialized form.
 *
 * <h2>Two indexes, one set of sequences</h2>
 * {@link #variantsByPattern} and {@link #geometryByTrip} are two access paths over the same
 * {@link HopGeometrySequence} instances, not two copies of the data:
 * <ul>
 *   <li>the per-pattern index answers "what distinct geometries does this pattern have, and which
 *       trips use each" in one lookup, which is what the pattern-level API fields need and what
 *       makes the most-used default computable;</li>
 *   <li>the per-trip index answers "what geometry applies to this trip" in constant time, which is
 *       the hot path, and holds an entry only for trips that differ from their pattern's default —
 *       so it stays small.</li>
 * </ul>
 * Keeping only the per-pattern index would force a scan plus caller-side deduplication for the
 * common query; keeping only the per-trip index would lose the trip-to-geometry association that
 * makes the default selectable.
 * <p>
 * SKELETON: field layout and contract only. No logic is implemented in this phase.
 */
final class StaticShapeLayer implements Serializable {

  /**
   * The interned hops. Every {@link HopGeometrySequence} in either index is assembled from members
   * of this pool, so a geometry shared by two trips, two patterns or a trip and its pattern is
   * stored once. See {@link HopGeometry} for why hops rather than whole shapes are the unit.
   */
  private final List<HopGeometry> internedHops;

  /**
   * Pattern id to its distinct geometries, ordered most-used first, with exactly one flagged as the
   * default.
   */
  private final Map<FeedScopedId, List<ShapeVariant>> variantsByPattern;

  /**
   * Trip id to its own geometry, for the minority of trips whose geometry differs from their
   * pattern's default. A trip absent from this map follows its pattern.
   */
  private final Map<FeedScopedId, HopGeometrySequence> geometryByTrip;

  StaticShapeLayer(
    List<HopGeometry> internedHops,
    Map<FeedScopedId, List<ShapeVariant>> variantsByPattern,
    Map<FeedScopedId, HopGeometrySequence> geometryByTrip
  ) {
    this.internedHops = internedHops;
    this.variantsByPattern = variantsByPattern;
    this.geometryByTrip = geometryByTrip;
  }

  List<HopGeometry> internedHops() {
    return internedHops;
  }

  Map<FeedScopedId, List<ShapeVariant>> variantsByPattern() {
    return variantsByPattern;
  }

  Map<FeedScopedId, HopGeometrySequence> geometryByTrip() {
    return geometryByTrip;
  }
}
