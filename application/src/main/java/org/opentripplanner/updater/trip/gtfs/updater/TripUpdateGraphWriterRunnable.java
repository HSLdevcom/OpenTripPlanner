package org.opentripplanner.updater.trip.gtfs.updater;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.TransitRealTimeUpdateContext;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.gtfs.GtfsRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.trip.gtfs.interpolation.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.interpolation.ForwardsDelayPropagationType;

public class TripUpdateGraphWriterRunnable
  implements GraphWriterRunnable<TransitRealTimeUpdateContext> {

  private final UpdateIncrementality updateIncrementality;

  /**
   * The list with updates to apply to the graph
   */
  private final List<TripUpdate> updates;

  /**
   * Standalone GTFS-RT {@code Shape} FeedEntities from the same message as {@link #updates},
   * keyed by {@code shape_id}.
   */
  private final Map<String, LineString> shapesByShapeId;

  private final boolean fuzzyTripMatching;

  private final ForwardsDelayPropagationType forwardsDelayPropagationType;
  private final BackwardsDelayPropagationType backwardsDelayPropagationType;

  private final String feedId;
  private final Consumer<UpdateResult> sendMetrics;
  private final GtfsRealTimeTripUpdateAdapter adapter;

  public TripUpdateGraphWriterRunnable(
    GtfsRealTimeTripUpdateAdapter adapter,
    boolean fuzzyTripMatching,
    ForwardsDelayPropagationType forwardsDelayPropagationType,
    BackwardsDelayPropagationType backwardsDelayPropagationType,
    UpdateIncrementality updateIncrementality,
    List<TripUpdate> updates,
    Map<String, LineString> shapesByShapeId,
    String feedId,
    Consumer<UpdateResult> sendMetrics
  ) {
    this.adapter = adapter;
    this.fuzzyTripMatching = fuzzyTripMatching;
    this.forwardsDelayPropagationType = forwardsDelayPropagationType;
    this.backwardsDelayPropagationType = backwardsDelayPropagationType;
    this.updateIncrementality = updateIncrementality;
    this.updates = Objects.requireNonNull(updates);
    this.shapesByShapeId = Objects.requireNonNullElse(shapesByShapeId, Map.of());
    this.feedId = Objects.requireNonNull(feedId);
    this.sendMetrics = sendMetrics;
  }

  @Override
  public void run(TransitRealTimeUpdateContext context) {
    var result = adapter
      .forUpdate(context.timetableRepository())
      .applyTripUpdates(
        fuzzyTripMatching ? context.gtfsRealtimeFuzzyTripMatcher() : null,
        forwardsDelayPropagationType,
        backwardsDelayPropagationType,
        updateIncrementality,
        updates,
        shapesByShapeId,
        feedId
      );
    sendMetrics.accept(result);
  }
}
