package org.opentripplanner.raptor.rangeraptor;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.RangeRaptorWorker;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;
import org.opentripplanner.raptor.rangeraptor.support.RouterResultPathAggregator;
import org.opentripplanner.raptor.util.composite.CompositeUtil;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * Iterate over two RR workers. The head should process the access and the tail should produce the
 * result. Paths from the head needs to propagate to the tail - this is NOT part of the
 * responsibilities for this class.
 */
public class RangeRaptorWorkerComposite<T extends RaptorTripSchedule>
  implements RangeRaptorWorker<T> {

  private final List<RangeRaptorWorker<T>> children;
  private final ParetoComparator<RaptorPath<T>> comparator;
  private RaptorRouterResult<T> result = null;

  public RangeRaptorWorkerComposite(
    List<RangeRaptorWorker<T>> children,
    ParetoComparator<RaptorPath<T>> comparator
  ) {
    this.children = children;
    this.comparator = comparator;
  }

  /**
   * Concatenate the two given workers, flattening any composite workers into a list.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T extends RaptorTripSchedule> RangeRaptorWorker<T> of(
    ParetoComparator<RaptorPath<T>> comparator,
    @Nullable RangeRaptorWorker<T> a,
    @Nullable RangeRaptorWorker<T> b
  ) {
    return CompositeUtil.of(
      children -> new RangeRaptorWorkerComposite(children, comparator),
      it -> it instanceof RangeRaptorWorkerComposite<T>,
      it -> ((RangeRaptorWorkerComposite) it).children,
      a,
      b
    );
  }

  @Override
  public RaptorRouterResult<T> result() {
    if (result == null) {
      this.result = new RouterResultPathAggregator(
        children.stream().map(RangeRaptorWorker::result).toList(),
        comparator
      );
    }
    return result;
  }

  @Override
  public boolean hasMoreRounds() {
    for (RangeRaptorWorker<T> child : children) {
      if (child.hasMoreRounds()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void findTransitForRound() {
    for (RangeRaptorWorker<T> child : children) {
      child.findTransitForRound();
    }
  }

  @Override
  public void findTransfersForRound() {
    for (RangeRaptorWorker<T> child : children) {
      child.findTransfersForRound();
    }
  }

  @Override
  public boolean isDestinationReachedInCurrentRound() {
    for (RangeRaptorWorker<T> child : children) {
      if (child.isDestinationReachedInCurrentRound()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void findAccessOnStreetForRound() {
    for (RangeRaptorWorker<T> child : children) {
      child.findAccessOnStreetForRound();
    }
  }

  @Override
  public void findAccessOnBoardForRound() {
    for (RangeRaptorWorker<T> child : children) {
      child.findAccessOnBoardForRound();
    }
  }
}
