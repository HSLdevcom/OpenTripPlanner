package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * Filter itineraries with BIKE access and egress modes which have a higher generalized-cost than the bicycle-only itinerary (if it exists).
 */
public class RemoveBicycleTransitIfCyclingIsBetter implements RemoveItineraryFlagger {

  /**
   * Required for {@link org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChain},
   * to know which filters removed
   */
  public static final String TAG = "bicycle-transit-vs-bicycle-filter";

  @Override
  public String name() {
    return TAG;
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    OptionalInt minBicycleCost = itineraries
      .stream()
      .filter(Itinerary::isCyclingAllTheWay)
      .mapToInt(Itinerary::getGeneralizedCost)
      .min();

    if (minBicycleCost.isEmpty()) {
      return List.of();
    }

    var limit = minBicycleCost.getAsInt();

    return itineraries
      .stream()
      // we use the cost without the access/egress penalty since we don't want to give
      // searches that are only on the street network an unfair advantage
      .filter(it ->
        !it.isOnStreetAllTheWay() &&
        // Only if both access and egress modes are BIKE should the itinerary be filtered
        it.getRequest().journey().access().mode() == StreetMode.BIKE &&
        it.getRequest().journey().egress().mode() == StreetMode.BIKE &&
        it.getGeneralizedCost() >= limit)
      .collect(Collectors.toList());
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }
}
