package org.opentripplanner.service.shape.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ShapeRepository;
import org.opentripplanner.service.shape.ShapeSnapshot;
import org.opentripplanner.service.shape.model.GeometrySource;
import org.opentripplanner.service.shape.model.HopGeometrySequence;
import org.opentripplanner.service.shape.model.TripGeometry;
import org.opentripplanner.transit.model.network.StopPattern;

/**
 * SKELETON: constructor shape, field layout and lifecycle seams only. Method bodies are not
 * implemented in this phase — this class exists to prove {@link ShapeRepository} is implementable
 * and to pin down the two-layer storage described there.
 * <p>
 * Note what {@link #copyOnWrite()} and {@link #freeze()} do and do not copy. The static layer is
 * handed on by reference, so a write transaction costs one map copy of the real-time overrides
 * rather than a copy of the whole pattern index. This is the same tradeoff the transaction
 * framework's own example draws between its two lifecycle strategies, resolved here in favour of
 * copying, because the overrides are small enough that a failed transaction can be discarded
 * cleanly.
 */
public class DefaultShapeRepository implements ShapeRepository {

  /** Immutable, shared by reference with every copy and every snapshot. Serialized. */
  private final StaticShapeLayer staticLayer;

  /** Copied on each write transaction. Not serialized. */
  private final Map<FeedScopedId, TripGeometry> realTimeOverrides;

  public DefaultShapeRepository() {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  private DefaultShapeRepository(
    StaticShapeLayer staticLayer,
    Map<FeedScopedId, TripGeometry> realTimeOverrides
  ) {
    this.staticLayer = staticLayer;
    this.realTimeOverrides = realTimeOverrides;
  }

  @Override
  public void addTripGeometry(
    FeedScopedId patternId,
    FeedScopedId tripId,
    @Nullable FeedScopedId shapeId,
    List<LineString> hopGeometries,
    GeometrySource source
  ) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public void indexStaticGeometries() {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public void setRealTimeGeometry(
    FeedScopedId tripId,
    FeedScopedId patternId,
    List<LineString> hopGeometries
  ) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public void removeRealTimeGeometry(FeedScopedId tripId) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public void removeRealTimeGeometriesForFeed(String feedId) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public Optional<HopGeometrySequence> deriveGeometryForModifiedStopPattern(
    FeedScopedId originalPatternId,
    StopPattern newStopPattern
  ) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  /**
   * Publish an immutable view of this repository. The new snapshot shares the static layer with
   * every other snapshot and takes ownership of the current override map.
   */
  ShapeSnapshot freeze() {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  /**
   * Create the mutable repository for a write transaction: the same static layer by reference, plus
   * a copy of the overrides so that a transaction which is never frozen is discarded cleanly.
   */
  DefaultShapeRepository copyOnWrite() {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }
}
