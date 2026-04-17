package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

public class CarpoolAccessEgress implements RoutingAccessEgress {

  /**
   * The Raptor departure time of this access/egress leg, in seconds since
   * {@code transitSearchTimeZero}. For a carpool leg this is the moment the car arrives at the
   * pickup: the passenger must be ready by this instant, since the driver is on a committed
   * schedule and cannot wait. The boarding dwell at the pickup is part of
   * {@link #durationInSeconds}, not of the time before departure.
   */
  private final int departureTimeOfPassenger;

  /**
   * The Raptor arrival time of this access/egress leg, in seconds since
   * {@code transitSearchTimeZero}. For a carpool leg this is the moment the car reaches the
   * dropoff (the transit stop for access, the passenger's destination for egress), after
   * boarding dwell and shared travel.
   */
  private final int arrivalTimeOfPassenger;
  private final int stop;
  private final int durationInSeconds;
  private final int c1;
  private final List<GraphPath<State, Edge, Vertex>> segments;
  private final TimeAndCost penalty;
  private final double totalWeight;
  private final double carpoolReluctance;

  public CarpoolAccessEgress(
    int stop,
    Duration duration,
    int departureTimeOfPassenger,
    int arrivalTimeOfPassenger,
    List<GraphPath<State, Edge, Vertex>> segments,
    TimeAndCost penalty,
    Double carpoolReluctance
  ) {
    this.departureTimeOfPassenger = departureTimeOfPassenger;
    this.arrivalTimeOfPassenger = arrivalTimeOfPassenger;
    this.stop = stop;
    this.durationInSeconds = (int) duration.getSeconds();
    this.carpoolReluctance = carpoolReluctance;
    this.totalWeight = this.durationInSeconds * carpoolReluctance;
    this.c1 = RaptorCostConverter.toRaptorCost(this.totalWeight);
    this.segments = segments;
    this.penalty = penalty;
  }

  @Override
  public int stop() {
    return this.stop;
  }

  @Override
  public int c1() {
    return this.c1;
  }

  @Override
  public int durationInSeconds() {
    return this.durationInSeconds;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    if (requestedDepartureTime > departureTimeOfPassenger) {
      return RaptorConstants.TIME_NOT_SET;
    }
    return departureTimeOfPassenger;
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    if (requestedArrivalTime < arrivalTimeOfPassenger) {
      return RaptorConstants.TIME_NOT_SET;
    }
    return arrivalTimeOfPassenger;
  }

  @Override
  public boolean hasOpeningHours() {
    return true;
  }

  public int getDepartureTimeOfPassenger() {
    return departureTimeOfPassenger;
  }

  public int getArrivalTimeOfPassenger() {
    return arrivalTimeOfPassenger;
  }

  @Override
  public RoutingAccessEgress withPenalty(TimeAndCost penalty) {
    return new CarpoolAccessEgress(
      this.stop,
      Duration.ofSeconds(this.durationInSeconds),
      this.departureTimeOfPassenger,
      this.arrivalTimeOfPassenger,
      this.segments,
      penalty,
      this.carpoolReluctance
    );
  }

  /*
    TODO: Implement this function.
    It is never used for instances of CarpoolAccessEgress, but this might change in the future.
   */
  @Override
  public List<State> getFinalStates() {
    throw new UnsupportedOperationException(
      "Fetching last state of CarpoolAccessEgress is not yet implemented"
    );
  }

  @Override
  public boolean isWalkOnly() {
    return false;
  }

  @Override
  public TimeAndCost penalty() {
    return this.penalty;
  }

  public List<GraphPath<State, Edge, Vertex>> getSegments() {
    return this.segments;
  }

  public double getTotalWeight() {
    return this.totalWeight;
  }
}
