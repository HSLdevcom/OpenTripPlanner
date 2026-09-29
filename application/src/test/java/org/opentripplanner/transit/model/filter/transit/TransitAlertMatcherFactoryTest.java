package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.routing.alertpatch.AlertCalendar;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;

class TransitAlertMatcherFactoryTest {

  private static final FeedScopedId ROUTE_ID = id("F:R1");

  private static final TransitAlert ALERT = TransitAlert.of(id("F:A1"))
    .addEntity(new EntitySelector.Route(ROUTE_ID))
    .withSeverity(AlertSeverity.SEVERE)
    .withCause(AlertCause.WEATHER)
    .withEffect(AlertEffect.NO_SERVICE)
    .withCalendar(
      AlertCalendar.of(TimePeriod.of(Instant.ofEpochSecond(0), Instant.ofEpochSecond(1_000)))
    )
    .build();

  @Test
  void feed() {
    assertTrue(matches(TransitAlertSelectRequest.of().withFeeds(List.of("F"))));
    assertFalse(matches(TransitAlertSelectRequest.of().withFeeds(List.of("OTHER"))));
  }

  @Test
  void severity() {
    assertTrue(
      matches(TransitAlertSelectRequest.of().withSeverityLevels(List.of(AlertSeverity.SEVERE)))
    );
    assertFalse(
      matches(TransitAlertSelectRequest.of().withSeverityLevels(List.of(AlertSeverity.INFO)))
    );
  }

  @Test
  void cause() {
    assertTrue(matches(TransitAlertSelectRequest.of().withCauses(List.of(AlertCause.WEATHER))));
    assertFalse(matches(TransitAlertSelectRequest.of().withCauses(List.of(AlertCause.ACCIDENT))));
  }

  @Test
  void effect() {
    assertTrue(
      matches(TransitAlertSelectRequest.of().withEffects(List.of(AlertEffect.NO_SERVICE)))
    );
    assertFalse(matches(TransitAlertSelectRequest.of().withEffects(List.of(AlertEffect.DETOUR))));
  }

  @Test
  void timePeriod() {
    var overlapping = TimePeriod.of(Instant.ofEpochSecond(500), Instant.ofEpochSecond(2_000));
    var later = TimePeriod.of(Instant.ofEpochSecond(2_000), Instant.ofEpochSecond(3_000));

    assertTrue(matches(TransitAlertSelectRequest.of().withTimePeriods(List.of(overlapping))));
    assertFalse(matches(TransitAlertSelectRequest.of().withTimePeriods(List.of(later))));
  }

  @Test
  void valuesOfADimensionAreCombinedWithOr() {
    assertTrue(
      matches(
        TransitAlertSelectRequest.of().withCauses(List.of(AlertCause.ACCIDENT, AlertCause.WEATHER))
      )
    );
  }

  @Test
  void dimensionsAreCombinedWithAnd() {
    assertTrue(
      matches(
        TransitAlertSelectRequest.of()
          .withSeverityLevels(List.of(AlertSeverity.SEVERE))
          .withCauses(List.of(AlertCause.WEATHER))
          .withEffects(List.of(AlertEffect.NO_SERVICE))
      )
    );
    assertFalse(
      matches(
        TransitAlertSelectRequest.of()
          .withSeverityLevels(List.of(AlertSeverity.SEVERE))
          .withCauses(List.of(AlertCause.ACCIDENT))
      )
    );
  }

  @Test
  void emptySelectorMatchesEverything() {
    assertTrue(matches(TransitAlertSelectRequest.of()));
  }

  /**
   * Entity criteria are resolved by the {@link TransitAlertEntityMatcherFactory}, so they don't
   * affect the alert matcher.
   */
  @Test
  void entityCriteriaAreIgnored() {
    var otherRoute = TransitAlertEntitySelectRequest.of()
      .withRoutes(
        List.of(
          TransitAlertRouteSelectRequest.of()
            .withIds(List.of(id("F:OTHER")))
            .build()
        )
      )
      .build();

    assertTrue(matches(TransitAlertSelectRequest.of().withEntities(List.of(otherRoute))));
  }

  private static boolean matches(TransitAlertSelectRequest.Builder selector) {
    return TransitAlertMatcherFactory.of(selector.build()).match(ALERT);
  }
}
