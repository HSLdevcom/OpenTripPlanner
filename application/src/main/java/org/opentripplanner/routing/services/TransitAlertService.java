package org.opentripplanner.routing.services;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.EntityKey;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.api.request.TransitAlertRequest;
import org.opentripplanner.transit.model.filter.selector.SelectorBasedMatcherFactory;
import org.opentripplanner.transit.model.filter.transit.TransitAlertEntityMatcherFactory;
import org.opentripplanner.transit.model.filter.transit.TransitAlertEntityResolver;
import org.opentripplanner.transit.model.filter.transit.TransitAlertMatcherFactory;
import org.opentripplanner.transit.model.filter.transit.TransitAlertSelectRequest;
import org.opentripplanner.transit.model.timetable.Direction;

/**
 * A TransitAlertService stores a set of alerts (passenger-facing textual information associated
 * with transit entities such as stops or routes) which are currently active and should be provided
 * to end users when their itineraries include the relevant stop, route, etc.
 *
 * Its primary purpose is to index those alerts, which may be numerous, so they can be looked up
 * rapidly and attached to the various pieces of an itinerary as it's being returned to the user.
 *
 * Most elements in an itinerary will have no alerts attached, so those cases need to return
 * quickly. For example, no alerts on board stop A, no alerts on route 1 ridden, no alerts on alight
 * stop B, no alerts on route 2 ridden, yes one alert found on alight stop C.
 *
 * The fact that alerts are relatively sparse (at the scale of the entire transportation system)
 * is central to this implementation. Adding a list of alerts to every element in the system would
 * mean storing large amounts of null or empty list references. Instead, alerts are looked up in
 * maps allowing them to be attached to any object with minimal space overhead, but requiring some
 * careful indexing to ensure their presence or absence on each object can be determined quickly.
 */
public interface TransitAlertService {
  void setAlerts(Collection<TransitAlert> alerts);

  Collection<TransitAlert> getAllAlerts();

  /**
   * Returns the keys of all entities which have at least one alert.
   */
  Collection<EntityKey> listEntityKeys();

  /**
   * Returns the alerts of the given entities. The returned collection contains no duplicates, even
   * if an alert affects several of the given entities.
   */
  Collection<TransitAlert> findAlerts(Collection<EntityKey> entityKeys);

  /**
   * Returns all alerts matching the given request. A request without filters matches all alerts.
   * <p>
   * Each selector of the request is resolved on its own: the entities which have alerts are
   * filtered with the {@link TransitAlertEntityMatcherFactory}, the alerts of the selected entities
   * are fetched, and those alerts are filtered with the {@link TransitAlertMatcherFactory}. The
   * resulting alerts of the selectors are then combined with the select/not semantics of the
   * filters.
   *
   * @param entityResolver resolves which entities are selected by the entity criteria.
   */
  default Collection<TransitAlert> findAlerts(
    TransitAlertRequest request,
    TransitAlertEntityResolver entityResolver
  ) {
    if (request.filters().isEmpty()) {
      return getAllAlerts();
    }

    Map<TransitAlertSelectRequest, Set<TransitAlert>> selectedAlerts = new HashMap<>();
    for (var filter : request.filters()) {
      Stream.of(filter.select(), filter.not())
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .forEach(selector ->
          selectedAlerts.computeIfAbsent(selector, s -> findSelectedAlerts(s, entityResolver))
        );
    }

    var matcher = SelectorBasedMatcherFactory.<TransitAlert, TransitAlertSelectRequest>of(
      request.filters(),
      selector -> selectedAlerts.get(selector)::contains
    );

    // Only the alerts selected by the includes can match, unless a filter has no includes
    Collection<TransitAlert> candidates = request
      .filters()
      .stream()
      .allMatch(filter -> filter.select() != null)
      ? request
          .filters()
          .stream()
          .flatMap(filter -> filter.select().stream())
          .flatMap(selector -> selectedAlerts.get(selector).stream())
          .collect(Collectors.toSet())
      : getAllAlerts();

    return candidates.stream().filter(matcher::match).toList();
  }

  /**
   * Returns the alerts selected by a single selector, without duplicates.
   */
  private Set<TransitAlert> findSelectedAlerts(
    TransitAlertSelectRequest selector,
    TransitAlertEntityResolver entityResolver
  ) {
    Collection<TransitAlert> entityAlerts;
    if (selector.entities().includeEverything()) {
      entityAlerts = getAllAlerts();
    } else {
      var entityMatcher = TransitAlertEntityMatcherFactory.of(selector, entityResolver);
      entityAlerts = findAlerts(listEntityKeys().stream().filter(entityMatcher::match).toList());
    }
    var alertMatcher = TransitAlertMatcherFactory.of(selector);
    return entityAlerts.stream().filter(alertMatcher::match).collect(Collectors.toSet());
  }

  TransitAlert getAlertById(FeedScopedId id);

  default Collection<TransitAlert> getStopAlerts(FeedScopedId stop) {
    return getStopAlerts(stop, Set.of());
  }

  /**
   * Returns the alerts for the exact stop only. Alerts on the parent station (or any other related
   * stop) are not included; use {@link #getStopLocationsAlerts} to get alerts for multiple
   * locations at once.
   */
  Collection<TransitAlert> getStopAlerts(FeedScopedId stop, Set<StopCondition> stopConditions);

  /**
   * Returns the alerts for the stop locations.
   */
  Set<TransitAlert> getStopLocationsAlerts(List<FeedScopedId> stopLocationIds);

  Collection<TransitAlert> getRouteAlerts(FeedScopedId route);

  /**
   * Get Trip alerts for any date
   */
  Collection<TransitAlert> getTripAlerts(FeedScopedId trip);

  Collection<TransitAlert> getTripAlerts(FeedScopedId trip, LocalDate serviceDate);

  Collection<TransitAlert> getAgencyAlerts(FeedScopedId agency);

  default Collection<TransitAlert> getStopAndRouteAlerts(FeedScopedId stop, FeedScopedId route) {
    return getStopAndRouteAlerts(stop, route, Set.of(), Direction.UNKNOWN);
  }

  Collection<TransitAlert> getStopAndRouteAlerts(
    FeedScopedId stop,
    FeedScopedId route,
    Set<StopCondition> stopConditions,
    Direction direction
  );

  default Collection<TransitAlert> getStopAndTripAlerts(
    FeedScopedId stop,
    FeedScopedId trip,
    LocalDate serviceDate
  ) {
    return getStopAndTripAlerts(stop, trip, serviceDate, Set.of());
  }

  Collection<TransitAlert> getStopAndTripAlerts(
    FeedScopedId stop,
    FeedScopedId trip,
    LocalDate serviceDate,
    Set<StopCondition> stopConditions
  );

  Collection<TransitAlert> getRouteTypeAndAgencyAlerts(int routeType, FeedScopedId agency);

  Collection<TransitAlert> getRouteTypeAlerts(int routeType, String feedId);

  Collection<TransitAlert> getDirectionAndRouteAlerts(Direction direction, FeedScopedId route);
}
