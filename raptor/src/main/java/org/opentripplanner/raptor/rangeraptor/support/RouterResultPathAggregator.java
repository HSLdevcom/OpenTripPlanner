package org.opentripplanner.raptor.rangeraptor.support;

import java.util.Collection;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.SingleCriteriaStopArrivals;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

public class RouterResultPathAggregator<T extends RaptorTripSchedule>
  implements RaptorRouterResult<T> {

  private final RaptorRouterResult<T> master;
  private final ParetoSet<RaptorPath<T>> paths;

  public RouterResultPathAggregator(
    Collection<RaptorRouterResult<T>> results,
    ParetoComparator<RaptorPath<T>> comparator
  ) {
    this.paths = ParetoSet.of(comparator);
    RaptorRouterResult<T> first = null;
    for (var it : results) {
      if (first == null) {
        first = it;
      }
      this.paths.addAll(it.extractPaths());
    }
    this.master = first;
  }

  @Override
  public Collection<RaptorPath<T>> extractPaths() {
    return paths.stream().toList();
  }

  @Override
  public SingleCriteriaStopArrivals extractBestOverallArrivals() {
    return master.extractBestOverallArrivals();
  }

  @Override
  public SingleCriteriaStopArrivals extractBestTransitArrivals() {
    return master.extractBestTransitArrivals();
  }

  @Override
  public SingleCriteriaStopArrivals extractBestNumberOfTransfers() {
    return master.extractBestNumberOfTransfers();
  }

  @Override
  public boolean isDestinationReached() {
    return !paths.isEmpty();
  }
}
