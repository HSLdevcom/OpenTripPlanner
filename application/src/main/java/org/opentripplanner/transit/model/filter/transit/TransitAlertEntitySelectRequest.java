package org.opentripplanner.transit.model.filter.transit;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Selects the entities that alerts affect. An entity is selected if it is of one of the
 * {@link #entityTypes()} and it is related to both the {@link #routes()} and the
 * {@link #stopsOrStations()}, if they are set. Within the route and stop lists, the selectors
 * are combined with OR logic.
 * <p>
 * If no entity types are given, only the types which directly refer to the selected routes and
 * stops are used, see {@link #entityTypes()}.
 */
public class TransitAlertEntitySelectRequest {

  private final Set<AlertEntityType> entityTypes;
  private final FilterValues<TransitAlertRouteSelectRequest> routes;
  private final FilterValues<TransitAlertStopOrStationSelectRequest> stopsOrStations;

  private TransitAlertEntitySelectRequest(Builder builder) {
    this.routes = FilterValues.ofNullIsEverything("routes", builder.routes);
    this.stopsOrStations = FilterValues.ofNullIsEverything(
      "stopsOrStations",
      builder.stopsOrStations
    );
    if (builder.entityTypes != null) {
      if (builder.entityTypes.isEmpty()) {
        throw new IllegalArgumentException("'entityTypes' must not be empty.");
      }
      this.entityTypes = Set.copyOf(builder.entityTypes);
    } else {
      this.entityTypes = defaultEntityTypes(
        !routes.includeEverything(),
        !stopsOrStations.includeEverything()
      );
    }
  }

  public static Builder of() {
    return new Builder();
  }

  /**
   * The types of entities to select. If not set explicitly, the default is based on which of the
   * routes and stops are set: {@link AlertEntityType#ROUTE} for routes,
   * {@link AlertEntityType#STOP} for stops and {@link AlertEntityType#STOP_ON_ROUTE} for both.
   */
  public Set<AlertEntityType> entityTypes() {
    return entityTypes;
  }

  public FilterValues<TransitAlertRouteSelectRequest> routes() {
    return routes;
  }

  public FilterValues<TransitAlertStopOrStationSelectRequest> stopsOrStations() {
    return stopsOrStations;
  }

  private static Set<AlertEntityType> defaultEntityTypes(boolean hasRoutes, boolean hasStops) {
    if (hasRoutes && hasStops) {
      return EnumSet.of(AlertEntityType.STOP_ON_ROUTE);
    }
    if (hasRoutes) {
      return EnumSet.of(AlertEntityType.ROUTE);
    }
    if (hasStops) {
      return EnumSet.of(AlertEntityType.STOP);
    }
    throw new IllegalArgumentException(
      "At least one of 'entityTypes', 'routes' and 'stopsOrStations' must be set."
    );
  }

  @Override
  public String toString() {
    var builder = ToStringBuilder.ofEmbeddedType().addCol("entityTypes", entityTypes);
    if (!routes.includeEverything()) {
      builder.addCol("routes", routes.get());
    }
    if (!stopsOrStations.includeEverything()) {
      builder.addCol("stopsOrStations", stopsOrStations.get());
    }
    return builder.toString();
  }

  public static class Builder {

    @Nullable
    private List<AlertEntityType> entityTypes;

    @Nullable
    private List<TransitAlertRouteSelectRequest> routes;

    @Nullable
    private List<TransitAlertStopOrStationSelectRequest> stopsOrStations;

    public Builder withEntityTypes(@Nullable List<AlertEntityType> entityTypes) {
      this.entityTypes = entityTypes;
      return this;
    }

    public Builder withRoutes(@Nullable List<TransitAlertRouteSelectRequest> routes) {
      this.routes = routes;
      return this;
    }

    public Builder withStopsOrStations(
      @Nullable List<TransitAlertStopOrStationSelectRequest> stopsOrStations
    ) {
      this.stopsOrStations = stopsOrStations;
      return this;
    }

    public TransitAlertEntitySelectRequest build() {
      return new TransitAlertEntitySelectRequest(this);
    }
  }
}
