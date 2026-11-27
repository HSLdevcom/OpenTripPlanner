package org.opentripplanner.updater.trip.shape;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.shape.CompactLineStringSequence;
import org.opentripplanner.transit.model.shape.TripShapeFactory;

public class RealTimeTripShapeFactory {

  private final DeduplicatorService deduplicator;

  public RealTimeTripShapeFactory(DeduplicatorService deduplicator) {
    this.deduplicator = deduplicator;
  }

  /**
   * Create the shape of a real-time pattern from the pattern it replaces. Hops are compared by
   * position: a hop between the same stops keeps the original hop shape, a hop between other stops
   * of the same stations keeps it with its ends moved to the new stops, and any other hop is a
   * straight line.
   */
  public CompactLineStringSequence createTripPatternShape(
    StopPattern stopPattern,
    StopPattern originalStopPattern,
    CompactLineStringSequence originalTripPatternShape
  ) {
    return TripShapeFactory.fromLineStrings(
      generateHopShapesFromOriginalTripPatternShape(
        stopPattern,
        originalStopPattern,
        originalTripPatternShape
      ),
      deduplicator
    );
  }

  private static List<LineString> generateHopShapesFromOriginalTripPatternShape(
    StopPattern stopPattern,
    StopPattern originalStopPattern,
    CompactLineStringSequence originalTripPatternShape
  ) {
    List<LineString> hopShapes = new ArrayList<>();

    for (int i = 0; i < stopPattern.getSize() - 1; i++) {
      LineString hopShape =
        i < originalStopPattern.getSize() - 1 ? originalTripPatternShape.get(i) : null;

      if (hopShape != null && stopPattern.sameStops(originalStopPattern, i)) {
        // Copy hop shape from previous pattern
        hopShapes.add(hopShape);
      } else if (hopShape != null && stopPattern.sameStations(originalStopPattern, i)) {
        // Use old geometry but patch first and last point with new stops
        var newStart = stopPattern.getStop(i).getCoordinate().asJtsCoordinate();
        var newEnd = stopPattern
          .getStop(i + 1)
          .getCoordinate()
          .asJtsCoordinate();

        Coordinate[] coordinates = hopShape.getCoordinates();
        coordinates[0].setCoordinate(newStart);
        coordinates[coordinates.length - 1].setCoordinate(newEnd);

        hopShapes.add(GeometryUtils.getGeometryFactory().createLineString(coordinates));
      } else {
        // Create new straight-line geometry for hop
        hopShapes.add(
          GeometryUtils.getGeometryFactory().createLineString(new Coordinate[] {
            stopPattern.getStop(i).getCoordinate().asJtsCoordinate(),
            stopPattern
              .getStop(i + 1)
              .getCoordinate()
              .asJtsCoordinate(),
          })
        );
      }
    }
    return hopShapes;
  }
}
