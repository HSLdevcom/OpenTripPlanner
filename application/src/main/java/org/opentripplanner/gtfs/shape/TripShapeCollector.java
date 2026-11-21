package org.opentripplanner.gtfs.shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.module.geometry.GeometryProcessor;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripShapeFactory;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Collects the shape of each trip while the trip patterns are generated, and chooses the shape of
 * each trip pattern from the shapes of its trips.
 */
public final class TripShapeCollector {

  private final GeometryProcessor geometryProcessor;
  private final DeduplicatorService deduplicator;

  private final Map<TripShapeKey, CompactLineStringSequence> tripShapeCache = new HashMap<>();

  private final Map<
    FeedScopedId,
    Map<CompactLineStringSequence, List<FeedScopedId>>
  > tripsByShapeByTripPattern = new HashMap<>();

  private int numberOfTripsWithDifferentShape = 0;
  private int numberOfTripPatternsWithDifferentTripShapes = 0;

  public TripShapeCollector(GeometryProcessor geometryProcessor, DeduplicatorService deduplicator) {
    this.geometryProcessor = geometryProcessor;
    this.deduplicator = deduplicator;
  }

  public int numberOfTripsWithDifferentShape() {
    return numberOfTripsWithDifferentShape;
  }

  public int numberOfTripPatternsWithDifferentTripShapes() {
    return numberOfTripPatternsWithDifferentTripShapes;
  }

  public int numberOfComputedTripShapes() {
    return tripShapeCache.size();
  }

  public void addTrip(
    FeedScopedId tripPatternId,
    Trip trip,
    StopPattern stopPattern,
    List<StopTime> stopTimes
  ) {
    CompactLineStringSequence tripShape = tripShapeCache.computeIfAbsent(
      new TripShapeKey(trip.getShapeId(), stopPattern, shapeDistTraveled(stopTimes)),
      key -> TripShapeFactory.fromLineStrings(geometryProcessor.createHopShapes(trip), deduplicator)
    );
    tripsByShapeByTripPattern
      .computeIfAbsent(tripPatternId, id -> new HashMap<>())
      .computeIfAbsent(tripShape, shape -> new ArrayList<>())
      .add(trip.getId());
  }

  /**
   * Add the shape used by most trips of the pattern, and the shapes of the trips that differ from
   * it, to the data import.
   */
  public void addShapesToDataImport(TripPattern tripPattern, TransitDataImportBuilder dataImport) {
    FeedScopedId tripPatternId = tripPattern.getId();
    Map<CompactLineStringSequence, List<FeedScopedId>> tripsByShape = tripsByShapeByTripPattern.get(
      tripPatternId
    );
    CompactLineStringSequence tripPatternShape = mostUsedShape(tripsByShape);
    dataImport.getShapesByTripPattern().put(tripPatternId, tripPatternShape);

    Map<FeedScopedId, CompactLineStringSequence> differingShapesByTrip = new HashMap<>();
    for (var entry : tripsByShape.entrySet()) {
      CompactLineStringSequence tripShape = entry.getKey();
      if (!tripShape.equals(tripPatternShape)) {
        for (FeedScopedId tripId : entry.getValue()) {
          differingShapesByTrip.put(tripId, tripShape);
        }
      }
    }
    if (!differingShapesByTrip.isEmpty()) {
      dataImport.getShapesByTripByTripPattern().put(tripPatternId, differingShapesByTrip);
      numberOfTripsWithDifferentShape += differingShapesByTrip.size();
      numberOfTripPatternsWithDifferentTripShapes++;
    }
  }

  private static CompactLineStringSequence mostUsedShape(
    Map<CompactLineStringSequence, List<FeedScopedId>> tripsByShape
  ) {
    var entries = tripsByShape.entrySet().iterator();
    var mostUsed = entries.next();
    while (entries.hasNext()) {
      var entry = entries.next();
      List<FeedScopedId> trips = entry.getValue();
      List<FeedScopedId> mostUsedTrips = mostUsed.getValue();
      if (
        trips.size() > mostUsedTrips.size() ||
        // Tie break: choose the shape of the trip with the lowest id so the result is reproducible
        (trips.size() == mostUsedTrips.size() &&
          Collections.min(trips).compareTo(Collections.min(mostUsedTrips)) < 0)
      ) {
        mostUsed = entry;
      }
    }
    return mostUsed.getKey();
  }

  @Nullable
  private static List<Double> shapeDistTraveled(List<StopTime> stopTimes) {
    if (!stopTimes.getFirst().isShapeDistTraveledSet()) {
      return null;
    }
    return stopTimes.stream().map(StopTime::getShapeDistTraveled).toList();
  }

  private record TripShapeKey(
    @Nullable FeedScopedId shapeId,
    StopPattern stopPattern,
    @Nullable List<Double> shapeDistTraveled
  ) {}
}
