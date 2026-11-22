package org.opentripplanner.service.shape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.service.shape.dataimport.ShapeDataImport;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripShapeFactory;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Test helper for creating shape data. Patterns that are not built from shape data get a straight
 * line between each pair of stops. Use {@code TestServerContext.createShapeService} to serve the
 * data.
 */
public class ShapeTestFactory {

  private ShapeTestFactory() {}

  /**
   * A short north-south shape through the given number of stops, starting at the given latitude.
   * Different latitudes give different shapes, which makes them easy to tell apart in tests.
   */
  public static CompactLineStringSequence straightLine(double lat, int numberOfStops) {
    var testModel = TransitRepositoryForTest.of();
    List<StopLocation> stops = new ArrayList<>();
    for (int i = 0; i < numberOfStops; i++) {
      stops.add(testModel.stop("S" + i, lat + i * 0.01, 10.0).build());
    }
    var stopPattern = TransitRepositoryForTest.stopPattern(stops);
    return TripShapeFactory.straightLines(stopPattern, DeduplicatorService.NOOP);
  }

  /**
   * A shape data import with a straight-line shape for each of the patterns.
   */
  public static ShapeDataImport shapeDataImport(Collection<TripPattern> patterns) {
    var shapeDataImport = new ShapeDataImport();
    for (var pattern : patterns) {
      shapeDataImport.setTripPatternShape(
        pattern,
        TripShapeFactory.straightLines(pattern.getStopPattern(), DeduplicatorService.NOOP)
      );
    }
    return shapeDataImport;
  }
}
