package org.opentripplanner.transit.model.filter.transit;

import javax.annotation.Nullable;
import org.opentripplanner.routing.alertpatch.EntityKey;

/**
 * The type of entity affected by an alert, used when filtering alerts by the entities they affect.
 */
public enum AlertEntityType {
  AGENCY,
  PATTERN,
  ROUTE,
  ROUTE_TYPE,
  STOP,
  STOP_ON_ROUTE,
  STOP_ON_TRIP,
  TRIP;

  /**
   * Returns the type of the given entity key, or {@code null} if the key has no type which can be
   * filtered on.
   */
  @Nullable
  public static AlertEntityType of(EntityKey key) {
    return switch (key) {
      case EntityKey.Agency _ -> AGENCY;
      case EntityKey.DirectionAndRoute _ -> PATTERN;
      case EntityKey.Route _ -> ROUTE;
      case EntityKey.RouteType _ -> ROUTE_TYPE;
      case EntityKey.RouteTypeAndAgency _ -> ROUTE_TYPE;
      case EntityKey.Stop _ -> STOP;
      case EntityKey.StopAndRoute _ -> STOP_ON_ROUTE;
      case EntityKey.StopAndTrip _ -> STOP_ON_TRIP;
      case EntityKey.Trip _ -> TRIP;
      case EntityKey.Unknown _ -> null;
    };
  }
}
