package org.opentripplanner.transit.model.shape;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.network.StopPattern;

public final class TripShapeValidator {

  private TripShapeValidator() {}

  /**
   * Check that the shape exists and has one hop for each pair of consecutive stops of the stop
   * pattern.
   *
   * @throws IllegalArgumentException if the shape is missing, or if the number of hops does not
   *                                  match the stop pattern
   */
  public static void validate(
    StopPattern stopPattern,
    CompactLineStringSequence shape,
    FeedScopedId id
  ) {
    if (shape == null) {
      throw new IllegalArgumentException("No shape for " + id);
    }
    int expectedHops = Math.max(stopPattern.getSize() - 1, 0);
    if (shape.size() != expectedHops) {
      throw new IllegalArgumentException(
        "Shape of %s has %d hops, expected %d".formatted(id, shape.size(), expectedHops)
      );
    }
  }
}
