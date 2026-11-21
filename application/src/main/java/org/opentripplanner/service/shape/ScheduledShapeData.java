package org.opentripplanner.service.shape;

import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

/**
 * The shapes of the scheduled (non-real-time) trip patterns, and the shapes of the scheduled trips
 * whose shape differs from the shape of their trip pattern.
 * <p>
 * Immutable: it holds an immutable copy of the data it is created from and never changes, so it
 * can be shared freely, for example between threads.
 */
public final class ScheduledShapeData {

  private static final ScheduledShapeData EMPTY = new ScheduledShapeData(Map.of(), Map.of());

  private final Map<FeedScopedId, CompactLineStringSequence> shapesByTripPattern;
  private final Map<
    FeedScopedId,
    Map<FeedScopedId, CompactLineStringSequence>
  > shapesByTripByTripPattern;

  private ScheduledShapeData(
    Map<FeedScopedId, CompactLineStringSequence> shapesByTripPattern,
    Map<FeedScopedId, Map<FeedScopedId, CompactLineStringSequence>> shapesByTripByTripPattern
  ) {
    this.shapesByTripPattern = shapesByTripPattern;
    this.shapesByTripByTripPattern = shapesByTripByTripPattern;
  }

  /**
   * Create the immutable scheduled data. The given maps are copied, so later changes to them are
   * not reflected. The shape instances are shared, not copied.
   */
  public static ScheduledShapeData of(
    Map<FeedScopedId, CompactLineStringSequence> shapesByTripPattern,
    Map<FeedScopedId, Map<FeedScopedId, CompactLineStringSequence>> shapesByTripByTripPattern
  ) {
    return new ScheduledShapeData(
      Map.copyOf(shapesByTripPattern),
      shapesByTripByTripPattern
        .entrySet()
        .stream()
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Map.copyOf(e.getValue())))
    );
  }

  public static ScheduledShapeData empty() {
    return EMPTY;
  }

  @Nullable
  public CompactLineStringSequence getTripPatternShape(FeedScopedId tripPatternId) {
    return shapesByTripPattern.get(tripPatternId);
  }

  @Nullable
  public CompactLineStringSequence getTripShape(FeedScopedId tripPatternId, FeedScopedId tripId) {
    var tripShapes = shapesByTripByTripPattern.get(tripPatternId);
    if (tripShapes == null) {
      return null;
    }
    return tripShapes.get(tripId);
  }
}
