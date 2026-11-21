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

  private final Map<FeedScopedId, CompactLineStringSequence> geometriesByTripPattern;
  private final Map<
    FeedScopedId,
    Map<FeedScopedId, CompactLineStringSequence>
  > geometriesByTripByTripPattern;

  private ScheduledShapeData(
    Map<FeedScopedId, CompactLineStringSequence> geometriesByTripPattern,
    Map<FeedScopedId, Map<FeedScopedId, CompactLineStringSequence>> geometriesByTripByTripPattern
  ) {
    this.geometriesByTripPattern = geometriesByTripPattern;
    this.geometriesByTripByTripPattern = geometriesByTripByTripPattern;
  }

  /**
   * Create the immutable scheduled data. The given maps are copied, so later changes to them are
   * not reflected. The shape instances are shared, not copied.
   */
  public static ScheduledShapeData of(
    Map<FeedScopedId, CompactLineStringSequence> geometriesByTripPattern,
    Map<FeedScopedId, Map<FeedScopedId, CompactLineStringSequence>> geometriesByTripByTripPattern
  ) {
    return new ScheduledShapeData(
      Map.copyOf(geometriesByTripPattern),
      geometriesByTripByTripPattern
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
    return geometriesByTripPattern.get(tripPatternId);
  }

  @Nullable
  public CompactLineStringSequence getTripShape(FeedScopedId tripPatternId, FeedScopedId tripId) {
    var tripShapes = geometriesByTripByTripPattern.get(tripPatternId);
    if (tripShapes == null) {
      return null;
    }
    return tripShapes.get(tripId);
  }
}
