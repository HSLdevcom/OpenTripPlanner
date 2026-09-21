package org.opentripplanner.service.shape.internal;

import java.util.Map;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ShapeSnapshot;
import org.opentripplanner.service.shape.model.HopGeometrySequence;
import org.opentripplanner.service.shape.model.HopGeometrySequenceGroup;
import org.opentripplanner.service.shape.model.TripGeometry;

/**
 * SKELETON: field layout and lifecycle seam only. Method bodies are not implemented in this phase.
 * <p>
 * Holds the same two layers as {@link DefaultShapeRepository}, both now immutable: the static layer
 * shared by reference with every other snapshot built from the same graph, and the override map as
 * it stood at the commit this snapshot belongs to.
 */
public class DefaultShapeSnapshot implements ShapeSnapshot {

  private final StaticShapeLayer staticLayer;
  private final Map<FeedScopedId, TripGeometry> realTimeOverrides;

  DefaultShapeSnapshot(
    StaticShapeLayer staticLayer,
    Map<FeedScopedId, TripGeometry> realTimeOverrides
  ) {
    this.staticLayer = staticLayer;
    this.realTimeOverrides = realTimeOverrides;
  }

  @Override
  public Optional<HopGeometrySequence> findDefaultPatternGeometry(FeedScopedId patternId) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public Optional<HopGeometrySequenceGroup> findPatternGeometries(FeedScopedId patternId) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public Optional<TripGeometry> findTripGeometry(FeedScopedId tripId) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public boolean hasRealTimeGeometry(FeedScopedId tripId) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  /** Create the mutable repository a write transaction starts from. */
  DefaultShapeRepository copyOnWrite() {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }
}
