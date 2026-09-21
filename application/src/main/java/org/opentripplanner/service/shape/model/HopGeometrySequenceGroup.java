package org.opentripplanner.service.shape.model;

import java.util.List;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * Every distinct geometry of one trip pattern, optionally restricted to a range of stop positions.
 * <p>
 * This is the answer to "what does this pattern actually look like on the map", and it replaces the
 * single {@code LineString} that the APIs can ask for today. For a pattern whose trips all follow
 * the same shape — the overwhelmingly common case — it holds exactly one variant and is no more
 * expensive to produce than the current single-geometry accessor.
 * <p>
 * It is deliberately a list of <em>distinct</em> geometries rather than one entry per trip. A
 * pattern with four hundred trips and two shapes yields two variants, not four hundred: the trips
 * are carried inside {@link ShapeVariant#tripIds()}, so nothing is lost, but callers that only want
 * to draw the pattern do not have to deduplicate first.
 * <p>
 * PROVISIONAL: this type is part of the initial {@code ShapeService} API design and may change
 * before the service is wired up.
 *
 * @param patternId the pattern these variants belong to.
 * @param variants  the distinct geometries, ordered most-used first so that
 *                  {@code variants().getFirst()} is the default. Never empty for a pattern with at
 *                  least two stops; empty for a degenerate pattern with no hops.
 */
public record HopGeometrySequenceGroup(FeedScopedId patternId, List<ShapeVariant> variants) {
  /**
   * The variant representing the pattern as a whole, i.e. the one used by the most trips.
   * <p>
   * Empty only for a degenerate pattern with no hops.
   */
  public Optional<ShapeVariant> defaultVariant() {
    return variants.stream().filter(ShapeVariant::isDefault).findFirst();
  }

  /**
   * Whether the trips of this pattern disagree about their geometry. Useful for data-quality
   * reporting: in a well-formed feed this is usually {@code false}, and when it is {@code true} the
   * default variant is only an approximation for some of the trips.
   */
  public boolean hasMultipleVariants() {
    return variants.size() > 1;
  }
}
