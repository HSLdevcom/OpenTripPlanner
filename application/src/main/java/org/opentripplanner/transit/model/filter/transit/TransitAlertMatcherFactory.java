package org.opentripplanner.transit.model.filter.transit;

import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.GenericUnaryMatcher;
import org.opentripplanner.transit.model.filter.expr.Matcher;

/**
 * A factory for creating matchers for {@link TransitAlert}s.
 * <p>
 * The matcher is built from the alert-level criteria of a single {@link TransitAlertSelectRequest},
 * which are combined with AND logic. The entity criteria of the selector are not part of this
 * matcher, as they are resolved from the entities which alerts affect, see
 * {@link TransitAlertEntityMatcherFactory}.
 */
public class TransitAlertMatcherFactory {

  /**
   * Creates a matcher for the alert-level criteria of the given selector.
   */
  public static Matcher<TransitAlert> of(TransitAlertSelectRequest selector) {
    ExpressionBuilder<TransitAlert> expr = ExpressionBuilder.of();

    expr.atLeastOneMatch(selector.feeds(), TransitAlertMatcherFactory::feed);
    expr.atLeastOneMatch(selector.severityLevels(), TransitAlertMatcherFactory::severity);
    expr.atLeastOneMatch(selector.causes(), TransitAlertMatcherFactory::cause);
    expr.atLeastOneMatch(selector.effects(), TransitAlertMatcherFactory::effect);
    expr.atLeastOneMatch(selector.timePeriods(), TransitAlertMatcherFactory::timePeriod);

    return expr.build();
  }

  static Matcher<TransitAlert> feed(String feedId) {
    return new EqualityMatcher<>("feed", feedId, a -> a.getId().getFeedId());
  }

  static Matcher<TransitAlert> severity(AlertSeverity severity) {
    return new EqualityMatcher<>("severity", severity, TransitAlert::severity);
  }

  static Matcher<TransitAlert> cause(AlertCause cause) {
    return new EqualityMatcher<>("cause", cause, TransitAlert::cause);
  }

  static Matcher<TransitAlert> effect(AlertEffect effect) {
    return new EqualityMatcher<>("effect", effect, TransitAlert::effect);
  }

  static Matcher<TransitAlert> timePeriod(TimePeriod timePeriod) {
    return new GenericUnaryMatcher<>("timePeriod", alert ->
      alert.calendar().isActiveDuring(timePeriod)
    );
  }
}
