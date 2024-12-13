package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;

public class RemoveCarTransitIfDrivingIsBetterTest implements PlanTestConstants {

  @Test
  public void filterAwayNothingIfNoDriving() {
    // Given:
    RouteRequest request = new RouteRequest();
    request.journey().access().setMode(StreetMode.CAR);
    request.journey().egress().setMode(StreetMode.CAR);
    Itinerary i1 = newItinerary(A).withRequest(request).bus(21, 6, 7, E).build();
    Itinerary i2 = newItinerary(A).withRequest(request).rail(110, 6, 9, E).build();

    // When:
    List<Itinerary> result = new RemoveCarTransitIfDrivingIsBetter()
      .removeMatchesForTest(List.of(i1, i2));

    // Then:
    assertEquals(toStr(List.of(i1, i2)), toStr(result));
  }

  @Test
  public void filterAwayTransitWithLongerCarLeg() {
    RouteRequest request = new RouteRequest();
    request.journey().access().setMode(StreetMode.CAR);
    request.journey().egress().setMode(StreetMode.CAR);
    // a car itinerary that takes the board cost of using the train in other itineraries into consideration for the tests
    Itinerary car = newItinerary(A).drive(6, 10 + BOARD_COST, E).build();

    // a walk itinerary that will not be filtered
    Itinerary walk = newItinerary(A, 6).walk(D2m, E).build();

    // a transit itinerary with more cost than the car itinerary by itself should be dropped
    Itinerary i1 = newItinerary(A, 6).withRequest(request).drive(6, 9, D).rail(1, 9, 11, E).build();

    // a transit itinerary with less cost than the car itinerary by itself should be kept
    Itinerary i2 = newItinerary(A, 6).withRequest(request).drive(6, 8, D).rail(2, 8, 9, E).build();

    List<Itinerary> result = new RemoveCarTransitIfDrivingIsBetter()
      .removeMatchesForTest(List.of(i1, i2, car, walk));

    assertEquals(toStr(List.of(i2, car, walk)), toStr(result));
  }

  @Test
  public void filterAwayNothingIfDrivingButNoRequest() {
    // a car itinerary that takes the board cost of using the train in other itineraries into consideration for the tests
    Itinerary car = newItinerary(A).drive(6, 10 + BOARD_COST, E).build();

    // a walk itinerary that will not be filtered
    Itinerary walk = newItinerary(A, 6).walk(D2m, E).build();

    // should be kept
    Itinerary i1 = newItinerary(A, 6).withRequest(null).drive(6, 9, D).rail(1, 9, 11, E).build();

    // should be kept
    Itinerary i2 = newItinerary(A, 6).withRequest(null).drive(6, 8, D).rail(2, 8, 9, E).build();

    List<Itinerary> result = new RemoveCarTransitIfDrivingIsBetter()
      .removeMatchesForTest(List.of(i1, i2, car, walk));

    assertEquals(toStr(List.of(i1, i2, car, walk)), toStr(result));
  }
}
