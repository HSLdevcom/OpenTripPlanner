package org.opentripplanner.service.shape.internal;

import org.opentripplanner.core.model.basic.Distance;
import org.opentripplanner.service.shape.ShapeService;
import org.opentripplanner.service.shape.ShapeSnapshot;
import org.opentripplanner.service.shape.model.HopGeometrySequence;
import org.opentripplanner.service.shape.model.HopGeometrySequenceGroup;
import org.opentripplanner.service.shape.model.TripGeometry;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;

/**
 * SKELETON: constructor shape only. Method bodies are not implemented in this phase.
 * <p>
 * A request-scoped view over a {@link ShapeSnapshot}. A new instance should be created for each
 * request, with the request's {@link TransitService} and a shape snapshot resolved from the same
 * transaction scope, so that the whole request sees one consistent view.
 * <p>
 * The {@link TransitService} is what separates this class from the snapshot it wraps: resolving a
 * trip to its pattern, and a pattern created by a real-time update back to the scheduled pattern it
 * was derived from, both need the transit model. The snapshot deliberately knows nothing about it.
 */
public class DefaultShapeService implements ShapeService {

  private final ShapeSnapshot snapshot;
  private final TransitService transitService;

  public DefaultShapeService(ShapeSnapshot snapshot, TransitService transitService) {
    this.snapshot = snapshot;
    this.transitService = transitService;
  }

  @Override
  public HopGeometrySequence getDefaultPatternGeometry(TripPattern pattern) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public HopGeometrySequenceGroup getPatternGeometries(TripPattern pattern) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public TripGeometry getTripGeometry(Trip trip) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public HopGeometrySequence defaultGeometryBetween(
    TripPattern pattern,
    int boardingStopPosition,
    int alightingStopPosition
  ) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public HopGeometrySequenceGroup geometriesBetween(
    TripPattern pattern,
    int boardingStopPosition,
    int alightingStopPosition
  ) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public HopGeometrySequence geometryBetween(
    Trip trip,
    int boardingStopPosition,
    int alightingStopPosition
  ) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public Distance distanceBetween(Trip trip, int boardingStopPosition, int alightingStopPosition) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public int distanceMetersBetween(Trip trip, int boardingStopPosition, int alightingStopPosition) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }

  @Override
  public int defaultDistanceMetersBetween(
    TripPattern pattern,
    int boardingStopPosition,
    int alightingStopPosition
  ) {
    throw new UnsupportedOperationException("Not implemented - API design only");
  }
}
